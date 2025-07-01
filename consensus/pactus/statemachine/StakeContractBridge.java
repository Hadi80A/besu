package org.hyperledger.besu.consensus.pactus.statemachine;

import org.hyperledger.besu.consensus.pactus.ValidatorMetadata;
import org.hyperledger.besu.consensus.pactus.ValidatorSet;

public class StakeContractBridge {

    private final ValidatorSet validatorSet;

    public StakeContractBridge(ValidatorSet validatorSet) {
        this.validatorSet = validatorSet;
    }

    public boolean depositStake(String address, double ethAmount) {
        ValidatorMetadata validator = validatorSet.getValidator(address).orElse(null);
        if (validator == null) return false;

        // Only allow stake update if not in current committee
        if (!CommitteeTracker.getCurrentCommittee().contains(address)) {
            double newStake = validator.getStake() + ethAmount;
            validatorSet.addValidator(new ValidatorMetadata(address, newStake));
            return true;
        }
        return false;
    }

    public boolean canUpdateStake(String address) {
        return !CommitteeTracker.getCurrentCommittee().contains(address);
    }
}