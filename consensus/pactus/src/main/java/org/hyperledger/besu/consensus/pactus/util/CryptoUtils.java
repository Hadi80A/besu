// CryptoUtils.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.util;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

/**
 * Utility class for cryptographic operations such as signature creation and verification.
 * Placeholder for full cryptographic integration (e.g., ECDSA or BLS).
 */
public class CryptoUtils {

  /**
   * Signs a given message using the provided private key.
   *
   * @param message The message to sign.
   * @param privateKey The private key to sign with.
   * @return The Base64-encoded signature.
   * @throws Exception if signing fails.
   */
  public static String sign(String message, PrivateKey privateKey) throws Exception {
    Signature signature = Signature.getInstance("SHA256withRSA"); // Use appropriate algorithm
    signature.initSign(privateKey);
    signature.update(message.getBytes(StandardCharsets.UTF_8));
    byte[] signedBytes = signature.sign();
    return Base64.getEncoder().encodeToString(signedBytes);
  }

  /**
   * Verifies a signature against the message and public key.
   *
   * @param message The original message.
   * @param signatureStr The Base64-encoded signature to verify.
   * @param publicKey The public key of the signer.
   * @return true if valid, false otherwise.
   * @throws Exception if verification fails.
   */
  public static boolean verify(String message, String signatureStr, PublicKey publicKey) throws Exception {
    Signature signature = Signature.getInstance("SHA256withRSA"); // Use appropriate algorithm
    signature.initVerify(publicKey);
    signature.update(message.getBytes(StandardCharsets.UTF_8));
    byte[] decodedSignature = Base64.getDecoder().decode(signatureStr);
    return signature.verify(decodedSignature);
  }
}
