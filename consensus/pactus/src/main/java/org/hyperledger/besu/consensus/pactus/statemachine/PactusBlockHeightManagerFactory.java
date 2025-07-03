// PactusBlockHeightManagerFactory.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.statemachine;

import org.hyperledger.besu.consensus.pactus.core.ValidatorSet;
import org.hyperledger.besu.consensus.pactus.payload.MessageFactory;

/**
 * Factory class to create new instances of PactusBlockHeightManager.
 * This is invoked each time the consensus progresses to a new block height.
 */
public class PactusBlockHeightManagerFactory {

  private final ValidatorSet validatorSet;
  private final MessageFactory messageFactory;

  public PactusBlockHeightManagerFactory(
      final ValidatorSet validatorSet,
      final MessageFactory messageFactory) {
    this.validatorSet = validatorSet;
    this.messageFactory = messageFactory;
  }

  /**
   * Creates a new BlockHeightManager for a specific block height.
   *
   * @param height the height to manage
   * @return a configured PactusBlockHeightManager
   */
  public PactusBlockHeightManager createForHeight(final long height) {
    return new PactusBlockHeightManager(height, validatorSet, messageFactory);
  }
}
