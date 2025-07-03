// CommitMessageData.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.messagedata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a commit message sent by a validator in the commit phase of Pactus consensus.
 * This message includes the validator's signature on the proposed block.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommitMessageData {

  /** ID or public key of the validator sending this commit message. */
  private String validatorId;

  /** The hash of the block being committed to. */
  private String blockHash;

  /** The round number during which this commit is cast. */
  private int round;

  /** The digital signature of the validator for this commit. */
  private String signature;

  /**
   * Validates structural integrity of the message.
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
