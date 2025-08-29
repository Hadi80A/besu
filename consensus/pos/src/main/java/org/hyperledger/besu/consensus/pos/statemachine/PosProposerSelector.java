package org.hyperledger.besu.consensus.pos.statemachine;

import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.pos.core.NodeSet;
import org.hyperledger.besu.consensus.pos.messagewrappers.SelectLeader;
import org.hyperledger.besu.consensus.pos.payload.SelectLeaderPayload;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Log4j2
@Setter
public class PosProposerSelector {

    private static final double LAMBDA =1.0; // tune so ≈1 leader is expected per round

    private final NodeSet nodeSet;     // all validators
    private final NodeKey nodeKey;     // local validator’s key
    private final long selfStake;      // local validator stake
//    private final String selfId;       // unique validator id

//    private SECPPublicKey selfPublicKey;
    private Optional<Address> currentLeader = Optional.empty();

    public PosProposerSelector(final NodeSet nodeSet,
                               final NodeKey nodeKey,
//                               final String selfId,
                               final long selfStake) {
        this.nodeSet = Objects.requireNonNull(nodeSet);
        this.nodeKey = Objects.requireNonNull(nodeKey);
//        this.selfId = Objects.requireNonNull(selfId);
        this.selfStake = selfStake;
//        this.selfPublicKey = nodeKey.getPublicKey();
    }

    /** Run VRF and check if self is elected leader. */
    public Optional<VRF.Result> calculateVrf(final long round, final Bytes32 prevBlockHash) {
        // Compute seed for VRF
        final Bytes32 seed = seed(round, prevBlockHash);
        log.debug("seed({},{}): {}", round, prevBlockHash,seed);
        // Run VRF locally
        final VRF.Result vrf = VRF.prove(nodeKey, seed);

        return Optional.of(vrf);

    }

    public Optional<Address> getCurrentProposer() {
        return currentLeader;
    }

    public boolean isLocalProposer(){
        if (Util.publicKeyToAddress(nodeKey.getPublicKey()).equals(getCurrentProposer().get())){
            return true;
        }
        return false;
    }

    /** Seed derivation: keccak256("NEXUS-VRF-SEED" || round || prevBlockHash). */
    public static Bytes32 seed(final long round, final Bytes32 prevBlockHash) {
        final ByteBuffer bb = ByteBuffer.allocate(Long.BYTES).putLong(round);
        bb.flip();
        return Hash.keccak256(
                org.apache.tuweni.bytes.Bytes.concatenate(
                        org.apache.tuweni.bytes.Bytes.wrap("NEXUS-VRF-SEED".getBytes(StandardCharsets.UTF_8)),
                        org.apache.tuweni.bytes.Bytes.wrap(bb.array()),
                        prevBlockHash
                )
        );
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
