
package org.hyperledger.besu.consensus.pactus.validation;

import java.util.HashMap;
import java.util.Map;

public class ByzantineBehaviorDetector {
    private final Map<String, Integer> faultCounts = new HashMap<>();
    private final int faultThreshold = 2;

    public void reportFault(String validatorId) {
        faultCounts.put(validatorId, faultCounts.getOrDefault(validatorId, 0) + 1);
    }

    public boolean isByzantine(String validatorId) {
        return faultCounts.getOrDefault(validatorId, 0) >= faultThreshold;
    }
}
