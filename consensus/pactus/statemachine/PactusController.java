
package org.hyperledger.besu.consensus.pactus.statemachine;

import java.util.List;
import org.hyperledger.besu.consensus.pactus.ValidatorMetadata;
import org.hyperledger.besu.consensus.pactus.ValidatorSet;

public class PactusController {
    private final ValidatorSet validatorSet;
    private int round = 0;

    public PactusController(ValidatorSet validatorSet) {
        this.validatorSet = validatorSet;
    }

    public void startConsensusRound() {
        System.out.println("Starting Pactus consensus round: " + round);
        round++;
    }

    public List<ValidatorMetadata> getCommittee() {
        return validatorSet.getValidators();
    }

    public int getCurrentRound() {
        return round;
    }
}
