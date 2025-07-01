
package org.hyperledger.besu.consensus.pactus.payload;

public class PayloadDeserializers {
    public static String decode(byte[] data) {
        return new String(data);
    }
}
