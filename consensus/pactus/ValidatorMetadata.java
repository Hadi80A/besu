
package org.hyperledger.besu.consensus.pactus;

public class ValidatorMetadata {
    private final String id;
    private final double stake;

    public ValidatorMetadata(String id, double stake) {
        this.id = id;
        this.stake = stake;
    }

    public String getId() {
        return id;
    }

    public double getStake() {
        return stake;
    }
}
