package org.hyperledger.besu.consensus.pactus.statemachine;

public enum State {
    PROPOSE,
    PREPARE,
    PRE_COMMIT,
    COMMIT,
    CHANGE_PROPOSER
}
