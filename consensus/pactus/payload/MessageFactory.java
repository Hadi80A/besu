
package org.hyperledger.besu.consensus.pactus.payload;

public class MessageFactory {
    public static PactusPayload createProposal(String proposer, String blockHash) {
        return new ProposalPayload(proposer, blockHash);
    }

    public static PactusPayload createPreCommit(String validator, String blockHash) {
        return new PreCommitPayload(validator, blockHash);
    }

    public static PactusPayload createVote(String voter, int round, boolean approve) {
        return new VotePayload(voter, round, approve);
    }

    public static PactusPayload createCertificate(String hash) {
        return new CertificatePayload(hash);
    }
}
