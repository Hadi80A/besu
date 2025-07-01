
package org.hyperledger.besu.consensus.pactus.payload;

public class PreCommitPayload implements PactusPayload {
    private final String validator;
    private final String blockHash;

    public PreCommitPayload(String validator, String blockHash) {
        this.validator = validator;
        this.blockHash = blockHash;
    }

    @Override
    public String getType() {
        return "PreCommit";
    }

    @Override
    public byte[] encode() {
        return (validator + ":" + blockHash).getBytes();
    }

    public String getValidator() {
        return validator;
    }

    public String getBlockHash() {
        return blockHash;
    }
}
