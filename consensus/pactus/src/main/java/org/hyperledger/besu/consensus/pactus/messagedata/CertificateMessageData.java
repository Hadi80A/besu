package org.hyperledger.besu.consensus.pactus.messagedata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents the message data containing the quorum certificate for a finalized block.
 * This includes the block hash, round, and a collection of commit signatures from validators.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateMessageData {

  /** Hash of the block this certificate finalizes. */
  private String blockHash;

  /** Block height this certificate is associated with. */
  private long blockHeight;

  /** Round during which the block was finalized. */
  private int round;

  /** Validator IDs who signed this certificate (could be public keys or addresses). */
  private List<String> signerIds;

  /** Signatures of the validators on the block hash. */
  private List<String> signatures;

  /**
   * Validates that the certificate is structurally complete.
   */
  public boolean isValidStructure() {
    return blockHash != null &&
           !signerIds.isEmpty() &&
           signerIds.size() == signatures.size();
  }
}
