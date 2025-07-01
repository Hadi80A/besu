
package org.hyperledger.besu.consensus.pactus.messagedata;

public class PactusV1 {
    public static final int VERSION = 1;

    public static String identify(String type) {
        return "PACTUSv1-" + type;
    }
}
