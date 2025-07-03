// PactusProtocolScheduleBuilder.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus;

import org.hyperledger.besu.config.GenesisConfigOptions;
import org.hyperledger.besu.ethereum.chain.ChainId;
import org.hyperledger.besu.ethereum.chain.ChainIdLong;
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule;
import org.hyperledger.besu.ethereum.mainnet.ProtocolScheduleBuilder;
import org.hyperledger.besu.ethereum.speculation.SpeculativeExecution;
import org.hyperledger.besu.ethereum.mainnet.ProtocolSpecAdapters;

/**
 * Builds a ProtocolSchedule instance that applies Pactus consensus rules.
 * Used to define block validation, header checks, and consensus settings.
 */
public class PactusProtocolScheduleBuilder {

  private final GenesisConfigOptions genesisConfigOptions;

  public PactusProtocolScheduleBuilder(final GenesisConfigOptions genesisConfigOptions) {
    this.genesisConfigOptions = genesisConfigOptions;
  }

  /**
   * Builds the ProtocolSchedule for Pactus consensus.
   */
  public ProtocolSchedule createProtocolSchedule() {
    return new ProtocolScheduleBuilder(
        genesisConfigOptions,
        ChainId.from(genesisConfigOptions.getChainId().orElse(ChainIdLong.MAINNET)),
        ProtocolSpecAdapters.createDefault(),
        false,
        new PactusBlockHeaderValidationRulesetFactory()::createValidator
    ).createProtocolSchedule();
  }
}
