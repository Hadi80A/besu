
package org.hyperledger.besu.consensus.pactus.validation;

public class MessageValidatorFactory {
    public static ProposalValidator createProposalValidator() {
        return new ProposalValidator();
    }

    public static PreCommitValidator createPreCommitValidator() {
        return new PreCommitValidator();
    }

    public static StakeValidator createStakeValidator(double minStake) {
        return new StakeValidator(minStake);
    }

    public static CertificateValidator createCertificateValidator() {
        return new CertificateValidator();
    }
}
