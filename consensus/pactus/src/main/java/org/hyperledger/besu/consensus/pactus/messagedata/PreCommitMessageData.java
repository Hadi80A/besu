package org.hyperledger.besu.consensus.pactus.messagedata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a pre-commit message in the Pactus consensus protocol.
 * This is the second phase of voting after a proposal has been made.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreCommitMessageData {

  /** ID of the validator sending this pre-commit message. */
  private String validatorId;

  /** The hash of the block being pre-committed to. */
  private String blockHash;

  /** The round number during which this pre-commit is made. */
  private int round;

  /** The signature of the validator over the block hash and round. */
  private String signature;

  /**
   * Validates basic structural correctness of the message.
   */
  public boolean isValid() {
    return validatorId != null &&
           blockHash != null &&
           signature != null &&
           !validatorId.isEmpty() &&
           !blockHash.isEmpty() &&
           !signature.isEmpty();
  }
}
