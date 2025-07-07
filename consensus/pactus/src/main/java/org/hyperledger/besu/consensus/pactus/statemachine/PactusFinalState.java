// PactusFinalState.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.statemachine;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.hyperledger.besu.consensus.common.bft.BftHelpers;
import org.hyperledger.besu.consensus.common.bft.BlockTimer;
import org.hyperledger.besu.consensus.common.bft.RoundTimer;
import org.hyperledger.besu.consensus.pactus.core.ValidatorSet;
import org.hyperledger.besu.consensus.pactus.network.PactusMessageTransmitter;
import org.hyperledger.besu.consensus.pactus.payload.MessageFactory;
import org.hyperledger.besu.datatypes.Address;

import java.time.Clock;

/**
 * Represents shared, immutable state used across Pactus consensus rounds and heights.
 * Acts as a container for dependencies like validators, messaging, and local identity.
 */
@Data
@AllArgsConstructor
public class PactusFinalState {

  private final ValidatorSet validatorSet;
  private final PactusMessageTransmitter messageTransmitter;
  private final Address localAddress;

  private final MessageFactory messageFactory;
  private final String localValidatorId;
  private final RoundTimer roundTimer;
  private final BlockTimer blockTimer;
  private final Clock clock;

  public int getQuorum() {
    return BftHelpers.calculateRequiredValidatorQuorum(validatorSet.committeeSize());
  }

}
