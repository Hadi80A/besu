
package org.hyperledger.besu.consensus.pactus.validation;

import java.util.List;

public class CertificateValidator {
    public boolean validateCertificate(List<String> signedPreCommits) {
        return signedPreCommits.size() >= 2;
    }
}
