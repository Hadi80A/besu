
package org.hyperledger.besu.consensus.pactus;

import java.util.List;

public class PactusGossip {
    public void broadcast(String messageType, byte[] payload) {
        System.out.println("Broadcasting message type: " + messageType);
    }

    public void receive(String messageType, byte[] payload) {
        System.out.println("Received message of type: " + messageType);
    }
}
