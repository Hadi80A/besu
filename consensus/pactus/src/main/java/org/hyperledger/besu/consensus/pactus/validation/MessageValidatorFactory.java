// MessageValidatorFactory.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.validation;

import org.hyperledger.besu.consensus.pactus.core.ValidatorSet;
import org.hyperledger.besu.consensus.pactus.messagewrappers.Commit;
import org.hyperledger.besu.consensus.pactus.messagewrappers.PreCommit;
import org.hyperledger.besu.consensus.pactus.messagewrappers.Proposal;

import java.util.Optional;

/**
 * Factory for generating message validators for Pactus consensus messages.
 */
public class MessageValidatorFactory {

  private final ValidatorSet validatorSet;

  public MessageValidatorFactory(final ValidatorSet validatorSet) {
    this.validatorSet = validatorSet;
  }

  public Optional<MessageValidator<Proposal>> createProposalValidator() {
    return Optional.of(new ProposalMessageValidator(validatorSet));
  }

  public Optional<MessageValidator<PreCommit>> createPreCommitValidator() {
    return Optional.of(new PreCommitMessageValidator(validatorSet));
  }

  public Optional<MessageValidator<Commit>> createCommitValidator() {
    return Optional.of(new CommitMessageValidator(validatorSet));
  }
}
