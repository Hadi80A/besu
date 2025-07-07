package org.hyperledger.besu.consensus.pactus.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.consensus.pactus.util.SerializeUtil;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.rlp.RLPInput;
import org.hyperledger.besu.ethereum.rlp.RLPOutput;

import java.io.IOException;

/**
 * Represents the metadata (header) of a block in Pactus consensus.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PactusBlockHeader {

  private Address proposer;

  private int version;

  private long sortitionSeed;

  private BlockHeader besuHeader;

  public void writeTo(RLPOutput rlpOutput) throws JsonProcessingException {
    rlpOutput.writeBytes(SerializeUtil.toBytes(proposer));
    rlpOutput.writeInt(version);
    rlpOutput.writeLong(sortitionSeed);
  }

  public static PactusBlockHeader readFrom(RLPInput rlpInput, BlockHeader besuHeader) throws IOException {
    Address proposer= (Address) SerializeUtil.toObject(rlpInput.readBytes(),Address.class);
    int version = rlpInput.readInt();
    long sortitionSeed = rlpInput.readLong();
    return new PactusBlockHeader(proposer,version,sortitionSeed,besuHeader);
  }


}
