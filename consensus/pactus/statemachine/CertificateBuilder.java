
package org.hyperledger.besu.consensus.pactus.statemachine;

import java.util.List;

public class CertificateBuilder {
    public static String buildCertificate(List<String> signedPreCommits) {
        return String.join("-", signedPreCommits).hashCode() + "";
    }
}
