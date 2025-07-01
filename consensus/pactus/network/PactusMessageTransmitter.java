
package org.hyperledger.besu.consensus.pactus.network;

public class PactusMessageTransmitter {
    public void send(String recipientId, byte[] message) {
        System.out.println("Sending message to: " + recipientId);
    }

    public void broadcast(byte[] message) {
        System.out.println("Broadcasting message to all validators.");
    }
}
