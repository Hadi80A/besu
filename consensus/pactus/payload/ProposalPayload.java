
package org.hyperledger.besu.consensus.pactus.payload;

public class ProposalPayload implements PactusPayload {
    private final String proposer;
    private final String blockHash;

    public ProposalPayload(String proposer, String blockHash) {
        this.proposer = proposer;
        this.blockHash = blockHash;
    }

    @Override
    public String getType() {
        return "Proposal";
    }

    @Override
    public byte[] encode() {
        return (proposer + ":" + blockHash).getBytes();
    }

    public String getProposer() {
        return proposer;
    }

    public String getBlockHash() {
        return blockHash;
    }
}
