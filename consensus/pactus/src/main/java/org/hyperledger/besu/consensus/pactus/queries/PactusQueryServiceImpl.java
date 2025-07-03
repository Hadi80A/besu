// PactusQueryServiceImpl.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.queries;

import org.hyperledger.besu.consensus.pactus.core.Validator;
import org.hyperledger.besu.consensus.pactus.core.ValidatorSet;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Provides query services for inspecting validator set and consensus-related state in Pactus.
 */
public class PactusQueryServiceImpl {

  private final ValidatorSet validatorSet;

  public PactusQueryServiceImpl(final ValidatorSet validatorSet) {
    this.validatorSet = validatorSet;
  }

  /**
   * Returns the list of current committee members (i.e., active validators).
   */
  public List<String> getActiveValidatorIds() {
    return validatorSet.getCommitteeValidators().stream()
        .map(Validator::getId)
        .collect(Collectors.toList());
  }

  /**
   * Returns a full snapshot of all validators and their statuses.
   */
  public List<Validator> getAllValidators() {
    return List.copyOf(validatorSet.getAllValidators());
  }

  /**
   * Fetch information about a specific validator.
   */
  public Optional<Validator> getValidatorById(String id) {
    return validatorSet.getValidator(id);
  }

  /**
   * Returns the size of the current committee.
   */
  public int getCommitteeSize() {
    return validatorSet.committeeSize();
  }

  /**
   * Returns the total number of registered validators.
   */
  public int getTotalValidatorCount() {
    return validatorSet.totalSize();
  }
}
