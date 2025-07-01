
package org.hyperledger.besu.consensus.pactus.messagedata;

public class ProposalMessageData {
    private final String proposer;
    private final String blockHash;

    public ProposalMessageData(String proposer, String blockHash) {
        this.proposer = proposer;
        this.blockHash = blockHash;
    }

    public String getProposer() {
        return proposer;
    }

    public String getBlockHash() {
        return blockHash;
    }
}
