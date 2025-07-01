
package org.hyperledger.besu.consensus.pactus.messagewrappers;

import pactus.messagedata.ProposalMessageData;

public class Proposal {
    private final ProposalMessageData data;

    public Proposal(ProposalMessageData data) {
        this.data = data;
    }

    public ProposalMessageData getData() {
        return data;
    }
}
