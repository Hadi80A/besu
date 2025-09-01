/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.controller;

import org.hyperledger.besu.config.BftConfigOptions;
//import org.hyperledger.besu.config.BftFork;
import org.hyperledger.besu.config.PosConfigOptions;
import org.hyperledger.besu.config.PosFork;
import org.hyperledger.besu.consensus.common.BftValidatorOverrides;
import org.hyperledger.besu.consensus.common.EpochManager;
import org.hyperledger.besu.consensus.common.ForksSchedule;
import org.hyperledger.besu.consensus.common.bft.*;
import org.hyperledger.besu.consensus.common.bft.blockcreation.BftBlockCreatorFactory;
import org.hyperledger.besu.consensus.common.bft.blockcreation.BftMiningCoordinator;
import org.hyperledger.besu.consensus.common.bft.blockcreation.BftProposerSelector;
import org.hyperledger.besu.consensus.common.bft.blockcreation.ProposerSelector;
import org.hyperledger.besu.consensus.common.bft.network.ValidatorPeers;
import org.hyperledger.besu.consensus.common.bft.protocol.BftProtocolManager;
import org.hyperledger.besu.consensus.common.bft.statemachine.BftEventHandler;
import org.hyperledger.besu.consensus.common.bft.statemachine.BftFinalState;
import org.hyperledger.besu.consensus.common.bft.statemachine.FutureMessageBuffer;
import org.hyperledger.besu.consensus.common.validator.ValidatorProvider;
import org.hyperledger.besu.consensus.common.validator.blockbased.BlockValidatorProvider;
import org.hyperledger.besu.consensus.pos.*;
import org.hyperledger.besu.consensus.pos.core.*;
import org.hyperledger.besu.consensus.pos.protocol.PosSubProtocol;
import org.hyperledger.besu.consensus.pos.statemachine.*;
import org.hyperledger.besu.consensus.pos.validation.MessageValidatorFactory;
import org.hyperledger.besu.crypto.Hash;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.ProtocolContext;
import org.hyperledger.besu.ethereum.blockcreation.MiningCoordinator;
import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.chain.MinedBlockObserver;
import org.hyperledger.besu.ethereum.chain.MutableBlockchain;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.core.MiningConfiguration;
import org.hyperledger.besu.ethereum.core.Util;
import org.hyperledger.besu.ethereum.eth.EthProtocol;
import org.hyperledger.besu.ethereum.eth.SnapProtocol;
import org.hyperledger.besu.ethereum.eth.manager.EthProtocolManager;
import org.hyperledger.besu.ethereum.eth.manager.snap.SnapProtocolManager;
import org.hyperledger.besu.ethereum.eth.sync.state.SyncState;
import org.hyperledger.besu.ethereum.eth.transactions.TransactionPool;
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule;
import org.hyperledger.besu.ethereum.p2p.config.SubProtocolConfiguration;
import org.hyperledger.besu.ethereum.worldstate.WorldStateArchive;
import org.hyperledger.besu.evm.account.Account;
import org.hyperledger.besu.evm.worldstate.WorldState;
import org.hyperledger.besu.plugin.services.BesuEvents;
import org.hyperledger.besu.util.Subscribers;

import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import com.ibm.icu.math.BigDecimal;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The Pos besu controller builder. */
public class PosBesuControllerBuilder extends BesuControllerBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(PosBesuControllerBuilder.class);
  private BftEventQueue bftEventQueue;
  private BftConfigOptions bftConfig;
  private PosConfigOptions posConfig;
  private ForksSchedule<PosConfigOptions> forksSchedule;
  private ValidatorPeers peers;
  private PosExtraDataCodec bftExtraDataCodec;
  private BftBlockInterface bftBlockInterface;
//  private Address localAddress;

  /** Default Constructor */
  public PosBesuControllerBuilder() {}

  @Override
  protected void prepForBuild() {
    bftConfig = genesisConfigOptions.getBftConfigOptions();
    posConfig = genesisConfigOptions.getPosConfigOptions();
    bftEventQueue = new BftEventQueue(bftConfig.getMessageQueueLimit());
    forksSchedule = PosForksSchedulesFactory.create(genesisConfigOptions);
    bftExtraDataCodec = new PosExtraDataCodec();
    bftBlockInterface = new BftBlockInterface(bftExtraDataCodec);
  }

  @Override
  protected SubProtocolConfiguration createSubProtocolConfiguration(
      final EthProtocolManager ethProtocolManager,
      final Optional<SnapProtocolManager> maybeSnapProtocolManager) {

    final SubProtocolConfiguration subProtocolConfiguration =
        new SubProtocolConfiguration()
            .withSubProtocol(EthProtocol.get(), ethProtocolManager)
            .withSubProtocol(
                PosSubProtocol.get(),
                new BftProtocolManager(
                    bftEventQueue, peers, PosSubProtocol.POS, PosSubProtocol.get().getName()));
    maybeSnapProtocolManager.ifPresent(
        snapProtocolManager -> subProtocolConfiguration.withSubProtocol(SnapProtocol.get(), snapProtocolManager));
    return subProtocolConfiguration;
  }

  @Override
  protected MiningCoordinator createMiningCoordinator(
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final TransactionPool transactionPool,
      final MiningConfiguration miningConfiguration,
      final SyncState syncState,
      final EthProtocolManager ethProtocolManager) {
    final MutableBlockchain blockchain = protocolContext.getBlockchain();
    final BftExecutors bftExecutors =
        BftExecutors.create(metricsSystem, BftExecutors.ConsensusType.POS);

    Address localAddress = Util.publicKeyToAddress(nodeKey.getPublicKey());
    final BftProtocolSchedule bftProtocolSchedule = (BftProtocolSchedule) protocolSchedule;
    PosProtocolSchedule posProtocolSchedule =
            new PosProtocolSchedule(bftProtocolSchedule, protocolContext);
    PeerPublicKeyFetcher peerPublicKeyFetcher = new PeerPublicKeyFetcher(ethProtocolManager.ethContext().getEthPeers());
    final BftBlockCreatorFactory<?> bftblockCreatorFactory =
        new BftBlockCreatorFactory<>(
            transactionPool,
            protocolContext,
            bftProtocolSchedule,
            forksSchedule,
            miningConfiguration,
            localAddress,
            bftExtraDataCodec,
            ethProtocolManager.ethContext().getScheduler());
    final PosBlockCreatorFactory  blockCreatorFactory =
            new PosBlockCreatorFactory(
                    bftblockCreatorFactory,
                    bftExtraDataCodec
            );

    final ValidatorProvider validatorProvider =
        protocolContext.getConsensusContext(BftContext.class).getValidatorProvider();

    final ProposerSelector proposerSelector =
        new BftProposerSelector(blockchain, bftBlockInterface, true, validatorProvider);

    // NOTE: peers should not be used for accessing the network as it does not enforce the
    // "only send once" filter applied by the UniqueMessageMulticaster.
    peers = new ValidatorPeers(validatorProvider, PosSubProtocol.NAME);

    final UniqueMessageMulticaster uniqueMessageMulticaster =
        new UniqueMessageMulticaster(peers, bftConfig.getGossipedHistoryLimit());

    final PosGossip gossiper = new PosGossip(uniqueMessageMulticaster);

    final BftFinalState bftfinalState =
        new BftFinalState(
            validatorProvider,
            nodeKey,
            Util.publicKeyToAddress(nodeKey.getPublicKey()),
            proposerSelector,
            uniqueMessageMulticaster,
            new RoundTimer(
                bftEventQueue,
                    new BftRoundExpiryTimeCalculator(
                      Duration.ofSeconds(bftConfig.getRequestTimeoutSeconds())
                    ),
                bftExecutors),
            new BlockTimer(bftEventQueue, forksSchedule, bftExecutors, clock),
                bftblockCreatorFactory,
            clock);
    final PosFinalState posFinalState=new PosFinalState(
            validatorProvider,
            nodeKey,
            Util.publicKeyToAddress(nodeKey.getPublicKey()),
            proposerSelector,
            uniqueMessageMulticaster,
            new RoundTimer(
                    bftEventQueue,
                    new BftRoundExpiryTimeCalculator(
                    Duration.ofSeconds(bftConfig.getRequestTimeoutSeconds())
                    ),
                    bftExecutors),
            new BlockTimer(bftEventQueue, forksSchedule, bftExecutors, clock),
            blockCreatorFactory,
            clock,
            bftfinalState

    );
    final MessageValidatorFactory messageValidatorFactory =
        new MessageValidatorFactory(
            proposerSelector, bftProtocolSchedule, protocolContext, bftExtraDataCodec);

    final Subscribers<PosMinedBlockObserver> minedBlockObservers = Subscribers.create();
    minedBlockObservers.subscribe(posBlock -> ethProtocolManager.blockMined(BlockUtil.toBesuBlock(posBlock)));
    minedBlockObservers.subscribe(posBlock ->
            blockLogger(transactionPool, localAddress)
                    .blockMined(BlockUtil.toBesuBlock(posBlock)));

    final FutureMessageBuffer futureMessageBuffer =
        new FutureMessageBuffer(
            bftConfig.getFutureMessagesMaxDistance(),
            bftConfig.getFutureMessagesLimit(),
            blockchain.getChainHeadBlockNumber());
    final MessageTracker duplicateMessageTracker =
        new MessageTracker(bftConfig.getDuplicateMessageLimit());

    final PosRoundFactory.MessageFactory messageFactory = new PosRoundFactory.MessageFactory();
    NodeSet nodeSet = createNodeSet(protocolContext);
    ContractCaller contractCaller =
        new ContractCaller(posConfig.getContractAddress(), protocolContext);


    PosProposerSelector posProposerSelector = null;
    if (  nodeSet.getNode(localAddress).isPresent()) {
      posProposerSelector = new PosProposerSelector(nodeSet, nodeKey,
              nodeSet.getNode(localAddress).get().getStakeInfo().getStakedAmount());
    }
    final BftEventHandler posController =
        new PosController(
            blockchain,
                posFinalState,
            new PosBlockHeightManagerFactory(
                  posFinalState,
                new PosRoundFactory(
                    posFinalState,
                    protocolContext,
                    posProtocolSchedule,
                    posConfig,
                    minedBlockObservers,
                    messageValidatorFactory,
                    messageFactory,
                    bftExtraDataCodec,
                    contractCaller,
                    nodeSet,
                    posProposerSelector,
                    peerPublicKeyFetcher
                ),
                messageValidatorFactory,
                    posConfig,
                messageFactory,
                posProposerSelector,
                peerPublicKeyFetcher,
                syncState

            ),
            gossiper,
            duplicateMessageTracker,
            futureMessageBuffer,
            new EthSynchronizerUpdater(ethProtocolManager.ethContext().getEthPeers()));

    final EventMultiplexer eventMultiplexer = new EventMultiplexer(posController);
    final BftProcessor bftProcessor = new BftProcessor(bftEventQueue, eventMultiplexer);

    final MiningCoordinator posMiningCoordinator =
        new BftMiningCoordinator(
            bftExecutors,
            posController,
            bftProcessor,
             bftblockCreatorFactory,
            blockchain,
            bftEventQueue);

    // Update the next block period in seconds according to the transition schedule
    protocolContext
        .getBlockchain()
        .observeBlockAdded(
            o ->
                miningConfiguration.setBlockPeriodSeconds(
                    forksSchedule
                        .getFork(o.getBlock().getHeader().getNumber() + 1)
                        .getValue()
                        .getBlockPeriodSeconds()));

    syncState.subscribeSyncStatus(
        syncStatus -> {
          if (syncState.syncTarget().isPresent()) {
            // We're syncing so stop doing other stuff
            LOG.info("Stopping POS mining coordinator while we are syncing");
            posMiningCoordinator.stop();
          } else {
            LOG.info("Starting POS mining coordinator following sync");
            posMiningCoordinator.enable();
            posMiningCoordinator.start();
          }
        });

    syncState.subscribeCompletionReached(
        new BesuEvents.InitialSyncCompletionListener() {
          @Override
          public void onInitialSyncCompleted() {
            LOG.info("Starting POS mining coordinator following initial sync");
            posMiningCoordinator.enable();
            posMiningCoordinator.start();
          }

          @Override
          public void onInitialSyncRestart() {
            // Nothing to do. The mining coordinator won't be started until
            // sync has completed.
          }
        });

    return posMiningCoordinator;
  }

  private NodeSet createNodeSet(ProtocolContext protocolContext) {

    WorldStateArchive worldStateArchive = protocolContext.getWorldStateArchive();
    Blockchain blockchain = protocolContext.getBlockchain();

    BlockHeader genesisHeader =
        blockchain
            .getBlockHeader(0)
            .orElseThrow(() -> new RuntimeException("Genesis block not found"));

    WorldState worldState =
        worldStateArchive
            .get(genesisHeader.getStateRoot(), genesisHeader.getHash())
            .orElseThrow(() -> new RuntimeException("Genesis state not available"));

    NodeSet nodeSet = new NodeSet();

    // Use your custom codec to decode extraData
    PosExtraDataCodec codec = new PosExtraDataCodec();
    BftExtraData bftExtraData = codec.decodeRaw(genesisHeader.getExtraData());

    // Extract validator addresses
    Collection<Address> validators = bftExtraData.getValidators();

    // 4. Contract address
    //    Address stakeManager =
    // Address.fromHexString("0x1234567890123456789012345678901234567890");
    Address stakeManager = posConfig.getContractAddress();

    System.out.println("========== Validator Wallets ==========");
    System.out.printf(
        "%-20s | %-42s | %-15s | %-15s%n",
        "Validator ID", "Address", "Balance (ETH)", "Stake (ETH)");
    System.out.println(
        "---------------------------------------------------------------------------");

    int validatorCount = 0;
    for (Address validator : validators) {
      validatorCount++;
      String id = "Validator-" + validatorCount;
      // Get account balance from world state
      Account account = worldState.get(validator);
      BigInteger balanceWei =
          account != null ? account.getBalance().toBigInteger() : BigInteger.ZERO;

      BigDecimal balanceEth = weiToEth(balanceWei);

      // Get stake from contract
      BigInteger stakeWei = getValidatorStake(worldState, stakeManager, validator);
      BigDecimal stakeEth = weiToEth(stakeWei);

      System.out.printf(
          "%-20s | %-42s | %-15s | %-15s%n",
          id, validator.toHexString(), balanceEth.toString(), stakeEth.toString());

      // Build node info (customize as needed)
      StakeInfo stake = StakeInfo.builder().stakedAmount(100).build();//todo

      Node node =
          Node.builder()
              .id(id)
              .address(validator)
              .inCommittee(false)
              .stakeInfo(stake)
              .blocksProposed(0)
              .lastProposedAt(0)
              .build();
      LOG.debug("stake:{}", node.getStakeInfo().getStakedAmount());
      nodeSet.addOrUpdateNode(node);
    }

    System.out.println("Total validators: " + validators.size());

    //
    //    System.out.println("========== Genesis Accounts ==========");
    //    System.out.printf("%-20s | %-42s | %-20s%n", "Account ID", "Address", "Balance");
    //    System.out.println("------------------------------------------------------------");
    //
    //    AtomicInteger accountCount = new AtomicInteger(0);
    //    NodeSet nodeSet = new NodeSet();
    //
    //    worldState.streamAccounts(Bytes32.ZERO, Integer.MAX_VALUE).forEach(account -> {
    //      if (account.getAddress().isPresent()) {
    //        int count = accountCount.incrementAndGet();
    //        Address address = account.getAddress().get();
    //        String id = "Node-" + count;
    //        BigInteger balance = account.getBalance().toBigInteger();
    //
    //        // Convert wei to ETH
    //        BigDecimal balanceEth = new BigDecimal(balance)
    //                .divide(new BigDecimal(1_000_000_000_000_000_000L), 6,
    // RoundingMode.HALF_UP.ordinal());
    //
    //        System.out.printf("%-20s | %-42s | %-20s ETH%n",
    //                id,
    //                address.toHexString(),
    //                balanceEth.toString());
    //
    //        // Build node info (customize as needed)
    //        StakeInfo stake = StakeInfo.builder()
    //                .stakedAmount(0)
    //                .active(false)
    //                .build();
    //
    //        Node node = Node.builder()
    //                .id(id)
    //                .address(address)
    //                .inCommittee(false)
    //                .stakeInfo(stake)
    //                .blocksProposed(0)
    //                .lastProposedAt(0)
    //                .build();
    //
    //        nodeSet.addOrUpdateNode(node);
    //      }
    //    });
    //
    //    System.out.println("Total genesis accounts: " + accountCount.get());

    return nodeSet;
  }

  private BigDecimal weiToEth(BigInteger wei) {
    return new BigDecimal(wei)
        .divide(new BigDecimal("1000000000000000000"), 6, RoundingMode.HALF_UP.ordinal());
  }

  private BigInteger getValidatorStake(
      WorldState worldState, Address contractAddress, Address validatorAddress) {
    // 1. Get contract account
    Account contractAccount = worldState.get(contractAddress);
    if (contractAccount == null || contractAccount.isEmpty()) {
      System.out.println("contractAccount is null or empty");
      return BigInteger.ZERO;
    }

    // 2. Compute storage slot for validator's stake
    // Slot = keccak256(validatorAddress + slot_index)
    // slot_index = 0 (first slot in the contract storage layout)
    Bytes32 key = Bytes32.leftPad(validatorAddress);
    Bytes32 slotIndex = Bytes32.leftPad(Bytes.of(0)); // Slot 0 for mapping
    Bytes concatenated = Bytes.concatenate(key, slotIndex);
    Bytes32 slotHash = Hash.keccak256(concatenated);

    // 3. Read storage value at computed slot
    UInt256 stakeValue =
        contractAccount.getStorageValue(UInt256.valueOf(slotHash.toUnsignedBigInteger()));
    return stakeValue.toBigInteger();
  }

  @Override
  protected PluginServiceFactory createAdditionalPluginServices(
      final Blockchain blockchain, final ProtocolContext protocolContext) {
    final ValidatorProvider validatorProvider =
        protocolContext.getConsensusContext(BftContext.class).getValidatorProvider();
    return new PosQueryPluginServiceFactory(
        blockchain, bftBlockInterface, validatorProvider, nodeKey);
  }

  @Override
  protected ProtocolSchedule createProtocolSchedule() {
    return PosProtocolScheduleBuilder.create(
        genesisConfigOptions,
        forksSchedule,
        isRevertReasonEnabled,
        bftExtraDataCodec,
        evmConfiguration,
        miningConfiguration,
        badBlockManager,
        isParallelTxProcessingEnabled,
        metricsSystem);
  }

  @Override
  protected void validateContext(final ProtocolContext context) {
    final BlockHeader genesisBlockHeader = context.getBlockchain().getGenesisBlock().getHeader();

    if (bftBlockInterface.validatorsInBlock(genesisBlockHeader).isEmpty()) {
      LOG.warn("Genesis block contains no signers - chain will not progress.");
    }
  }

  @Override
  protected BftContext createConsensusContext(
      final Blockchain blockchain,
      final WorldStateArchive worldStateArchive,
      final ProtocolSchedule protocolSchedule) {
    final PosConfigOptions posConfig = genesisConfigOptions.getPosConfigOptions();
    final EpochManager epochManager = new EpochManager(posConfig.getEpochLength());

    final BftValidatorOverrides validatorOverrides =
        convertPosForks(genesisConfigOptions.getTransitions().getPosForks());

    return new BftContext(
        BlockValidatorProvider.forkingValidatorProvider(
            blockchain, epochManager, bftBlockInterface, validatorOverrides),
        epochManager,
        bftBlockInterface);
  }

  private BftValidatorOverrides convertPosForks(final List<PosFork> posForks) {
    final Map<Long, List<Address>> result = new HashMap<>();

    for (final PosFork fork : posForks) {
      fork.getValidators()
          .ifPresent(
              validators ->
                  result.put(
                      fork.getForkBlock(),
                      validators.stream()
                          .map(Address::fromHexString)
                          .collect(Collectors.toList())));
    }

    return new BftValidatorOverrides(result);
  }

  private static MinedBlockObserver blockLogger(
      final TransactionPool transactionPool, final Address localAddress) {
    return block ->
        LOG.info(
            String.format(
                "%s #%,d / %d tx / %d pending / %,d (%01.1f%%) gas / (%s)",
                block.getHeader().getCoinbase().equals(localAddress) ? "Produced" : "Imported",
                block.getHeader().getNumber(),
                block.getBody().getTransactions().size(),
                transactionPool.count(),
                block.getHeader().getGasUsed(),
                (block.getHeader().getGasUsed() * 100.0) / block.getHeader().getGasLimit(),
                block.getHash().toHexString()));
  }
}
