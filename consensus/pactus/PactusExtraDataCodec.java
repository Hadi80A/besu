
package org.hyperledger.besu.consensus.pactus;

import java.util.Optional;

public class PactusExtraDataCodec {
    public static Optional<String> decodeExtraData(byte[] extraData) {
        try {
            return Optional.of(new String(extraData));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
