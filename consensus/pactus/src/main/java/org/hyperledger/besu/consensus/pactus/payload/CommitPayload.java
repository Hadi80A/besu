// CommitPayload.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the payload of a commit message in Pactus consensus.
 * Each validator uses this payload to vote on finalizing a proposed block.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommitPayload {

  /** The ID or address of the committing validator. */
  private String validatorId;

  /** Hash of the block being committed. */
  private String blockHash;

  /** The round number when this commit occurred. */
  private int round;

  /** The cryptographic signature over the blockHash and round. */
  private String signature;

  /**
   * Verifies whether this payload is structurally valid.
   */
  public boolean isValid() {
    return validatorId != null &&
           !validatorId.isEmpty() &&
           blockHash != null &&
           !blockHash.isEmpty() &&
           signature != null &&
           !signature.isEmpty();
  }
}
