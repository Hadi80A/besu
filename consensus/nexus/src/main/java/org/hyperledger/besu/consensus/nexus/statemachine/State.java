package org.hyperledger.besu.consensus.nexus.statemachine;

public enum State {
    PROPOSE,
    VOTE,
    COMMIT,
    CHANGE_PROPOSER
}