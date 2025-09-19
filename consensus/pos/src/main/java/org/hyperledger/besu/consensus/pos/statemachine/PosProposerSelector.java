package org.hyperledger.besu.consensus.pos.statemachine;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Log4j2
@Setter
public class PosProposerSelector {

    private static final double LAMBDA =1.0; // tune so ≈1 leader is expected per round

    private final NodeSet nodeSet;     // all validators
    private final NodeKey nodeKey;     // local validator’s key
    private final long selfStake;      // local validator stake
    private Map<Long,Bytes32> seedMap;
//    private final String selfId;       // unique validator id

//    private SECPPublicKey selfPublicKey;
    private Optional<Address> currentLeader = Optional.empty();
    @Getter
    private Bytes32 leaderY;
    public PosProposerSelector(final NodeSet nodeSet,
                               final NodeKey nodeKey,
//                               final String selfId,
                               final long selfStake) {
        this.nodeSet = Objects.requireNonNull(nodeSet);
        this.nodeKey = Objects.requireNonNull(nodeKey);
//        this.selfId = Objects.requireNonNull(selfId);
        this.selfStake = selfStake;
        seedMap = new HashMap<>();
//        this.selfPublicKey = nodeKey.getPublicKey();
    }

    /** Run VRF and check if self is elected leader. */
    public Optional<VRF.Result> calculateVrf(final long round, final Bytes32 prevBlockHash, final Long height ,final Bytes32 previousSeed) {
        // Compute seed for VRF
        final Bytes32 seed = seed(round, prevBlockHash,height,previousSeed);
        log.debug("seed({},{},{},{}): {}", round, prevBlockHash,height,previousSeed,seed);
        // Run VRF locally
        final VRF.Result vrf = VRF.prove(nodeKey, seed);

        return Optional.of(vrf);

    }

    public boolean canLeader(VRF.Proof proof, Bytes32 seed, Address nodeAddress, SECPPublicKey publicKey) {
        Optional<Node> node = nodeSet.getNode(nodeAddress);
        if(node.isEmpty()){
            log.debug("node is empty");
            return false;
        }
        log.debug("node{}", node.get().getAddress());
        final var y = VRF.hash(publicKey, seed, proof);
        final BigDecimal ratio = PosProposerSelector.toUnitFraction(y);

        final long totalStake = nodeSet.getAllNodes().stream().mapToLong(n -> n.getStakeInfo().getStakedAmount()).sum();

        final BigDecimal thr = PosProposerSelector.threshold(node.get().getStakeInfo().getStakedAmount(), totalStake);
        log.debug("ratio: {}, thr:{}, auther:{}", ratio, thr, nodeAddress);
        return  ratio.compareTo(thr) < 0;
    }

    public Optional<Address> getCurrentProposer() {
        return currentLeader;
    }

    public boolean isLocalProposer(){

        return getCurrentProposer().isPresent() && Util.publicKeyToAddress(nodeKey.getPublicKey()).equals(getCurrentProposer().get());
    }

    /** Seed derivation: keccak256("NEXUS-VRF-SEED" || round || prevBlockHash). */
    public Bytes32 seed(final long round, final Bytes32 prevBlockHash, final Long height ,final Bytes32 previousSeed) {
        final ByteBuffer bb = ByteBuffer.allocate(Long.BYTES).putLong(round);
        final ByteBuffer hh = ByteBuffer.allocate(Long.BYTES).putLong(height);
        bb.flip();
        hh.flip();
        Bytes32 result=Hash.keccak256(
                org.apache.tuweni.bytes.Bytes.concatenate(
                        org.apache.tuweni.bytes.Bytes.wrap(bb.array()),
                        org.apache.tuweni.bytes.Bytes.wrap(hh.array()),
                        previousSeed,
                        prevBlockHash
                ));
        seedMap.put(round,result);
        return result;
    }

    public Bytes32 getSeedAtRound(long round ) {
        if(round<0) {
            return Bytes32.ZERO;
        }else{
            if(seedMap.containsKey(round)) {
                return seedMap.get(round);
            }else
                return getPreviousSeed(round);
        }
    }

    private Bytes32 getPreviousSeed(long round) {
        while(!seedMap.containsKey(round)){
            round--;
        }
        return seedMap.getOrDefault(round,Bytes32.ZERO);
    }

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
}
