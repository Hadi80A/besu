package org.hyperledger.besu.consensus.pactus.statemachine;

import org.hyperledger.besu.consensus.pactus.core.Block;
import org.hyperledger.besu.consensus.pactus.core.ValidatorSet;
import org.hyperledger.besu.consensus.pactus.network.PactusMessageTransmitter;
import org.hyperledger.besu.consensus.pactus.payload.MessageFactory;
import org.hyperledger.besu.consensus.pactus.deployment.StakeManagerDeployer;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.core.MutableBlockchain;
import org.hyperledger.besu.ethereum.core.WorldUpdater;

/**
 * Main controller of the Pactus consensus process.
 * Orchestrates round transitions, message propagation, and block height progression.
 */
public class PactusController {

  private long currentHeight;
  private final ValidatorSet validatorSet;
  private final MessageFactory messageFactory;
  private final PactusMessageTransmitter messageTransmitter;
  private final PactusBlockHeightManagerFactory heightManagerFactory;
  private final StakeManagerDeployer stakeDeployer;

  private PactusBlockHeightManager currentHeightManager;

  public PactusController(
      final long startHeight,
      final ValidatorSet validatorSet,
      final MessageFactory messageFactory,
      final PactusMessageTransmitter messageTransmitter,
      final MutableBlockchain blockchain,
      final WorldUpdater worldState) {

    this.currentHeight = startHeight;
    this.validatorSet = validatorSet;
    this.messageFactory = messageFactory;
    this.messageTransmitter = messageTransmitter;
    this.heightManagerFactory = new PactusBlockHeightManagerFactory(validatorSet, messageFactory);

    // 💡 Use known deployer address (must be funded in genesis.json)
    final Address deployerAddress = Address.fromHexString("0x000000000000000000000000000000000000abcd");
    this.stakeDeployer = new StakeManagerDeployer(blockchain, worldState, deployerAddress, 1337L);

    this.currentHeightManager = heightManagerFactory.createForHeight(startHeight);
  }

  /**
   * Begins consensus for the current block height.
   */
  public void startConsensus() {
    // 🚀 Auto-deploy StakeManager if missing
    stakeDeployer.deployIfNotExists();

    currentHeightManager.startRound();
  }

  /**
   * Transitions to the next block height.
   *
   * @param newBlock the block that was finalized and appended to the chain
   */
  public void moveToNextHeight(final Block newBlock) {
    this.currentHeight++;
    this.currentHeightManager = heightManagerFactory.createForHeight(currentHeight);
    this.currentHeightManager.startRound();
  }

  /**
   * Advances the current round manually (e.g., timeout or failed quorum).
   */
  public void advanceRound() {
    currentHeightManager.incrementRound();
    currentHeightManager.startRound();
  }

  /**
   * Returns the current block height.
   */
  public long getCurrentHeight() {
    return currentHeight;
  }

  /**
   * Returns the manager responsible for the current height and round.
   */
  public PactusBlockHeightManager getCurrentHeightManager() {
    return currentHeightManager;
  }
}
