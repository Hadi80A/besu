
package org.hyperledger.besu.consensus.pactus.statemachine;

public class PactusBlockHeightManager {
    private long currentHeight = 0;

    public void incrementHeight() {
        currentHeight++;
    }

    public long getCurrentHeight() {
        return currentHeight;
    }
}
