
package org.hyperledger.besu.consensus.pactus.statemachine;

public class PactusBlockHeightManagerFactory {
    public static PactusBlockHeightManager create() {
        return new PactusBlockHeightManager();
    }
}
