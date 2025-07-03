// PactusConstants.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.util;

/**
 * Holds constants used throughout the Pactus consensus implementation.
 */
public final class PactusConstants {

  private PactusConstants() {
    // Prevent instantiation
  }

  /** Default round timeout in milliseconds. */
  public static final long ROUND_TIMEOUT_MS = 3000;

  /** Minimum number of validators required for quorum (2f + 1). */
  public static int calculateQuorum(int validatorCount) {
    return (2 * validatorCount / 3) + 1;
  }

  /** Maximum round number allowed (safety guard). */
  public static final int MAX_ROUND = 100;

  /** Block proposal validity period (milliseconds) — optional future use. */
  public static final long PROPOSAL_TTL_MS = 10000;

  /** Default committee size (can be dynamic based on total validators). */
  public static final int DEFAULT_COMMITTEE_SIZE = 21;

  /** Number of blocks after which a validator must leave the committee (per Pactus design). */
  public static final int MAX_COMMITTEE_DURATION_BLOCKS = 51;

  /** Protocol name used in sub-protocol identifiers and gossip topics. */
  public static final String PROTOCOL_NAME = "pactus";

  /** P2P topic string prefixes. */
  public static final String TOPIC_PROPOSAL = "pactus/proposal";
  public static final String TOPIC_PRECOMMIT = "pactus/precommit";
  public static final String TOPIC_COMMIT = "pactus/commit";
  public static final String TOPIC_CERTIFICATE = "pactus/certificate";
}
