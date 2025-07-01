
package org.hyperledger.besu.consensus.pactus.validation;

public class PreCommitValidator {
    public boolean validate(String validatorId, String blockHash) {
        return validatorId != null && blockHash != null;
    }
}
