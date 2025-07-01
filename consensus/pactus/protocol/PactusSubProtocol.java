
package org.hyperledger.besu.consensus.pactus.protocol;

public class PactusSubProtocol {
    public static final String NAME = "PACTUS";
    public static final int VERSION = 1;

    public String getName() {
        return NAME;
    }

    public int getVersion() {
        return VERSION;
    }
}
