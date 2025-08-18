package org.hyperledger.besu.consensus.pos.bls;

import java.util.BitSet;
import java.util.List;
import java.util.Optional;

public interface Bls {
  record PublicKey(byte[] compressed48) {}
  record SecretKey(byte[] raw32) {}
  record Signature(byte[] compressed96) {}
  record KeyPair(SecretKey sk, PublicKey pk) {}

  // Keygen / PoP
  KeyPair keyGen(byte[] ikm32);                   // ikm = input key material (secure random)
  Signature proofOfPossession(SecretKey sk, PublicKey pk);
  boolean verifyPoP(PublicKey pk, Signature pop);

  // Basic
  Signature sign(SecretKey sk, byte[] message, String dst);
  boolean verify(PublicKey pk, byte[] message, Signature sig, String dst);

  // Aggregation (all sign the SAME message; fastAggVerify requires PoP done at join)
  Signature aggregate(List<Signature> sigs);
  boolean fastAggregateVerify(List<PublicKey> pks, byte[] message, Signature aggSig, String dst);

  // Utilities
  Optional<PublicKey> deserializePk(byte[] compressed48);
  Optional<Signature> deserializeSig(byte[] compressed96);
  byte[] serializePk(PublicKey pk);
  byte[] serializeSig(Signature sig);

  // Extract subset of public keys by signer bitmap (LSB-first bit ordering).
  static List<PublicKey> selectByBitmap(List<PublicKey> validatorSet, BitSet bitmap) {
    return java.util.stream.IntStream.range(0, validatorSet.size())
        .filter(bitmap::get)
        .mapToObj(validatorSet::get)
        .toList();
  }
}
