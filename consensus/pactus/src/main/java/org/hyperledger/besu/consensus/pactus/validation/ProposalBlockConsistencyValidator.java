// ProposalBlockConsistencyValidator.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.validation;

import org.hyperledger.besu.consensus.pactus.messagewrappers.Proposal;
import org.hyperledger.besu.consensus.pactus.core.Block;

/**
 * Validates whether a proposed block is consistent with the expected consensus round and height.
 */
public class ProposalBlockConsistencyValidator {

  private final long expectedHeight;
  private final int expectedRound;

  public ProposalBlockConsistencyValidator(final long expectedHeight, final int expectedRound) {
    this.expectedHeight = expectedHeight;
    this.expectedRound = expectedRound;
  }

  /**
   * Checks if the block in the proposal matches the current height and round expectations.
   *
   * @param proposal The proposal to validate.
   * @return true if consistent, false otherwise.
   */
  public boolean validate(final Proposal proposal) {
    if (proposal == null || proposal.getProposedBlock() == null) {
      return false;
    }

    Block block = proposal.getProposedBlock();
    return block.getHeight() == expectedHeight && proposal.getRound() == expectedRound;
  }
}
