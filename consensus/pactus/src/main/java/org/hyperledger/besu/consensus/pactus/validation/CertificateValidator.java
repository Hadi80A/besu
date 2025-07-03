// CertificateValidator.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.validation;

import org.hyperledger.besu.consensus.pactus.messagewrappers.Certificate;
import org.hyperledger.besu.consensus.pactus.core.Validator;
import org.hyperledger.besu.consensus.pactus.core.ValidatorSet;

import java.security.PublicKey;
import java.util.Base64;
import java.util.Optional;

/**
 * Validates quorum certificates used to finalize blocks in Pactus consensus.
 */
public class CertificateValidator {

  private final ValidatorSet validatorSet;

  public CertificateValidator(ValidatorSet validatorSet) {
    this.validatorSet = validatorSet;
  }

  /**
   * Validates whether the given certificate meets quorum and signature conditions.
   *
   * @param certificate The certificate to validate.
   * @return true if valid, false otherwise.
   */
  public boolean validate(Certificate certificate) {
    if (!certificate.isStructurallyValid()) {
      return false;
    }

    int validSignatures = 0;

    for (int i = 0; i < certificate.getSignerIds().size(); i++) {
      String validatorId = certificate.getSignerIds().get(i);
      String signature = certificate.getSignatures().get(i);

      Optional<Validator> validatorOpt = validatorSet.getValidator(validatorId);
      if (validatorOpt.isEmpty()) continue;

      Validator validator = validatorOpt.get();
      PublicKey publicKey = validator.getPublicKey();

      String message = certificate.getBlockHash() + ":" + certificate.getRound();
      try {
        if (CryptoUtils.verify(message, signature, publicKey)) {
          validSignatures++;
        }
      } catch (Exception e) {
        return false;
      }
    }

    int quorum = PactusConstants.calculateQuorum(validatorSet.totalSize());
    return validSignatures >= quorum;
  }
}
