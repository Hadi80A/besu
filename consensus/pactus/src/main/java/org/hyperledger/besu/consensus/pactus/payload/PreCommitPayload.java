// PreCommitPayload.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.payload;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.*;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.payload.Payload;
import org.hyperledger.besu.consensus.pactus.PactusBlockCodec;
import org.hyperledger.besu.consensus.pactus.core.PactusBlock;
import org.hyperledger.besu.consensus.pactus.util.SerializeUtil;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.rlp.RLPInput;
import org.hyperledger.besu.ethereum.rlp.RLPOutput;
import org.hyperledger.besu.plugin.data.Signature;

import java.io.IOException;

/**
 * Payload for a block preCommit in the Pactus consensus protocol.
 * Sent by the proposer to initiate a new round.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreCommitPayload implements Payload {

  /** The proposed block for the current round. */
  private PactusBlock pactusBlock;

  /** The round number in which the preCommit is made. */
  private int round;

  private int height;


  /**
   * Checks whether the preCommit payload is complete and valid.
   */
  public boolean isValid() {
    return  pactusBlock != null &&
            signature != null &&
            !signature.isEmpty();
  }

  public static PreCommitPayload readFrom(
          final RLPInput rlpInput, final PactusBlockCodec blockEncoder) throws IOException {
    PactusBlock pactusBlock1 = PactusBlock.readFrom(rlpInput);
    int round = rlpInput.readInt();
    int height = rlpInput.readInt();
    Signature signature = SerializeUtil.toObject(rlpInput.readBytes(),Signature.class) ;


    return new PreCommitPayload(pactusBlock1,round,height,signature);
  }

  public PactusBlock getProposedBlock(){
    return pactusBlock;
  }

  @SneakyThrows
  @Override
  public void writeTo(RLPOutput rlpOutput) {
    pactusBlock.writeTo(rlpOutput);
    rlpOutput.writeInt(round);
    rlpOutput.writeInt(height);
    rlpOutput.writeBytes(SerializeUtil.toBytes(signature));
  }



  @Override
  public int getMessageType() {
    return 0;
  }

  @Override
  public Hash hashForSignature() {
    return null;
  }

  @Override
  public ConsensusRoundIdentifier getRoundIdentifier() {
    return null;
  }
}
