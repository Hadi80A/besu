package org.hyperledger.besu.consensus.pactus;

import org.hyperledger.besu.consensus.pactus.statemachine.CommitteeTracker;

import java.util.*;

public class ValidatorSet {
    private final List<ValidatorMetadata> validators = new ArrayList<>();

    public void addValidator(ValidatorMetadata validator) {
        validators.removeIf(v -> v.getId().equals(validator.getId()));
        validators.add(validator);
    }

    public void removeValidator(String id) {
        validators.removeIf(v -> v.getId().equals(id));
    }

    public List<ValidatorMetadata> getValidators() {
        return Collections.unmodifiableList(validators);
    }

    public Optional<ValidatorMetadata> getValidator(String id) {
        return validators.stream().filter(v -> v.getId().equals(id)).findFirst();
    }

    public boolean depositStakeIfEligible(String validatorId, double stakeAmount) {
        Optional<ValidatorMetadata> opt = getValidator(validatorId);
        if (opt.isPresent()) {
            if (!CommitteeTracker.getCurrentCommittee().contains(validatorId)) {
                ValidatorMetadata updated = new ValidatorMetadata(validatorId, opt.get().getStake() + stakeAmount);
                addValidator(updated);
                return true;
            }
        }
        return false;
    }
}