
package org.hyperledger.besu.consensus.pactus.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PactusPubSubStub {
    private final List<Consumer<String>> subscribers = new ArrayList<>();

    public void subscribe(Consumer<String> callback) {
        subscribers.add(callback);
    }

    public void publish(String message) {
        for (Consumer<String> subscriber : subscribers) {
            subscriber.accept(message);
        }
    }
}
