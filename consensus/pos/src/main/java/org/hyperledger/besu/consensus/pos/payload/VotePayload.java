package org.hyperledger.besu.consensus.pos.payload;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.payload.Payload;
import org.hyperledger.besu.consensus.pos.bls.Bls;
import org.hyperledger.besu.consensus.pos.messagedata.PosMessage;
import org.hyperledger.besu.crypto.SECPSignature;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.rlp.RLPInput;
import org.hyperledger.besu.ethereum.rlp.RLPOutput;

import java.math.BigInteger;

@Getter
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
public class VotePayload extends PosPayload {
  private static final int TYPE = PosMessage.VOTE.getCode();

  /**
   * Canonical secp256k1 curve order 'n' (from SEC2 / secp256k1 parameters).
   * Hex: FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141
   */
  private static final BigInteger SECP256K1_CURVE_ORDER =
          new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16);

  private final Hash digest;
  private final Bls.Signature signature;

  protected VotePayload(
          final ConsensusRoundIdentifier roundIdentifier, final long height, final Hash digest, final Bls.Signature signature) {
    super(roundIdentifier, height);
    this.digest = digest;
    this.signature = signature;
  }

  public static VotePayload readFrom(final RLPInput rlpInput) {
    rlpInput.enterList();
    final ConsensusRoundIdentifier roundIdentifier = ConsensusRoundIdentifier.readFrom(rlpInput);
    final Hash digest = Payload.readDigest(rlpInput);
    final long height = rlpInput.readLong();

    final Bytes signatureBytes = rlpInput.readBytes();
    Bls.Signature blsSignature =Bls.Signature.signatureFromCompressed(signatureBytes.toArray()) ;
    // read signature in (r, s, recId/recParity) scalar fields
//    final BigInteger r = rlpInput.readBigIntegerScalar();
//    final BigInteger s = rlpInput.readBigIntegerScalar();
//    final byte recId = rlpInput.readByte();
    rlpInput.leaveList();

//    // validate/construct signature using the curve order (n)
//    final SECPSignature secpSignature =
//            SECPSignature.create(r, s, recId, SECP256K1_CURVE_ORDER);

    return new VotePayload(roundIdentifier, height, digest,blsSignature);
  }

  @Override
  public void writeTo(final RLPOutput rlpOutput) {
    rlpOutput.startList();
    getRoundIdentifier().writeTo(rlpOutput);
    rlpOutput.writeBytes(digest);
    rlpOutput.writeLong(getHeight());
    rlpOutput.writeBytes(Bytes.wrap(signature.toBytesCompressed()));
//    rlpOutput.writeBigIntegerScalar(signature.getR());
//    rlpOutput.writeBigIntegerScalar(signature.getS());
//    rlpOutput.writeByte(signature.getRecId());
    rlpOutput.endList();
  }

  @Override
  public int getMessageType() {
    return TYPE;
  }

}
