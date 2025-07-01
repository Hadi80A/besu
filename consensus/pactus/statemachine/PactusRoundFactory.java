
package org.hyperledger.besu.consensus.pactus.statemachine;

import java.util.List;
import org.hyperledger.besu.consensus.pactus.ValidatorMetadata;

public class PactusRoundFactory {
    public static PactusRound createRound(int roundNumber, List<ValidatorMetadata> committee) {
        return new PactusRound(roundNumber, committee);
    }
}
