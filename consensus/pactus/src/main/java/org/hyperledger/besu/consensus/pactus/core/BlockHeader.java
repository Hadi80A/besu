package org.hyperledger.besu.consensus.pactus.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the metadata (header) of a block in Pactus consensus.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockHeader {

  /** Unique block identifier. */
  private int id;

  /** Block height in the chain. */
  private int height;

  /** Consensus round during which the block was proposed. */
  private int round;

  /** Proposer node identifier. */
  private int proposer;

  /** Timestamp when the block was proposed. */
  private long timestamp;

  /** Hash of the parent block (optional for now). */
  private String parentHash;

  /** Merkle root or commitment to transaction data (optional). */
  private String transactionsRoot;

  /** Optional certificate hash (quorum signature, etc). */
  private String certificateHash;
}
