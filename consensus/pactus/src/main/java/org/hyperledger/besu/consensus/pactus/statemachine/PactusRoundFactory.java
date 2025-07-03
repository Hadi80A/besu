package org.hyperledger.besu.consensus.pactus.statemachine;

import org.hyperledger.besu.consensus.pactus.network.PactusMessageTransmitter;
import org.hyperledger.besu.consensus.pactus.payload.MessageFactory;

/**
 * Factory to create PactusRound instances for a specific height and round.
 */
public class PactusRoundFactory {

  private final String localValidatorId;
  private final MessageFactory messageFactory;
  private final PactusMessageTransmitter messageTransmitter;

  public PactusRoundFactory(
      final String localValidatorId,
      final MessageFactory messageFactory,
      final PactusMessageTransmitter messageTransmitter) {
    this.localValidatorId = localValidatorId;
    this.messageFactory = messageFactory;
    this.messageTransmitter = messageTransmitter;
  }

  /**
   * Create a new PactusRound instance.
   *
   * @param height the block height for the round
   * @param round the round number
   * @return a new PactusRound
   */
  public PactusRound createNewRound(final long height, final int round) {
    return new PactusRound(height, round, localValidatorId, messageFactory, messageTransmitter);
  }
}
