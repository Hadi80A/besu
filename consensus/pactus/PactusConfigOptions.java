
package org.hyperledger.besu.consensus.pactus;

import java.util.Map;

public class PactusConfigOptions {
    private final double minStake;
    private final double threshold;
    private final int committeeSize;

    public PactusConfigOptions(Map<String, Object> config) {
        this.minStake = (double) config.getOrDefault("minStake", 1.0);
        this.threshold = (double) config.getOrDefault("stakeThreshold", 0.67);
        this.committeeSize = (int) config.getOrDefault("committeeSize", 4);
    }

    public double getMinStake() {
        return minStake;
    }

    public double getThreshold() {
        return threshold;
    }

    public int getCommitteeSize() {
        return committeeSize;
    }
}
