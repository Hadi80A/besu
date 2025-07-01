package org.hyperledger.besu.consensus.pactus.queries;

import org.hyperledger.besu.consensus.pactus.ValidatorMetadata;
import org.hyperledger.besu.consensus.pactus.ValidatorSet;
import org.hyperledger.besu.consensus.pactus.statemachine.CommitteeTracker;

import java.util.*;
import java.util.stream.Collectors;

public class PactusQueryServiceImpl {
    private final ValidatorSet validatorSet;

    public PactusQueryServiceImpl(ValidatorSet validatorSet) {
        this.validatorSet = validatorSet;
    }

    public List<String> getCommittee() {
        return new ArrayList<>(CommitteeTracker.getCurrentCommittee());
    }

    public Map<String, Double> getStakes() {
        return validatorSet.getValidators().stream()
            .collect(Collectors.toMap(ValidatorMetadata::getId, ValidatorMetadata::getStake));
    }

    public String getValidatorStatus(String id) {
        Optional<ValidatorMetadata> val = validatorSet.getValidator(id);
        if (val.isPresent()) {
            return CommitteeTracker.getCurrentCommittee().contains(id) ? "InCommittee" : "Active";
        }
        return "NotFound";
    }
}