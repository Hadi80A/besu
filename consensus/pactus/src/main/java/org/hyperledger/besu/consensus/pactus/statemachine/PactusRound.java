// PactusRound.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.statemachine;

import org.hyperledger.besu.consensus.pactus.core.PactusBlock;
import org.hyperledger.besu.consensus.pactus.payload.MessageFactory;
import org.hyperledger.besu.consensus.pactus.payload.ProposePayload;
import org.hyperledger.besu.consensus.pactus.payload.PreCommitPayload;
import org.hyperledger.besu.consensus.pactus.payload.CommitPayload;
import org.hyperledger.besu.consensus.pactus.network.PactusMessageTransmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a single round of consensus at a given block height in Pactus protocol.
 * Manages proposal creation, message broadcast, and vote tracking.
 */
public class PactusRound {

  private final long height;
  private final int round;
  private final String localValidatorId;

  private final MessageFactory messageFactory;
  private final PactusMessageTransmitter messageTransmitter;

  private boolean proposalSent = false;
  private PactusBlock proposedPactusBlock;

  private final Map<String, PreCommitPayload> preCommits = new HashMap<>();
  private final Map<String, CommitPayload> commits = new HashMap<>();

  public PactusRound(
      final long height,
      final int round,
      final String localValidatorId,
      final MessageFactory messageFactory,
      final PactusMessageTransmitter messageTransmitter) {

    this.height = height;
    this.round = round;
    this.localValidatorId = localValidatorId;
    this.messageFactory = messageFactory;
    this.messageTransmitter = messageTransmitter;
  }

  /**
   * Called when the local node is proposer for the round.
   */
  public void proposeBlock(PactusBlock pactusBlock, String signature) {
    if (proposalSent) return;

    this.proposedPactusBlock = pactusBlock;
    ProposePayload payload = messageFactory.createProposePayload(pactusBlock, round, signature);
    messageTransmitter.multicastProposal(payload.toString()); // You may serialize to JSON
    this.proposalSent = true;
  }

  /**
   * Records a received pre-commit vote.
   */
  public void addPreCommitVote(PreCommitPayload payload) {
    preCommits.put(payload.getValidatorId(), payload);
  }

  /**
   * Records a received commit vote.
   */
  public void addCommitVote(CommitPayload payload) {
    commits.put(payload.getValidatorId(), payload);
  }

  public int countPreCommits() {
    return preCommits.size();
  }

  public int countCommits() {
    return commits.size();
  }

  public boolean hasQuorum(int totalValidators) {
    return countCommits() >= (2 * totalValidators / 3 + 1);
  }

  public long getHeight() {
    return height;
  }

  public int getRound() {
    return round;
  }

  public PactusBlock getProposedBlock() {
    return proposedPactusBlock;
  }

  public boolean isProposalSent() {
    return proposalSent;
  }

  public List<CommitPayload> getCommitPayloads() {
    return List.copyOf(commits.values());
  }
}
