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
import org.hyperledger.besu.consensus.pos.bls.Bls;
import org.hyperledger.besu.consensus.pos.core.*;
import org.hyperledger.besu.consensus.pos.protocol.PosSubProtocol;
import org.hyperledger.besu.consensus.pos.statemachine.*;
import org.hyperledger.besu.consensus.pos.validation.MessageValidatorFactory;
import org.hyperledger.besu.crypto.Hash;
import org.hyperledger.besu.crypto.SECPPublicKey;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.ProtocolContext;
import org.hyperledger.besu.ethereum.blockcreation.MiningCoordinator;
import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.chain.DefaultBlockchain;
import org.hyperledger.besu.ethereum.chain.MinedBlockObserver;
import org.hyperledger.besu.ethereum.chain.MutableBlockchain;
import org.hyperledger.besu.ethereum.core.*;
import org.hyperledger.besu.ethereum.eth.EthProtocol;
import org.hyperledger.besu.ethereum.eth.SnapProtocol;
import org.hyperledger.besu.ethereum.eth.manager.EthProtocolManager;
import org.hyperledger.besu.ethereum.eth.manager.snap.SnapProtocolManager;
import org.hyperledger.besu.ethereum.eth.sync.state.SyncState;
import org.hyperledger.besu.ethereum.eth.transactions.TransactionPool;
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule;
import org.hyperledger.besu.ethereum.p2p.config.SubProtocolConfiguration;
import org.hyperledger.besu.ethereum.rlp.RLP;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.hyperledger.besu.ethereum.trie.pathbased.common.provider.WorldStateQueryParams;
import org.hyperledger.besu.ethereum.worldstate.WorldStateArchive;
import org.hyperledger.besu.evm.account.Account;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.worldstate.WorldState;
import org.hyperledger.besu.evm.worldstate.WorldUpdater;
import org.hyperledger.besu.plugin.services.BesuEvents;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorageTransaction;
import org.hyperledger.besu.util.Subscribers;

import java.io.IOException;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
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
  private PosExtraDataCodec posExtraDataCodec;
  private BftBlockInterface bftBlockInterface;
  private Path dataDir;
//  private Address localAddress;

  /** Default Constructor */
  public PosBesuControllerBuilder() {}

  @Override
  protected void prepForBuild() {
    bftConfig = genesisConfigOptions.getBftConfigOptions();
    posConfig = genesisConfigOptions.getPosConfigOptions();
    bftEventQueue = new BftEventQueue(bftConfig.getMessageQueueLimit());
    forksSchedule = PosForksSchedulesFactory.create(genesisConfigOptions);
    posExtraDataCodec = new PosExtraDataCodec();
    bftBlockInterface = new BftBlockInterface(posExtraDataCodec);
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
    PosProtocolSchedule posProtocolSchedule = new PosProtocolSchedule(bftProtocolSchedule, protocolContext);
    final BftBlockCreatorFactory<?> bftblockCreatorFactory =
        new BftBlockCreatorFactory<>(
            transactionPool,
            protocolContext,
            bftProtocolSchedule,
            forksSchedule,
            miningConfiguration,
            localAddress,
                posExtraDataCodec,
            ethProtocolManager.ethContext().getScheduler());
    final PosBlockCreatorFactory  blockCreatorFactory =
            new PosBlockCreatorFactory(
                    bftblockCreatorFactory,
                    posExtraDataCodec
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
            proposerSelector, bftProtocolSchedule, protocolContext, posExtraDataCodec);

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

      MutableWorldState worldState = getMutableWorldState(protocolContext, blockchain);
      if(blockchain.getChainHeadBlockNumber()==0){
        readInitialStake(worldState,blockchain);
          overwriteGenesisStateRoot(worldState,protocolContext,protocolSchedule);
    }
    NodeSet nodeSet = createNodeSet(worldState,blockchain);
    ContractCaller contractCaller =
        new ContractCaller(posConfig.getContractAddress(), protocolContext);

    PosProposerSelector posProposerSelector = null;
    if (  nodeSet.getNode(localAddress).isPresent()) {
      posProposerSelector = new PosProposerSelector(nodeSet, nodeKey,
              nodeSet.getNode(localAddress).get().getStakeInfo().getStakedAmount(),posConfig);
    }
      Bls.KeyPair blsKeyPair;
      try {
          blsKeyPair=readBlsKeyPair(); //todo: pass to posController
          LOG.debug("blsPrivateKey:{}",blsKeyPair.getSecretKey().toHexString());
          LOG.debug("blsPublicKey:{}",blsKeyPair.getPublicKey().toHexString());
      } catch (IOException e) {
          throw new RuntimeException(e);
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
                        posExtraDataCodec,
                    contractCaller,
                    nodeSet,
                    posProposerSelector
                ),
                messageValidatorFactory,
                    posConfig,
                messageFactory,
                posProposerSelector,
                ethProtocolManager.ethContext().getEthPeers(),
                syncState,
                    blsKeyPair
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

    // ---------- helpers ----------
    private static Bytes32 computeMappingSlotForAddress(final Address addr, final long mappingSlotIndex) {
        // pad address to 32 bytes (left pad)
        Bytes addrBytes = Bytes.wrap(addr.toArray()); // 20 bytes
        Bytes32 paddedKey = Bytes32.leftPad(addrBytes); // left-pad to 32 bytes
        // slot index as 32 bytes
        Bytes32 slotIndexBytes = uintToBytes32(BigInteger.valueOf(mappingSlotIndex));
        Bytes concatenated = Bytes.concatenate(paddedKey, slotIndexBytes);
        Bytes32 slotHash = Hash.keccak256(concatenated);
        return slotHash;
    }

    private static Bytes32 uintToBytes32(final BigInteger value) {
        byte[] asBytes = value.toByteArray();
        byte[] out = new byte[32];
        int srcPos = Math.max(0, asBytes.length - 32);
        int length = Math.min(32, asBytes.length);
        System.arraycopy(asBytes, srcPos, out, 32 - length, length);
        return Bytes32.wrap(out);
    }

    private static UInt256 bytes32ToUInt256(final Bytes32 b) {
        // UInt256.fromBytes or UInt256.valueOf depending on API:
        return UInt256.fromBytes(b);
    }

    // ---------- set single validator stake (no commit here) ----------
    private void setValidatorStake(final WorldUpdater updater,
                                   final Address contractAddress,
                                   final Address validatorAddress,
                                   final BigInteger stakeWei,
                                   final long mappingSlotIndex) {

        // get (or create) contract account in updater
        MutableAccount contract = updater.getOrCreate(contractAddress);

        // compute storage slot (Solidity mapping: keccak(pad32(key) ++ pad32(slotIndex)))
        Bytes32 slotHash = computeMappingSlotForAddress(validatorAddress, mappingSlotIndex);
        UInt256 storageKey = bytes32ToUInt256(slotHash);
        UInt256 storageValue = UInt256.valueOf(stakeWei);

        // write into the contract storage (overlay)
        contract.setStorageValue(storageKey, storageValue);

        LOG.debug("WROTE to updater slot {} => value {} (wei) for validator {}", slotHash, stakeWei, validatorAddress);

        // read back from the updater overlay to verify to write (immediate check)
        UInt256 readBack = contract.getStorageValue(storageKey);
        LOG.debug("READ-BACK from updater slot {} => {}", slotHash, readBack);
        // don't commit here
    }

    // ---------- read initial stakes ----------
    private void readInitialStake(final MutableWorldState worldState, final MutableBlockchain blockchain) {
        final WorldUpdater updater = worldState.updater();
        final BlockHeader genesisHeader = blockchain.getBlockHeader(0)
                .orElseThrow(() -> new RuntimeException("Genesis block not found"));

        LOG.debug("stake in config:");
        final Address stakeManager = posConfig.getContractAddress();

        // ensure the contract account exists in updater
        MutableAccount contract = updater.getOrCreate(stakeManager);
        if (contract == null) {
            throw new RuntimeException("Cannot create stake manager account in updater");
        }

        // IMPORTANT: set this to the mapping slot index used by your solidity contract
        final long mappingSlotIndex = 0L; // <-- change if the mapping isn't in slot 0

        posConfig.getInitialStake().forEach((addrHex, amountEth) -> {
            LOG.debug("{}: {}", addrHex, amountEth);

            Address validator = Address.fromHexString(addrHex);

            // Convert ETH to wei if amount is ETH in config
            BigInteger stakeWei = BigInteger.valueOf(amountEth.longValue()).multiply(BigInteger.TEN.pow(18));

            // Write but DON'T commit here
            setValidatorStake(updater, stakeManager, validator, stakeWei, mappingSlotIndex);
        });

        // Now commit once
        updater.commit();
        LOG.debug("Updater committed initial stake writes.");

        // Persist world state for genesis header (if required by your flow)
//        worldState.persist(genesisHeader);
        LOG.debug("World state persisted for genesis header.");
        // compute the world-state root after the writes
        final Bytes32 computedRoot = worldState.rootHash();
        final Bytes32 genesisRoot = genesisHeader.getStateRoot();

        if (!computedRoot.equals(genesisRoot)) {
              LOG.error("Genesis header stateRoot mismatch after applying initial stakes.");
              LOG.error("  genesis.header.stateRoot = {}", genesisRoot);
             LOG.error("  computed.worldState.root   = {}", computedRoot);

            }else
                LOG.info("Initial stakes applied and genesis stateRoot matches the on-disk genesis header.");

    }

    private void overwriteGenesisStateRoot(final MutableWorldState worldState,
                                           final ProtocolContext context, ProtocolSchedule protocolSchedule) {

        final org.hyperledger.besu.datatypes.Hash newStateRoot = worldState.rootHash();
        final Blockchain blockchain = context.getBlockchain();
        final BlockHeader oldGenesis = blockchain
                .getBlockHeader(0L)
                .orElseThrow(() -> new IllegalStateException("Genesis header missing"));

        // 1. Build a new header identical except for stateRoot
        final BlockHeader newGenesis = BlockHeaderBuilder.create()
                .populateFrom(oldGenesis)
                .stateRoot(newStateRoot)
                .nonce(oldGenesis.getNonce())
                .blockHeaderFunctions(protocolSchedule
                        .getByBlockHeader(oldGenesis)
                        .getBlockHeaderFunctions())
                .buildBlockHeader();
        worldState.persist(newGenesis);
        final KeyValueStorage headerStore = storageProvider
                .getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.BLOCKCHAIN);

        final KeyValueStorageTransaction tx = headerStore.startTransaction();
        tx.put(
                    newGenesis.getHash().toArrayUnsafe(),
                    RLP.encode(newGenesis::writeTo).toArray());
            tx.commit();


        LOG.info("Genesis stateRoot overwritten to {}", newStateRoot);
    }

    // ---------- get validator stake ----------
    private BigInteger getValidatorStake(final WorldState worldState,
                                         final Address contractAddress,
                                         final Address validatorAddress,
                                         final long mappingSlotIndex) {
        Account contractAccount = worldState.get(contractAddress);
        if (contractAccount == null || contractAccount.isEmpty()) {
            LOG.debug("contractAccount is null or empty");
            return BigInteger.ZERO;
        }

        Bytes32 slotHash = computeMappingSlotForAddress(validatorAddress, mappingSlotIndex);
        UInt256 storageKey = bytes32ToUInt256(slotHash);
//        LOG.debug("storageKey for {}: {}", validatorAddress.toHexString(),storageKey);
        UInt256 stored = contractAccount.getStorageValue(storageKey);
        if (stored == null) return BigInteger.ZERO;
        return stored.toBigInteger(); // value in wei
    }

    private static MutableWorldState getMutableWorldState(ProtocolContext protocolContext, MutableBlockchain blockchain) {
        WorldStateArchive worldStateArchive = protocolContext.getWorldStateArchive();
        BlockHeader genesisHeader =
                blockchain
                        .getBlockHeader(0)
                        .orElseThrow(() -> new RuntimeException("Genesis block not found"));
        WorldStateQueryParams params =
                WorldStateQueryParams.withBlockHeaderAndUpdateNodeHead(genesisHeader);
        return worldStateArchive.getWorldState(params)
        .orElseThrow(() -> new RuntimeException("cannot get mutable world state"));
    }

    private NodeSet createNodeSet(MutableWorldState worldState, MutableBlockchain blockchain) {
    BlockHeader genesisHeader =
        blockchain
            .getBlockHeader(0)
            .orElseThrow(() -> new RuntimeException("Genesis block not found"));
    NodeSet nodeSet = new NodeSet();

    // Use your custom codec to decode extraData
    PosExtraDataCodec codec = new PosExtraDataCodec();
    PosExtraData posExtraData = codec.decodePosData(genesisHeader.getExtraData());

    // Extract validator addresses
    List<Address> validators = posExtraData.getValidators().stream().toList();
    List<SECPPublicKey> publicKeys = posExtraData.getPublicKeys().stream().toList();
    List<Bls.PublicKey> blsPublicKeys = posExtraData.getBlsPublicKeys().stream().toList();
    List<Bls.Signature> blsPops = posExtraData.getPops().stream().toList();
    PosExtraDataCodec.setPublicKeys(publicKeys);
    PosExtraDataCodec.setBlsPublicKeys(blsPublicKeys);
    registerPops(blsPops, blsPublicKeys);
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
    for (int index = 0; index < validators.size(); index++) {
      validatorCount++;
      Address validator=validators.get(index);
      SECPPublicKey publicKey=publicKeys.get(index);
      Bls.PublicKey blsPublicKey=blsPublicKeys.get(index);
      String id = "Validator-" + validatorCount;
      // Get account balance from world state
      Account account = worldState.get(validator);
      BigInteger balanceWei =
          account != null ? account.getBalance().toBigInteger() : BigInteger.ZERO;

      BigDecimal balanceEth = weiToEth(balanceWei);
        final long mappingSlotIndex = 0L;
      // Get stake from contract
      BigInteger stakeWei = getValidatorStake(worldState, stakeManager, validator,mappingSlotIndex);
      BigDecimal stakeEth = weiToEth(stakeWei);

      System.out.printf(
          "%-20s | %-42s | %-15s | %-15s%n",
          id, validator.toHexString(), balanceEth.toString(), stakeEth.toString());

      // Build node info (customize as needed)
      StakeInfo stake = StakeInfo.builder().stakedAmount(stakeEth.longValue()).build();//todo
//      StakeInfo stake = StakeInfo.builder().stakedAmount(100L).build();//todo

      Node node =
          Node.builder()
              .id(id)
              .address(validator)
              .inCommittee(false)
              .stakeInfo(stake)
              .blocksProposed(0)
              .lastProposedAt(0)
              .publicKey(publicKey)
              .blsPublicKey(blsPublicKey)
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


    private static void registerPops(List<Bls.Signature> blsPops, List<Bls.PublicKey> blsPublicKeys) {
        PosExtraDataCodec.setPops(blsPops);
        for (int i = 0; i < blsPublicKeys.size() ; i++) {
            Bls.registerProofOfPossession(blsPublicKeys.get(i), blsPops.get(i));
        }
    }

    public void setDataDir(Path dataDir) {
        this.dataDir = dataDir;
    }

    private BigDecimal weiToEth(BigInteger wei) {
    return new BigDecimal(wei)
        .divide(new BigDecimal("1000000000000000000"), 6, RoundingMode.HALF_UP.ordinal());
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
            posExtraDataCodec,
        evmConfiguration,
        miningConfiguration,
        badBlockManager,
        isParallelTxProcessingEnabled,
            isBlockAccessListEnabled,
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
    PosProtocolScheduleBuilder.setBlockchain(blockchain);
    LOG.debug("blockchain is set");
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

  private Bls.KeyPair readBlsKeyPair() throws IOException {
      Path privateKeyFile = dataDir.resolve("BlsKey");
      Path publicKeyFile = dataDir.resolve("BlsKey.pub");

      Bls.SecretKey privateKey = Bls.SecretKey.secretKeyFromHex(Files.readAllLines(privateKeyFile).getFirst());
      Bls.PublicKey publicKey = Bls.PublicKey.publicKeyFromHex(Files.readAllLines(publicKeyFile).getFirst());
      return new Bls.KeyPair(privateKey,publicKey);
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
