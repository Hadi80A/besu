// CommitPayload.java - placeholder for Pactus consensus implementation
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
 * Payload for a block commit in the Pactus consensus protocol.
 * Sent by the proposer to initiate a new round.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CommitPayload extends PactusPayload {
  private PactusBlock block;
  @Builder
  public CommitPayload(int round, int height,PactusBlock block) {
    super(round, height);
    this.block = block;
  }

  public static CommitPayload readFrom(final RLPInput rlpInput) throws IOException {
//    PactusBlock pactusBlock1 = PactusBlock.readFrom(rlpInput);
    int round = rlpInput.readInt();
    int height = rlpInput.readInt();
//    Hash blockHash= Hash.fromHexString(String.valueOf(rlpInput.readBytes()));
    PactusBlock pactusBlock= PactusBlock.readFrom(rlpInput);
//    Signature signature = SerializeUtil.toObject(rlpInput.readBytes(),Signature.class) ;
    return new CommitPayload(round,height,pactusBlock);
  }


  @SneakyThrows
  @Override
  public void writeTo(RLPOutput rlpOutput) {
//    pactusBlock.writeTo(rlpOutput);
    rlpOutput.writeInt(round);
    rlpOutput.writeInt(height);
//    rlpOutput.writeBytes(Bytes.wrap(blockHash.toHexString().getBytes()));
//    rlpOutput.writeBytes(SerializeUtil.toBytes(signature));
    block.writeTo(rlpOutput);
  }



  @Override
  public int getMessageType() {
    return PactusMessage.BLOCK_ANNOUNCE.getCode();
  }

  @Override
  public Hash hashForSignature() {
    return Hash.ZERO;
  }

  @Override
  public ConsensusRoundIdentifier getRoundIdentifier() {
    return null;
  }
}
