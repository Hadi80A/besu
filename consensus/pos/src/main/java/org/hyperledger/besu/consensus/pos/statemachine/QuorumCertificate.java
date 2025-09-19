package org.hyperledger.besu.consensus.pos.statemachine;

import lombok.Getter;
import lombok.Setter;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.payload.Payload;
import org.hyperledger.besu.consensus.pos.payload.CommitPayload;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.rlp.RLPInput;
import org.hyperledger.besu.ethereum.rlp.RLPOutput;

import java.util.BitSet;
@Getter
@Setter
public class QuorumCertificate {

    private final Bytes32 hash;
//    private final Enum hash;
    private final Long height;
    private final Long round;
//    private final  ;//aggregation
    private final BitSet bitmap;


    public QuorumCertificate(Bytes32 hash, Long height, Long round, BitSet bitmap) {
        this.hash = hash;
        this.height = height;
        this.round = round;
        this.bitmap = bitmap;
    }

    public static QuorumCertificate readFrom(final RLPInput rlpInput) {
        rlpInput.enterList();
        final Bytes32 hash = rlpInput.readBytes32();
        Long height = rlpInput.readLong();
        long round = rlpInput.readLong();
        Bytes bitMapBytes= rlpInput.readBytes();
        BitSet bitMap =BitSet.valueOf(bitMapBytes.toArray());

        rlpInput.leaveList();

        return new QuorumCertificate(hash,height,round,bitMap);
    }

    public void writeTo(final RLPOutput rlpOutput) {
        rlpOutput.startList();
        rlpOutput.writeBytes(hash);
        rlpOutput.writeLong(height);
        rlpOutput.writeLong(round);
        rlpOutput.writeBytes(Bytes.wrap(bitmap.toByteArray()));
        rlpOutput.endList();
    }

}
