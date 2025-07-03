package org.hyperledger.besu.consensus.pactus.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a block in the Pactus consensus protocol.
 * Contains metadata and a list of transactions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Block {

  private int id;
  private int height;
  private int round;
  private long timestamp;
  private int proposer;
  private double appendTime;

  private List<UserTransaction> transactions;

}
