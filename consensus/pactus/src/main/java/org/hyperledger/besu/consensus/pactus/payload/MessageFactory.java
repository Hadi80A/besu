// MessageFactory.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.payload;

import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.payload.Payload;
import org.hyperledger.besu.consensus.common.bft.payload.SignedData;
import org.hyperledger.besu.consensus.pactus.core.PactusBlock;
import org.hyperledger.besu.consensus.pactus.messagewrappers.*;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.rlp.RLPOutput;
import org.hyperledger.besu.plugin.data.Signature;

import java.util.List;

/**
 * Factory class for creating consensus message payloads and wrappers.
 */
public class MessageFactory implements Payload {

  private final int localValidatorId;

  public MessageFactory(final int localValidatorId) {
    this.localValidatorId = localValidatorId;
  }

  public Proposal createProposal(SignedData<ProposePayload> payload) {
    return new Proposal(payload,localValidatorId);
  }
  public Prepare createPrepare(SignedData<PreparePayload> payload) {
    return new Prepare(payload,localValidatorId);
  }
  public PreCommit createPreCommit(SignedData<PreCommitPayload> payload) {
    return new PreCommit(payload,localValidatorId);
  }
  public Commit createCommit(SignedData<CommitPayload> payload) {
    return new Commit(payload,localValidatorId);
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

  public ProposePayload createProposePayload(PactusBlock pactusBlock, int round,int height, Signature signature) {
    return ProposePayload.builder()
        .pactusBlock(pactusBlock)
        .round(round)
        .height(height)
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

  @Override
  public void writeTo(RLPOutput rlpOutput) {

  }

  @Override
  public int getMessageType() {
    return 0;
  }

  @Override
  public Hash hashForSignature() {
    return null;
  }

  @Override
  public ConsensusRoundIdentifier getRoundIdentifier() {
    return null;
  }
}
