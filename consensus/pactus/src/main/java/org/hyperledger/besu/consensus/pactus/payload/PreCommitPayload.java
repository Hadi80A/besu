// PreCommitPayload.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.payload;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.*;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.payload.Payload;
import org.hyperledger.besu.consensus.pactus.PactusBlockCodec;
import org.hyperledger.besu.consensus.pactus.core.PactusBlock;
import org.hyperledger.besu.consensus.pactus.messagedata.PactusMessage;
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
@EqualsAndHashCode(callSuper = true)
public class PreCommitPayload extends PactusPayload{
  private Hash blockHash;
  @Builder
  public PreCommitPayload(int round, int height,Hash blockHash) {
    super(round, height);
    this.blockHash = blockHash;
  }

  public static PreCommitPayload readFrom(final RLPInput rlpInput) throws IOException {
//    PactusBlock pactusBlock1 = PactusBlock.readFrom(rlpInput);
    int round = rlpInput.readInt();
    int height = rlpInput.readInt();
//    Signature signature = SerializeUtil.toObject(rlpInput.readBytes(),Signature.class) ;
    Hash blockHash= Hash.fromHexString(String.valueOf(rlpInput.readBytes()));

    return new PreCommitPayload(round,height,blockHash);
  }

  @SneakyThrows
  @Override
  public void writeTo(RLPOutput rlpOutput) {
//    pactusBlock.writeTo(rlpOutput);
    rlpOutput.writeInt(round);
    rlpOutput.writeInt(height);
    rlpOutput.writeBytes(Bytes.wrap(blockHash.toHexString().getBytes()));
//    rlpOutput.writeBytes(SerializeUtil.toBytes(signature));
  }

  @Override
  public int getMessageType() {
    return PactusMessage.PRE_COMMIT.getCode();
  }

  @Override
  public Hash hashForSignature() {
    String data= getMessageType() + "|"+round+"|"+height+"|"+blockHash.toHexString();
    return Hash.hash(Bytes.wrap(data.getBytes()));
  }

  @Override
  public ConsensusRoundIdentifier getRoundIdentifier() {
    return null;
  }
}
