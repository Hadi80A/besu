// PreCommitPayload.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the payload of a pre-commit message in the Pactus consensus protocol.
 * Used in the second voting phase to signal agreement before committing to a block.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreCommitPayload {

  /** Validator ID or public key. */
  private String validatorId;

  /** The hash of the block being pre-committed to. */
  private String blockHash;

  /** The round number in which the pre-commit was made. */
  private int round;

  /** The validator's signature for this pre-commit. */
  private String signature;

  /**
   * Checks whether the payload has the required structure.
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
