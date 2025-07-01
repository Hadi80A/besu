
package org.hyperledger.besu.consensus.pactus.validation;

public class SignedDataValidator {
    public boolean isValidSignature(String data, String signature) {
        return signature != null && signature.length() > 10; // fake logic
    }
}
