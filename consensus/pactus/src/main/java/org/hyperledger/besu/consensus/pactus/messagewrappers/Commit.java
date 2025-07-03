// Commit.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.messagewrappers;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a commit vote from a validator for a proposed block.
 * This message is sent during the commit phase of Pactus consensus.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Commit {

  /** Validator ID or public key who is committing. */
  private String validatorId;

  /** Block hash that is being committed to. */
  private String blockHash;

  /** Round during which the commit is cast. */
  private int round;

  /** Digital signature of the validator over the commit message. */
  private String signature;

  /**
   * Checks whether this commit message is structurally valid.
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
