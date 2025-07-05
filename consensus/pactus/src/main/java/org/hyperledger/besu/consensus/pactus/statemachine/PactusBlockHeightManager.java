// PactusBlockHeightManager.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.statemachine;

import org.hyperledger.besu.consensus.pactus.core.PactusBlock;
import org.hyperledger.besu.consensus.pactus.core.ValidatorSet;
import org.hyperledger.besu.consensus.pactus.payload.MessageFactory;
import org.hyperledger.besu.consensus.pactus.payload.ProposePayload;
import org.hyperledger.besu.consensus.pactus.validation.RoundState;

import java.util.Optional;

/**
 * Manages the current block height, active round, and related state in the Pactus consensus engine.
 */
public class PactusBlockHeightManager {

  private final long height;
  private int currentRound;
  private final ValidatorSet validatorSet;
  private final MessageFactory messageFactory;

  private final RoundState roundState;

  public PactusBlockHeightManager(
      final long height,
      final ValidatorSet validatorSet,
      final MessageFactory messageFactory) {

    this.height = height;
    this.validatorSet = validatorSet;
    this.messageFactory = messageFactory;
    this.currentRound = 0;

    this.roundState = new RoundState(height);
  }

  /**
   * Returns the current round for this height.
   */
  public int getCurrentRound() {
    return currentRound;
  }

  /**
   * Advances to the next round of consensus.
   */
  public void incrementRound() {
    currentRound++;
    roundState.onRoundChange(currentRound);
  }

  /**
   * Returns the height being managed.
   */
  public long getHeight() {
    return height;
  }

  /**
   * Starts the consensus round for the current height.
   */
  public void startRound() {
    roundState.beginRound(currentRound);
  }

  /**
   * Processes a block proposal for this round.
   */
  public void handleProposal(ProposePayload proposePayload) {
    roundState.processProposal(proposePayload);
  }

  /**
   * Attempts to retrieve the currently proposed block for this height and round.
   */
  public Optional<PactusBlock> getProposedBlock() {
    return roundState.getProposedBlock();
  }

  /**
   * Returns the round state for testing or external queries.
   */
  public RoundState getRoundState() {
    return roundState;
  }
}
