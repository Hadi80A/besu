
package org.hyperledger.besu.consensus.pactus.validation;

public class ProposalValidator {
    public boolean validate(String proposerId, String blockHash) {
        return proposerId != null && !proposerId.isEmpty() && blockHash != null && !blockHash.isEmpty();
    }
}
