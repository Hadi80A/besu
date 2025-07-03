// SignedDataValidator.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.validation;

import org.hyperledger.besu.consensus.pactus.core.Validator;
import org.hyperledger.besu.consensus.pactus.core.ValidatorSet;
import org.hyperledger.besu.consensus.pactus.util.CryptoUtils;

import java.security.PublicKey;
import java.util.Optional;

/**
 * Validates digital signatures from validators to ensure authenticity of signed messages.
 */
public class SignedDataValidator {

  private final ValidatorSet validatorSet;

  public SignedDataValidator(final ValidatorSet validatorSet) {
    this.validatorSet = validatorSet;
  }

  /**
   * Verifies that the message is signed by the expected validator and is cryptographically valid.
   *
   * @param message       The original message (e.g., blockHash + ":" + round).
   * @param signature     The Base64-encoded signature.
   * @param validatorId   The ID or public key string of the validator.
   * @return true if signature is valid and belongs to a recognized validator.
   */
  public boolean validateSignature(final String message, final String signature, final String validatorId) {
    Optional<Validator> validatorOpt = validatorSet.getValidator(validatorId);

    if (validatorOpt.isEmpty()) {
      return false;
    }

    Validator validator = validatorOpt.get();
    PublicKey publicKey = validator.getPublicKey();

    try {
      return CryptoUtils.verify(message, signature, publicKey);
    } catch (Exception e) {
      return false;
    }
  }
}
