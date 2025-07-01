
package org.hyperledger.besu.consensus.pactus.validation;

import org.hyperledger.besu.consensus.pactus.ValidatorMetadata;

public class StakeValidator {
    private final double minStake;

    public StakeValidator(double minStake) {
        this.minStake = minStake;
    }

    public boolean isValid(ValidatorMetadata validator) {
        return validator.getStake() >= minStake;
    }
}
