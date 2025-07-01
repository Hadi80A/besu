
package org.hyperledger.besu.consensus.pactus.config;

import java.util.Map;

public class PactusGenesisConfigLoader {
    public static Map<String, Object> load() {
        return Map.of(
            "minStake", 100.0,
            "stakeThreshold", 0.67,
            "committeeSize", 4
        );
    }
}
