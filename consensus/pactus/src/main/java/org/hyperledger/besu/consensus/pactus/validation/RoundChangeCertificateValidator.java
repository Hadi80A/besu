// RoundChangeCertificateValidator.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.validation;

import org.hyperledger.besu.consensus.pactus.core.PactusBlock;
import org.hyperledger.besu.consensus.pactus.core.Validator;
import org.hyperledger.besu.consensus.pactus.core.ValidatorSet;
import org.hyperledger.besu.consensus.pactus.payload.PreCommitPayload;

import java.security.PublicKey;
import java.util.List;
import java.util.Optional;

/**
 * Validates pre-commits accompanying a round change to ensure
 * that a block has legitimate backing before continuing from a previous round.
 */
public class RoundChangeCertificateValidator {

  private final ValidatorSet validatorSet;

  public RoundChangeCertificateValidator(ValidatorSet validatorSet) {
    this.validatorSet = validatorSet;
  }

  /**
   * Validates a round-change certificate based on a proposed block and its pre-commits.
   *
   * @param pactusBlock The proposed block for the new round.
   * @param preCommits List of pre-commits from validators supporting the block.
   * @param round The round from which the pre-commits originate.
   * @return true if the certificate is valid, false otherwise.
   */
  public boolean validate(PactusBlock pactusBlock, List<PreCommitPayload> preCommits, int round) {
    if (pactusBlock == null || preCommits == null || preCommits.isEmpty()) {
      return false;
    }

    int validSignatures = 0;
    String blockHash = pactusBlock.getHash();

    for (PreCommitPayload payload : preCommits) {
      if (!blockHash.equals(payload.getBlockHash()) || payload.getRound() != round) {
        continue;
      }

      Optional<Validator> validatorOpt = validatorSet.getValidator(payload.getValidatorId());
      if (validatorOpt.isEmpty()) continue;

      Validator validator = validatorOpt.get();
      PublicKey publicKey = validator.getPublicKey();
      String message = blockHash + ":" + round;

      try {
        if (CryptoUtils.verify(message, payload.getSignature(), publicKey)) {
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
