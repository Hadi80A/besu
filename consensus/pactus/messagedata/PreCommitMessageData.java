
package org.hyperledger.besu.consensus.pactus.messagedata;

public class PreCommitMessageData {
    private final String validator;
    private final String blockHash;

    public PreCommitMessageData(String validator, String blockHash) {
        this.validator = validator;
        this.blockHash = blockHash;
    }

    public String getValidator() {
        return validator;
    }

    public String getBlockHash() {
        return blockHash;
    }
}
