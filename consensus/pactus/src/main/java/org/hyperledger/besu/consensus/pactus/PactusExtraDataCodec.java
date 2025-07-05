// PactusExtraDataCodec.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus;

import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.consensus.common.bft.BftExtraData;
import org.hyperledger.besu.consensus.common.bft.BftExtraDataCodec;
import org.hyperledger.besu.ethereum.blockcreation.ExtraDataCodec;

/**
 * Handles encoding and decoding of the extraData field in block headers for Pactus consensus.
 * This typically includes validator information and round/height metadata.
 */
public class PactusExtraDataCodec extends BftExtraDataCodec {

  @Override
  public Bytes encode(final Object extraData) {
    // TODO: Serialize your extra data (e.g., validatorId + signature) into Bytes
    if (extraData instanceof String) {
      return Bytes.wrap(((String) extraData).getBytes());
    }
    throw new IllegalArgumentException("Unsupported extraData type");
  }

  @Override
  public Object decode(final Bytes bytes) {
    // TODO: Deserialize from bytes back into your custom structure
    return bytes.toUtf8String();
  }

  @Override
  public String name() {
    return "Pactus";
  }

  @Override
  protected Bytes encode(BftExtraData bftExtraData, EncodingType encodingType) {
    return null;
  }

  @Override
  public BftExtraData decodeRaw(Bytes bytes) {
    return null;
  }
}
