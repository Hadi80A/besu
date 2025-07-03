// ProposePayload.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.besu.consensus.pactus.core.Block;

/**
 * Payload for a block proposal in the Pactus consensus protocol.
 * Sent by the proposer to initiate a new round.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProposePayload {

  /** The ID (e.g., public key) of the proposer. */
  private String proposerId;

  /** The proposed block for the current round. */
  private Block block;

  /** The round number in which the proposal is made. */
  private int round;

  /** Proposer's signature on the proposed block and round. */
  private String signature;

  /**
   * Checks whether the proposal payload is complete and valid.
   */
  public boolean isValid() {
    return proposerId != null &&
           block != null &&
           signature != null &&
           !proposerId.isEmpty() &&
           !signature.isEmpty();
  }
}
