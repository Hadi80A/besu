
package org.hyperledger.besu.consensus.pactus.payload;

public interface PactusPayload {
    String getType();
    byte[] encode();
}
