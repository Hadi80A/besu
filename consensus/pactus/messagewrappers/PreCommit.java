
package org.hyperledger.besu.consensus.pactus.messagewrappers;

import pactus.messagedata.PreCommitMessageData;

public class PreCommit {
    private final PreCommitMessageData data;

    public PreCommit(PreCommitMessageData data) {
        this.data = data;
    }

    public PreCommitMessageData getData() {
        return data;
    }
}
