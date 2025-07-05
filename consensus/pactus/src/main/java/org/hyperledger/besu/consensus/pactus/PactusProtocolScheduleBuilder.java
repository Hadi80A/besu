// PactusProtocolScheduleBuilder.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus;

import org.hyperledger.besu.config.BftConfigOptions;
import org.hyperledger.besu.config.GenesisConfigOptions;
import org.hyperledger.besu.consensus.common.ForksSchedule;
import org.hyperledger.besu.consensus.common.bft.BaseBftProtocolScheduleBuilder;
import org.hyperledger.besu.consensus.common.bft.BftExtraDataCodec;
import org.hyperledger.besu.consensus.common.bft.BftProtocolSchedule;
import org.hyperledger.besu.ethereum.chain.BadBlockManager;

import org.hyperledger.besu.ethereum.core.MiningConfiguration;
import org.hyperledger.besu.ethereum.mainnet.BlockHeaderValidator;
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule;
import org.hyperledger.besu.ethereum.mainnet.ProtocolScheduleBuilder;
import org.hyperledger.besu.ethereum.mainnet.feemarket.BaseFeeMarket;
import org.hyperledger.besu.ethereum.mainnet.feemarket.FeeMarket;
import org.hyperledger.besu.ethereum.mainnet.ProtocolSpecAdapters;
import org.hyperledger.besu.evm.internal.EvmConfiguration;
import org.hyperledger.besu.plugin.services.MetricsSystem;

import java.time.Duration;
import java.util.Optional;

/**
 * Builds a ProtocolSchedule instance that applies Pactus consensus rules.
 * Used to define block validation, header checks, and consensus settings.
 */
public class PactusProtocolScheduleBuilder extends BaseBftProtocolScheduleBuilder {

  /** Default constructor. */
  protected PactusProtocolScheduleBuilder() {}

  /**
   * Create protocol schedule.
   *
   * @param config the config
   * @param forksSchedule the forks schedule
   * @param isRevertReasonEnabled the is revert reason enabled
   * @param bftExtraDataCodec the bft extra data codec
   * @param evmConfiguration the evm configuration
   * @param miningConfiguration the mining parameters
   * @param badBlockManager the cache to use to keep invalid blocks
   * @param isParallelTxProcessingEnabled indicates whether parallel transaction is enabled
   * @param metricsSystem A metricSystem instance to be able to expose metrics in the underlying
   *     calls
   * @return the protocol schedule
   */
  public static BftProtocolSchedule create(
          final GenesisConfigOptions config,
          final ForksSchedule<BftConfigOptions> forksSchedule,
          final boolean isRevertReasonEnabled,
          final BftExtraDataCodec bftExtraDataCodec,
          final EvmConfiguration evmConfiguration,
          final MiningConfiguration miningConfiguration,
          final BadBlockManager badBlockManager,
          final boolean isParallelTxProcessingEnabled,
          final MetricsSystem metricsSystem) {
    return new PactusProtocolScheduleBuilder()
            .createProtocolSchedule(
                    config,
                    forksSchedule,
                    isRevertReasonEnabled,
                    bftExtraDataCodec,
                    evmConfiguration,
                    miningConfiguration,
                    badBlockManager,
                    isParallelTxProcessingEnabled,
                    metricsSystem);
  }

  /**
   * Create protocol schedule.
   *
   * @param config the config
   * @param forksSchedule the forks schedule
   * @param bftExtraDataCodec the bft extra data codec
   * @param evmConfiguration the evm configuration
   * @param miningConfiguration the mining parameters
   * @param badBlockManager the cache to use to keep invalid blocks
   * @param isParallelTxProcessingEnabled indicates whether parallel transaction is enabled.
   * @param metricsSystem A metricSystem instance to be able to expose metrics in the underlying
   *     calls
   * @return the protocol schedule
   */
  public static BftProtocolSchedule create(
          final GenesisConfigOptions config,
          final ForksSchedule<BftConfigOptions> forksSchedule,
          final BftExtraDataCodec bftExtraDataCodec,
          final EvmConfiguration evmConfiguration,
          final MiningConfiguration miningConfiguration,
          final BadBlockManager badBlockManager,
          final boolean isParallelTxProcessingEnabled,
          final MetricsSystem metricsSystem) {
    return create(
            config,
            forksSchedule,
            false,
            bftExtraDataCodec,
            evmConfiguration,
            miningConfiguration,
            badBlockManager,
            isParallelTxProcessingEnabled,
            metricsSystem);
  }

  @Override
  protected BlockHeaderValidator.Builder createBlockHeaderRuleset(
          final BftConfigOptions config, final FeeMarket feeMarket) {
    final Optional<BaseFeeMarket> baseFeeMarket =
            Optional.of(feeMarket).filter(FeeMarket::implementsBaseFee).map(BaseFeeMarket.class::cast);

    return PactusBlockHeaderValidationRulesetFactory.blockHeaderValidator(
            config.getBlockPeriodMilliseconds() > 0
                    ? Duration.ofMillis(config.getBlockPeriodMilliseconds())
                    : Duration.ofSeconds(config.getBlockPeriodSeconds()),
            baseFeeMarket);
  }

//  private final GenesisConfigOptions genesisConfigOptions;
//
//  public PactusProtocolScheduleBuilder(final GenesisConfigOptions genesisConfigOptions) {
//    this.genesisConfigOptions = genesisConfigOptions;
//  }
//
//  /**
//   * Builds the ProtocolSchedule for Pactus consensus.
//   */
//  public ProtocolSchedule createProtocolSchedule() {
//    return new ProtocolScheduleBuilder(
//        genesisConfigOptions,
//        ChainId.from(genesisConfigOptions.getChainId().orElse(ChainIdLong.MAINNET)),
//        ProtocolSpecAdapters.createDefault(),
//        false,
//        new PactusBlockHeaderValidationRulesetFactory()::createValidator
//    ).createProtocolSchedule();
//  }
}
