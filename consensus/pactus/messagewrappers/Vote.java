
package org.hyperledger.besu.consensus.pactus.messagewrappers;

import pactus.messagedata.VoteMessageData;

public class Vote {
    private final VoteMessageData data;

    public Vote(VoteMessageData data) {
        this.data = data;
    }

    public VoteMessageData getData() {
        return data;
    }
}
