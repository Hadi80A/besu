// MessageFactory.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.payload;

import org.hyperledger.besu.consensus.pactus.core.Block;
import org.hyperledger.besu.consensus.pactus.messagewrappers.*;
import java.util.List;

/**
 * Factory class for creating consensus message payloads and wrappers.
 */
public class MessageFactory {

  private final String localValidatorId;

  public MessageFactory(final String localValidatorId) {
    this.localValidatorId = localValidatorId;
  }

  public Proposal createProposal(Block block, int round, String signature) {
    return Proposal.builder()
        .proposerId(localValidatorId)
        .proposedBlock(block)
        .round(round)
        .signature(signature)
        .build();
  }

  public PreCommit createPreCommit(String blockHash, int round, String signature) {
    return PreCommit.builder()
        .validatorId(localValidatorId)
        .blockHash(blockHash)
        .round(round)
        .signature(signature)
        .build();
  }

  public Commit createCommit(String blockHash, int round, String signature) {
    return Commit.builder()
        .validatorId(localValidatorId)
        .blockHash(blockHash)
        .round(round)
        .signature(signature)
        .build();
  }

  public Certificate createCertificate(String blockHash, long height, int round,
                                       List<String> signerIds, List<String> signatures) {
    return Certificate.builder()
        .blockHash(blockHash)
        .blockHeight(height)
        .round(round)
        .signerIds(signerIds)
        .signatures(signatures)
        .build();
  }

  public CommitPayload createCommitPayload(String blockHash, int round, String signature) {
    return CommitPayload.builder()
        .validatorId(localValidatorId)
        .blockHash(blockHash)
        .round(round)
        .signature(signature)
        .build();
  }

  public ProposePayload createProposePayload(Block block, int round, String signature) {
    return ProposePayload.builder()
        .proposerId(localValidatorId)
        .block(block)
        .round(round)
        .signature(signature)
        .build();
  }

  public PreCommitPayload createPreCommitPayload(String blockHash, int round, String signature) {
    return PreCommitPayload.builder()
        .validatorId(localValidatorId)
        .blockHash(blockHash)
        .round(round)
        .signature(signature)
        .build();
  }

  public CertificatePayload createCertificatePayload(String blockHash, long height, int round,
                                                     List<String> signerIds, List<String> signatures) {
    return CertificatePayload.builder()
        .blockHash(blockHash)
        .blockHeight(height)
        .round(round)
        .signerIds(signerIds)
        .signatures(signatures)
        .build();
  }
}
