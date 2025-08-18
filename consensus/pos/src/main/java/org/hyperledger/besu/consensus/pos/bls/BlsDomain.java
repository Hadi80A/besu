package org.hyperledger.besu.consensus.pos.bls;

/**
 * Domain separation tags for BLS (IETF ciphersuites, min-pubkey, POP).
 * Keep these constants in one place so all signing/verification is consistent.
 */
public final class BlsDomain {
  // Regular signing/verifying (signatures live in G2, 96 bytes compressed)
  public static final String SIGNATURE_DST = "BLS_SIG_BLS12381G2_XMD:SHA-256_SSWU_RO_POP_";

  // Proof-of-possession (POP) domain; used when proving/validating key ownership
  public static final String POP_DST       = "BLS_POP_BLS12381G1_XMD:SHA-256_SSWU_RO_POP_";

  // Back-compat alias if some code still references BlsDomain.DST
  public static final String DST = SIGNATURE_DST;

  private BlsDomain() {}
}
