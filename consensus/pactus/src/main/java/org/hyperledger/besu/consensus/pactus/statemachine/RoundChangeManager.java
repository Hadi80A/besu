package org.hyperledger.besu.consensus.pactus.statemachine;

import lombok.extern.slf4j.Slf4j;
import org.hyperledger.besu.consensus.pactus.payload.PreCommitPayload;

import java.util.*;

/**
 * Tracks pre-commits across rounds to support round change and recovery logic.
 */
@Slf4j
public class RoundChangeManager {

  private final Map<Integer, List<PreCommitPayload>> roundPreCommits = new HashMap<>();

  /**
   * Adds a pre-commit vote for the specified round.
   */
  public void addPreCommit(int round, PreCommitPayload payload) {
    roundPreCommits.computeIfAbsent(round, k -> new ArrayList<>()).add(payload);
  }

  /**
   * Gets all pre-commit payloads for a given round.
   */
  public List<PreCommitPayload> getPreCommitsForRound(int round) {
    return roundPreCommits.getOrDefault(round, Collections.emptyList());
  }

  /**
   * Returns the latest round number for which pre-commits exist.
   */
  public Optional<Integer> getLatestPreparedRound() {
    return roundPreCommits.keySet().stream()
        .max(Integer::compareTo);
  }

  /**
   * Clears all recorded round change state.
   */
  public void reset() {
    roundPreCommits.clear();
  }

  /**
   * Returns all known rounds with pre-commit collections.
   */
  public Set<Integer> getTrackedRounds() {
    return roundPreCommits.keySet();
  }

  /**
   * Debug print for internal diagnostics.
   */
  public void logState() {
    log.debug("Tracked rounds: {}", getTrackedRounds());
    roundPreCommits.forEach((round, precommits) ->
        log.debug("Round {} has {} pre-commits", round, precommits.size()));
  }
}
