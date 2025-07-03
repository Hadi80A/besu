package org.hyperledger.besu.consensus.pactus.messagewrappers;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a finalized quorum certificate proving a block's commitment.
 * It encapsulates the validators and their commit signatures.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Certificate {

  /** The hash of the committed block. */
  private String blockHash;

  /** The height of the block to which this certificate belongs. */
  private long blockHeight;

  /** Round number during which this block was finalized. */
  private int round;

  /** List of validator IDs (e.g., public keys or addresses) who signed the commit. */
  private List<String> signerIds;

  /** Corresponding list of signatures from validators. */
  private List<String> signatures;

  /**
   * Validates that the certificate has the necessary structure.
   * (Signatures can be validated cryptographically in a separate step.)
   */
  public boolean isStructurallyValid() {
    return blockHash != null &&
           !blockHash.isEmpty() &&
           signerIds != null &&
           signatures != null &&
           !signerIds.isEmpty() &&
           signerIds.size() == signatures.size();
  }

  /**
   * Returns the number of unique validator signatures in the certificate.
   */
  public int quorumSize() {
    return signerIds != null ? signerIds.size() : 0;
  }
}
