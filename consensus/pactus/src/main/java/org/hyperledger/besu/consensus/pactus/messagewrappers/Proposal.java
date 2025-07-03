// Proposal.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.messagewrappers;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.besu.consensus.pactus.core.Block;

/**
 * Represents the wrapper for a proposal in Pactus consensus.
 * This is the first message broadcast in a new round by the selected proposer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Proposal {

  /** The ID (or public key) of the proposer. */
  private String proposerId;

  /** The proposed block content. */
  private Block proposedBlock;

  /** The round number in which this proposal was broadcast. */
  private int round;

  /** The digital signature from the proposer. */
  private String signature;

  /**
   * Validates that the proposal message is complete and structurally correct.
   */
  public boolean isValid() {
    return proposerId != null &&
           proposedBlock != null &&
           signature != null &&
           !proposerId.isEmpty() &&
           !signature.isEmpty();
  }
}
