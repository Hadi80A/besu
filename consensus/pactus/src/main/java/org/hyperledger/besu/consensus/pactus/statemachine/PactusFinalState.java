// PactusFinalState.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.statemachine;

import org.hyperledger.besu.consensus.pactus.core.ValidatorSet;
import org.hyperledger.besu.consensus.pactus.network.PactusMessageTransmitter;
import org.hyperledger.besu.consensus.pactus.payload.MessageFactory;

/**
 * Represents shared, immutable state used across Pactus consensus rounds and heights.
 * Acts as a container for dependencies like validators, messaging, and local identity.
 */
public class PactusFinalState {

  private final ValidatorSet validatorSet;
  private final PactusMessageTransmitter messageTransmitter;
  private final MessageFactory messageFactory;
  private final String localValidatorId;

  public PactusFinalState(
      final ValidatorSet validatorSet,
      final PactusMessageTransmitter messageTransmitter,
      final MessageFactory messageFactory,
      final String localValidatorId) {
    this.validatorSet = validatorSet;
    this.messageTransmitter = messageTransmitter;
    this.messageFactory = messageFactory;
    this.localValidatorId = localValidatorId;
  }

  public ValidatorSet getValidatorSet() {
    return validatorSet;
  }

  public PactusMessageTransmitter getMessageTransmitter() {
    return messageTransmitter;
  }

  public MessageFactory getMessageFactory() {
    return messageFactory;
  }

  public String getLocalValidatorId() {
    return localValidatorId;
  }
}
