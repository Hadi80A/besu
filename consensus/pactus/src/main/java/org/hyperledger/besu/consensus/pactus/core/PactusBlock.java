package org.hyperledger.besu.consensus.pactus.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.consensus.common.bft.BftBlockHeaderFunctions;
import org.hyperledger.besu.consensus.pactus.PactusExtraDataCodec;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.core.Block;
import org.hyperledger.besu.ethereum.rlp.BytesValueRLPOutput;
import org.hyperledger.besu.ethereum.rlp.RLPInput;
import org.hyperledger.besu.ethereum.rlp.RLPOutput;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Represents a block in the Pactus consensus protocol.
 * Contains metadata and a list of transactions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PactusBlock {
  private Block besuBlock;
  private PactusBlockHeader pactusHeader;
  private PactusCertificate pactusCertificate;

  public void writeTo(RLPOutput rlpOutput) throws JsonProcessingException {

     besuBlock.writeTo(rlpOutput);
     pactusHeader.writeTo(rlpOutput);
     pactusCertificate.writeTo(rlpOutput);
  }

    public static PactusBlock readFrom(RLPInput rlpInput) throws IOException {
        Block block= Block.readFrom(rlpInput, BftBlockHeaderFunctions.forCommittedSeal(new PactusExtraDataCodec()));
        PactusBlockHeader header= PactusBlockHeader.readFrom(rlpInput,block.getHeader());
        PactusCertificate certificate=PactusCertificate.readFrom(rlpInput) ;
        return new PactusBlock(block,header,certificate);
    }

    public Hash getHash() {
        BytesValueRLPOutput rlpOutput = new BytesValueRLPOutput();
        try {
            besuBlock.writeTo(rlpOutput);
            pactusHeader.writeTo(rlpOutput);
            pactusCertificate.writeTo(rlpOutput);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize PactusBlock for hash computation", e);
        }
        Bytes encoded = rlpOutput.encoded();
        return Hash.hash(encoded);
    }
    public int getRound(){
      if(Objects.nonNull(pactusCertificate))
          return pactusCertificate.getRound();
      return 0;
    }

    public int getHeight(){
        if(Objects.nonNull(pactusCertificate))
            return pactusCertificate.getHeight();
        return 0;
    }

}
