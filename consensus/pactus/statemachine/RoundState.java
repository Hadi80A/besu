
package org.hyperledger.besu.consensus.pactus.statemachine;

public class RoundState {
    private boolean proposalReceived;
    private boolean certificateBuilt;

    public void markProposalReceived() {
        this.proposalReceived = true;
    }

    public void markCertificateBuilt() {
        this.certificateBuilt = true;
    }

    public boolean isProposalReceived() {
        return proposalReceived;
    }

    public boolean isCertificateBuilt() {
        return certificateBuilt;
    }
}
