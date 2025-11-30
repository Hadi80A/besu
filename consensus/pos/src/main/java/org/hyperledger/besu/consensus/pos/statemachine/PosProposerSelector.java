package org.hyperledger.besu.consensus.pos.statemachine;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.config.PosConfigOptions;
import org.hyperledger.besu.consensus.pos.core.Node;
import org.hyperledger.besu.consensus.pos.core.NodeSet;
import org.hyperledger.besu.consensus.pos.vrf.VRF;
import org.hyperledger.besu.crypto.Hash;
import org.hyperledger.besu.crypto.SECPPublicKey;
import org.hyperledger.besu.cryptoservices.NodeKey;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.core.Util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Log4j2
@Setter
@Getter
public class PosProposerSelector {

    private static final double LAMBDA =1.0; // tune so ≈1 leader is expected per round
    private static final long EPS = 1L;
    private final Map<Address, BigDecimal> allScore;
    private final NodeSet nodeSet;     // all validators
    private final NodeKey nodeKey;     // local validator’s key
    private final long selfStake;      // local validator stake
    private Map<Long,Bytes32> seedMap;
    private final PosConfigOptions posConfigOptions;
//    private final String selfId;       // unique validator id
    private Bytes32 previousSeed;
//    private SECPPublicKey selfPublicKey;
    private Optional<Address> currentLeader = Optional.empty();
    @Getter
    private BigDecimal leaderScore;
    @Getter
    private Bytes32 leaderY;
    public PosProposerSelector(final NodeSet nodeSet,
                               final NodeKey nodeKey,
//                               final String selfId,
                               final long selfStake, PosConfigOptions posConfigOptions) {
        this.nodeSet = Objects.requireNonNull(nodeSet);
        this.nodeKey = Objects.requireNonNull(nodeKey);
//        this.selfId = Objects.requireNonNull(selfId);
        this.selfStake = selfStake;
        this.posConfigOptions = posConfigOptions;
        seedMap = new HashMap<>();
        allScore = new HashMap<>();
//        this.selfPublicKey = nodeKey.getPublicKey();
    }

    /** Run VRF and check if self is elected leader. */
    public Optional<VRF.Result> calculateVrf(final long round, final Bytes32 prevBlockHash, final Long height ,final Bytes32 previousSeed) {
        // Compute seed for VRF
        final Bytes32 seed = seed(round, prevBlockHash,height);
        log.debug("seed({},{},{},{}): {}", round, prevBlockHash,height,previousSeed,seed);
        // Run VRF locally
        final VRF.Result vrf = VRF.prove(nodeKey, seed);

        return Optional.of(vrf);

    }

    public boolean canLeader(final VRF.Proof proof, final Bytes32 seed, final Address nodeAddress, final SECPPublicKey publicKey) {
        Optional<Node> maybeNode = nodeSet.getNode(nodeAddress);
        if (maybeNode.isEmpty()) {
            log.debug("node is empty for address {}", nodeAddress);
            return false;
        }
        final Node node = maybeNode.get();
// derive y and convert to unit fraction in [0,1)
        final Bytes32 yBytes = VRF.hash(publicKey, seed, proof);
        final BigDecimal yFraction = PosProposerSelector.toUnitFraction(yBytes);
// stake and sum(max(eps, wi))
        final long stake = node.getStakeInfo().getStakedAmount();
        final BigDecimal wiCap = BigDecimal.valueOf(Math.max(EPS, stake));
        long totalCapSum = nodeSet.getAllNodes()
                .stream()
                .mapToLong(n -> Math.max(EPS, n.getStakeInfo().getStakedAmount()))
                .sum();
        if (totalCapSum <= 0L) {
            // defensive: should not happen because EPS >= 1
            log.warn("totalCapSum <= 0; falling back to false");
            return false;
        }

// T = LAMBDA / sum_i max(eps, wi)
        final BigDecimal T = BigDecimal.valueOf(LAMBDA).divide(new BigDecimal(totalCapSum), MathContext.DECIMAL128);
// score = y / max(eps, wi)
        final BigDecimal score = yFraction.divide(wiCap, MathContext.DECIMAL128);
        log.debug("canLeader: addr={}, yFrac={}, stake={}, max(eps,wi)={}, score={}, T={}",
                nodeAddress, yFraction, stake, wiCap, score, T);

        allScore.put(node.getAddress(),score);
        return score.compareTo(T) < 0;
    }
    public Optional<Address> getCurrentProposer() {
        return currentLeader;
    }

    public boolean isLocalProposer(){

        return getCurrentProposer().isPresent() && Util.publicKeyToAddress(nodeKey.getPublicKey()).equals(getCurrentProposer().get());
    }

    /** Seed derivation: keccak256("NEXUS-VRF-SEED" || round || prevBlockHash). */
    public Bytes32 seed(final long round, final Bytes32 prevBlockHash, final Long height ) {
        // Follow spec seed(h,r) = H(h || r || Rh || H(B_{h-1}))
        // We serialize height (h) then round (r) then previousSeed (Rh) then prevBlockHash (H(B_{h-1}))
        final ByteBuffer hh = ByteBuffer.allocate(Long.BYTES).putLong(height);
        final ByteBuffer rr = ByteBuffer.allocate(Long.BYTES).putLong(round);
        hh.flip();
        rr.flip();
        Bytes32 lastSeed=previousSeed;
        if(round<=0) {
            if (posConfigOptions.getSeed().isPresent()) {
                lastSeed= longToBytes32(posConfigOptions.getSeed().getAsLong());

            } else {
                lastSeed= Bytes32.ZERO;
            }
            seedMap.put(0L,lastSeed);
        }
        Bytes32 result = Hash.keccak256(
                org.apache.tuweni.bytes.Bytes.concatenate(
                        org.apache.tuweni.bytes.Bytes.wrap(hh.array()),   // height first
                        org.apache.tuweni.bytes.Bytes.wrap(rr.array()),   // then round
                        lastSeed == null ? Bytes32.ZERO : lastSeed,
                        prevBlockHash == null ? Bytes32.ZERO : prevBlockHash
                ));
        seedMap.put(round,result);
        return result;
    }
    public Bytes32 getSeedAtRound(long round, org.hyperledger.besu.datatypes.Hash prevBlockHash, long height) {
        if(seedMap.containsKey(round)) {
            return seedMap.get(round);
        }else {
            return seed(round,prevBlockHash,height);        }
    }
//    public Bytes32 getSeedAtRound(long round, org.hyperledger.besu.datatypes.Hash prevBlockHash, long height) {
//        if(round<0) {
//            if(posConfigOptions.getSeed().isPresent()) {
//                return longToBytes32(posConfigOptions.getSeed().getAsLong());
//            }else {
//                return Bytes32.ZERO;
//            }
//        }else{
//
////            if(seedMap.containsKey(round)) {
////                return seedMap.get(round);
////            }else
////                return getPreviousSeed(round,prevBlockHash,height);
//        }
//    }

//    private Bytes32 getPreviousSeed(long round, org.hyperledger.besu.datatypes.Hash prevBlockHash, long height) {
//        while(!seedMap.containsKey(round)){
//            round--;
//        }
//        return seed(round,prevBlockHash,height,getSeedAtRound(round-1,prevBlockHash,height));

//        return seedMap.getOrDefault(round,Bytes32.ZERO);

//    }

    public static BigDecimal toUnitFraction(final Bytes32 y) {
        final BigInteger val = new BigInteger(1, y.toArrayUnsafe());
        final BigInteger max = BigInteger.ONE.shiftLeft(8 * y.size());
        return new BigDecimal(val).divide(new BigDecimal(max), MathContext.DECIMAL128);
    }

    public static BigDecimal threshold(final long stake, final long totalStake) {
        BigDecimal t = new BigDecimal(stake)
                .divide(new BigDecimal(totalStake), MathContext.DECIMAL128)
                .multiply(BigDecimal.valueOf(LAMBDA));
        if (t.compareTo(BigDecimal.ONE) > 0) t = BigDecimal.ONE;
        return t;
    }

   private Bytes32 longToBytes32(long value) {
        byte[] bytes32 = new byte[32];          // 32 zero bytes
        ByteBuffer.wrap(bytes32, 24, 8)         // start at offset 24
                .putLong(value);               // write 8 bytes in big-endian
        return Bytes32.wrap(bytes32);
    }

}
