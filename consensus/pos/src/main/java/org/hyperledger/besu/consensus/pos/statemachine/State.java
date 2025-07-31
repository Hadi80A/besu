package org.hyperledger.besu.consensus.pos.statemachine;

public enum State {
    PROPOSE,
    VOTE,
    COMMIT,
    CHANGE_PROPOSER
}