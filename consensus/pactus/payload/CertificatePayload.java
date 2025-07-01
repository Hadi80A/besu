
package org.hyperledger.besu.consensus.pactus.payload;

public class CertificatePayload implements PactusPayload {
    private final String certificateHash;

    public CertificatePayload(String certificateHash) {
        this.certificateHash = certificateHash;
    }

    @Override
    public String getType() {
        return "Certificate";
    }

    @Override
    public byte[] encode() {
        return certificateHash.getBytes();
    }

    public String getCertificateHash() {
        return certificateHash;
    }
}
