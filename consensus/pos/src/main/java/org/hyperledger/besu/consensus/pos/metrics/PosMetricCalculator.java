package org.hyperledger.besu.consensus.pos.metrics;

import com.sun.management.OperatingSystemMXBean;
import org.hyperledger.besu.ethereum.core.Block;
import org.hyperledger.besu.ethereum.core.Transaction;
import org.hyperledger.besu.datatypes.Hash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class PosMetricCalculator {

    private static final Logger LOG = LoggerFactory.getLogger("BesuMetrics");

    // --- Metric 4 & 5 Data: Throughput & Latency ---
    private final long simulationStartTime;
    private final AtomicLong totalConfirmedTransactions = new AtomicLong(0);
    private final AtomicLong cumulativeLatencyMs = new AtomicLong(0);

    // Store as Seconds to match Block Header timestamp format
    private volatile long firstTimeStamp = 0;

    // Map to store t_created (Arrival time of Tx) -> Hash mapping
    private final Map<Hash, Long> txArrivalTimes = new ConcurrentHashMap<>();

    // --- Metric 3 Data: Memory Usage ---
    private final MemoryMXBean memoryBean;
    private long peakMemoryUsageKB = 0;

    // --- Metric 1 & 2 Data: CPU Time ---
    private final OperatingSystemMXBean osBean;
    private final ThreadMXBean threadBean;

    public PosMetricCalculator() {
        this.simulationStartTime = System.currentTimeMillis();
        this.threadBean = ManagementFactory.getThreadMXBean();
        this.memoryBean = ManagementFactory.getMemoryMXBean();

        // Initialize OS Bean for System CPU time (Metric 2)
        if (ManagementFactory.getOperatingSystemMXBean() instanceof OperatingSystemMXBean) {
            this.osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        } else {
            this.osBean = null;
            LOG.warn("OperatingSystemMXBean not supported. System CPU Time will be 0.");
        }

        // Enable CPU monitoring (Metric 1)
        if (threadBean.isThreadCpuTimeSupported()) {
            threadBean.setThreadCpuTimeEnabled(true);
        }
    }

    // =========================================================
    // HOOK 0: Capture Transaction Creation (True Start of Latency)
    // =========================================================
    /**
     * Call this method when a transaction is added to the Transaction Pool (Mempool).
     * This captures the true "Creation/Arrival" time.
     */
    public void recordTransactionCreated(Transaction tx) {
        // We use putIfAbsent so we record the EARLIEST time we saw the Tx.
        txArrivalTimes.putIfAbsent(tx.getHash(), System.currentTimeMillis());
        LOG.debug("Transaction {} has arrival time {}", tx.getHash(), txArrivalTimes.get(tx.getHash()));
    }

    // =========================================================
    // HOOK 1: Capture Proposal (Start of Throughput Timer)
    // =========================================================
    public void recordProposalArrival(Block block) {
        long now = System.currentTimeMillis();
        updatePeakMemory(); // Update Metric 3

        // 1. Capture the timestamp of the very first block (Consensus Start)
        if (firstTimeStamp == 0) {
            firstTimeStamp = block.getHeader().getTimestamp(); // This is in SECONDS
        }

        // 2. Fallback for Latency:
        // If 'recordTransactionCreated' was NOT called for these transactions,
        // we record the time now. If it WAS called, putIfAbsent ignores this,
        // preserving the accurate creation time.
        for (Transaction tx : block.getBody().getTransactions()) {
            txArrivalTimes.putIfAbsent(tx.getHash(), now);
        }
    }

    // =========================================================
    // HOOK 2: Capture Block Commit (End of Latency, Throughput)
    // =========================================================
    public void recordBlockCommit(Block block) {
        long confirmTime = System.currentTimeMillis();
        updatePeakMemory(); // Update Metric 3

        List<Transaction> transactions = block.getBody().getTransactions();
        int count = transactions.size();

        if (count > 0) {
            totalConfirmedTransactions.addAndGet(count);

            for (Transaction tx : transactions) {
                // We remove the entry to free memory and get the start time
                Long creationTime = txArrivalTimes.remove(tx.getHash());

                if (creationTime != null) {
                    // Metric 5: Latency Calculation (t_confirmed - t_created)
                    long latency = confirmTime - creationTime;

                    // Sanity check for negative latency (clock skew protection)
                    if (latency < 0) latency = 0;

                    cumulativeLatencyMs.addAndGet(latency);
                }
            }
        }

        // Log all metrics after every block import
        printMetrics(block.getHeader().getNumber());
    }

    // Helper for Metric 3 (Peak Memory)
    private void updatePeakMemory() {
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();
        long currentTotal = heap.getUsed() + nonHeap.getUsed();
        long currentKB = currentTotal / 1024;

        synchronized (this) {
            if (currentKB > peakMemoryUsageKB) {
                peakMemoryUsageKB = currentKB;
            }
        }
    }

    // =========================================================
    // GETTERS FOR ALL 5 METRICS
    // =========================================================

    // Metric 1: User CPU Time
    public double getUserCpuTime() {
        long totalUserNs = 0;
        for (long id : threadBean.getAllThreadIds()) {
            long userTime = threadBean.getThreadUserTime(id);
            if (userTime != -1) totalUserNs += userTime;
        }
        return totalUserNs / 1_000_000_000.0;
    }

    // Metric 2: System CPU Time
    public double getSystemCpuTime() {
        if (osBean == null) return 0.0;
        long totalProcessNs = osBean.getProcessCpuTime(); // Total (User + Sys)
        long totalUserNs = 0;

        for (long id : threadBean.getAllThreadIds()) {
            long userTime = threadBean.getThreadUserTime(id);
            if (userTime != -1) totalUserNs += userTime;
        }

        long systemNs = totalProcessNs - totalUserNs;
        return Math.max(0, systemNs) / 1_000_000_000.0;
    }

    // Metric 3: Memory Usage (Peak) is accessed via peakMemoryUsageKB variable

    // Metric 4: Throughput (Tx/sec)
    public double getThroughput() {
        if (firstTimeStamp == 0) {
            return 0.0;
        }

        // Conversion: System time (ms) -> Seconds
        double currentSeconds = System.currentTimeMillis() / 1000.0;
        double duration = currentSeconds - firstTimeStamp;

        if (duration <= 0) {
            return 0.0;
        }

        return totalConfirmedTransactions.get() / duration;
    }

    // Metric 5: Average Latency
    public double getAvgLatency() {
        long txCount = totalConfirmedTransactions.get();
        if (txCount == 0) return 0;
        double totalLatencySec = cumulativeLatencyMs.get() / 1000.0;
        return totalLatencySec / txCount;
    }

    public void printMetrics(long blockNumber) {
        double currentTotalTime = (System.currentTimeMillis() - simulationStartTime) / 1000.0;

        // Construct JSON string
        String jsonLog = String.format(
                "{" +
                        "\"timestamp\": %d, " +
                        "\"blockNumber\": %d, " +
                        "\"metrics\": {" +
                        "\"userCpuTime\": %.4f, " +
                        "\"systemCpuTime\": %.4f, " +
                        "\"peakMemoryUsageKB\": %d, " +
                        "\"throughput\": %.4f, " +
                        "\"avgLatency\": %.4f" +
                        "}, " +
                        "\"meta\": {" +
                        "\"totalTime\": %.4f, " +
                        "\"confirmedTx\": %d" +
                        "}" +
                        "}",
                System.currentTimeMillis(),
                blockNumber,
                getUserCpuTime(),
                getSystemCpuTime(),
                peakMemoryUsageKB,
                getThroughput(),
                getAvgLatency(),
                currentTotalTime,
                totalConfirmedTransactions.get()
        );

        // Log the single JSON line
        LOG.info(jsonLog);
    }
}