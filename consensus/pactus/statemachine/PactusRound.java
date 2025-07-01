
package org.hyperledger.besu.consensus.pactus.statemachine;

import java.util.List;
import org.hyperledger.besu.consensus.pactus.ValidatorMetadata;

public class PactusRound {
    private final int roundNumber;
    private final List<ValidatorMetadata> committee;

    public PactusRound(int roundNumber, List<ValidatorMetadata> committee) {
        this.roundNumber = roundNumber;
        this.committee = committee;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public List<ValidatorMetadata> getCommittee() {
        return committee;
    }
}
