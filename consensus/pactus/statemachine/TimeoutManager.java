
package org.hyperledger.besu.consensus.pactus.statemachine;

public class TimeoutManager {
    private final long timeoutMillis;
    private long startTime;

    public TimeoutManager(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
        reset();
    }

    public void reset() {
        this.startTime = System.currentTimeMillis();
    }

    public boolean hasTimedOut() {
        return System.currentTimeMillis() - startTime > timeoutMillis;
    }
}
