// ProposeMessageData.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.messagedata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.besu.consensus.pactus.core.Block;

/**
 * Represents a proposal message sent by a validator in the Pactus consensus protocol.
 * This message is the initial proposal broadcast at the beginning of a round.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProposeMessageData {

  /** ID of the validator making the proposal. */
  private String proposerId;

  /** The block proposed for inclusion in the blockchain. */
  private Block proposedBlock;

  /** The round number during which this proposal is made. */
  private int round;

  /** Digital signature of the proposer for the block and round. */
  private String signature;

  /**
   * Validates the message structure before signature verification.
   */
  public boolean isValid() {
    return proposerId != null &&
           proposedBlock != null &&
           round >= 0 &&
           signature != null &&
           !proposerId.isEmpty() &&
           !signature.isEmpty();
  }
}
