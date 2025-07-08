// ProposePayload.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.payload;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.*;
import lombok.experimental.SuperBuilder;
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
 * Payload for a block proposal in the Pactus consensus protocol.
 * Sent by the proposer to initiate a new round.
 */
@Data
public class ProposePayload extends PactusPayload {

  @Builder
  public ProposePayload(int round, int height) {
    super(round, height);
  }

  public static ProposePayload readFrom(
          final RLPInput rlpInput, final PactusBlockCodec blockEncoder) throws IOException {
    int round = rlpInput.readInt();
    int height = rlpInput.readInt();
//    Signature signature = SerializeUtil.toObject(rlpInput.readBytes(),Signature.class) ;


    return new ProposePayload(round,height);
  }

  @SneakyThrows
  @Override
  public void writeTo(RLPOutput rlpOutput) {
    rlpOutput.writeInt(round);
    rlpOutput.writeInt(height);
//    rlpOutput.writeBytes(SerializeUtil.toBytes(signature));
  }



  @Override
  public int getMessageType() {
    return 0;
  }

  @Override
  public Hash hashForSignature() {
    String data= getMessageType() + "|"+round+"|"+height;
    return Hash.hash(Bytes.wrap(data.getBytes()));
  }

  @Override
  public ConsensusRoundIdentifier getRoundIdentifier() {
    return null;
  }
//  public PactusBlock getProposedBlock(){
//    return pactusBlock;
//  }
}
