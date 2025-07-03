// PreCommit.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.messagewrappers;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wrapper class for a pre-commit vote sent by a validator during the Pactus consensus round.
 * Used internally to manage validator responses and track votes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreCommit {

  /** ID or public key of the validator sending the pre-commit vote. */
  private String validatorId;

  /** The hash of the block being pre-committed to. */
  private String blockHash;

  /** The round number during which this vote is cast. */
  private int round;

  /** The digital signature over the block hash and round. */
  private String signature;

  /**
   * Checks if the structure of this pre-commit message is valid.
   */
  public boolean isValid() {
    return validatorId != null &&
           blockHash != null &&
           signature != null &&
           !validatorId.isEmpty() &&
           !blockHash.isEmpty() &&
           !signature.isEmpty();
  }
}
