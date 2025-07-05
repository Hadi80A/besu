// RoundState.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.statemachine;

import lombok.Getter;
import org.hyperledger.besu.consensus.pactus.core.PactusBlock;
import org.hyperledger.besu.consensus.pactus.payload.PreCommitPayload;
import org.hyperledger.besu.consensus.pactus.payload.ProposePayload;

import java.util.*;

/**
 * Maintains the state of a round in Pactus consensus — tracking proposals, votes, and round progress.
 */
public class RoundState {

  private final long height;

  @Getter
  private int currentRound;

  private ProposePayload proposedBlockPayload;
  private final Map<String, PreCommitPayload> preCommits = new HashMap<>();

  public RoundState(final long height) {
    this.height = height;
    this.currentRound = 0;
  }

  public void beginRound(final int round) {
    this.currentRound = round;
    this.proposedBlockPayload = null;
    this.preCommits.clear();
  }

  public void onRoundChange(final int newRound) {
    this.currentRound = newRound;
    this.proposedBlockPayload = null;
    this.preCommits.clear();
  }

  public void processProposal(ProposePayload payload) {
    if (payload != null && payload.getRound() == currentRound) {
      this.proposedBlockPayload = payload;
    }
  }

  public void addPreCommit(PreCommitPayload payload) {
    if (payload != null && payload.getRound() == currentRound) {
      preCommits.put(payload.getValidatorId(), payload);
    }
  }

  public int getPreCommitCount() {
    return preCommits.size();
  }

  public Optional<PactusBlock> getProposedBlock() {
    return Optional.ofNullable(proposedBlockPayload).map(ProposePayload::getPactusBlock);
  }

  public boolean isProposalReceived() {
    return proposedBlockPayload != null;
  }

  public Collection<PreCommitPayload> getAllPreCommits() {
    return preCommits.values();
  }
}
