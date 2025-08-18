package org.hyperledger.besu.consensus.pos.core;

import org.hyperledger.besu.consensus.pos.bls.Bls;

import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.List;

public final class CommitCertificate {
//  private final BitSet signerBitmap;   // length n
  private final Bls.Signature aggregate; // 96 bytes compressed

  public CommitCertificate(/*BitSet signerBitmap, */Bls.Signature aggregate){
//    this.signerBitmap = (BitSet) signerBitmap.clone();
    this.aggregate = aggregate;
  }
//  public BitSet signerBitmap() { return (BitSet) signerBitmap.clone(); }
  public Bls.Signature aggregate() { return aggregate; }

//  public byte[] toBytes() {
//    byte[] bitmapBytes = toLittleEndianBitmapBytes(signerBitmap);
//    byte[] sig = aggregate.compressed96();
//    return ByteBuffer.allocate(2 + bitmapBytes.length + sig.length)
//        .putShort((short) bitmapBytes.length)
//        .put(bitmapBytes)
//        .put(sig)
//        .array();
//  }
//
//  public static CommitCertificate fromBytes(byte[] data) {
//    ByteBuffer bb = ByteBuffer.wrap(data);
//    int len = Short.toUnsignedInt(bb.getShort());
//    byte[] bitmapBytes = new byte[len]; bb.get(bitmapBytes);
//    byte[] sig = new byte[96]; bb.get(sig);
//    return new CommitCertificate(fromLittleEndianBitmap(bitmapBytes), new Bls.Signature(sig));
//  }
//
//  public static byte[] toLittleEndianBitmapBytes(BitSet bs) {
//    byte[] a = bs.toByteArray(); // LSB-first in Java’s BitSet is fine for wire if you document it
//    return a;
//  }
//  public static BitSet fromLittleEndianBitmap(byte[] bytes) {
//    return BitSet.valueOf(bytes);
//  }
//
//  public List<Bls.PublicKey> signers(List<Bls.PublicKey> validatorSet) {
//    return Bls.selectByBitmap(validatorSet, signerBitmap);
//  }
}
