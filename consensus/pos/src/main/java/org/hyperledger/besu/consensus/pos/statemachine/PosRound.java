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
package org.hyperledger.besu.consensus.pos.statemachine;

import lombok.Getter;
import lombok.Setter;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.config.PosConfigOptions;
import org.hyperledger.besu.consensus.common.bft.BftBlockHashing;
import org.hyperledger.besu.consensus.common.bft.BftExtraData;
import org.hyperledger.besu.consensus.common.bft.BftExtraDataCodec;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.RoundTimer;
import org.hyperledger.besu.consensus.common.bft.payload.SignedData;
import org.hyperledger.besu.consensus.pos.PosBlockCreator;
import org.hyperledger.besu.consensus.pos.PosBlockImporter;
import org.hyperledger.besu.consensus.pos.PosProtocolSchedule;
import org.hyperledger.besu.consensus.pos.core.*;
import org.hyperledger.besu.consensus.pos.messagewrappers.Propose;
import org.hyperledger.besu.consensus.pos.messagewrappers.SelectLeader;
import org.hyperledger.besu.consensus.pos.network.PosMessageTransmitter;
import org.hyperledger.besu.consensus.pos.payload.PosPayload;
import org.hyperledger.besu.consensus.pos.payload.ProposePayload;
import org.hyperledger.besu.consensus.pos.payload.SelectLeaderPayload;
import org.hyperledger.besu.consensus.pos.vrf.VRF;
import org.hyperledger.besu.crypto.SECPPublicKey;
import org.hyperledger.besu.crypto.SECPSignature;
import org.hyperledger.besu.cryptoservices.NodeKey;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.ProtocolContext;
import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.core.Block;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.core.Util;
import org.hyperledger.besu.ethereum.worldstate.WorldStateArchive;
import org.hyperledger.besu.evm.account.Account;
import org.hyperledger.besu.evm.worldstate.WorldState;
import org.hyperledger.besu.plugin.services.securitymodule.SecurityModuleException;
import org.hyperledger.besu.util.Subscribers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** The Pos round. */
@Setter
@Getter
public class PosRound {

  private static final Logger LOG = LoggerFactory.getLogger(PosRound.class);

  private final Subscribers<PosMinedBlockObserver> observers;
  private final RoundState roundState;
  private final PosBlockCreator blockCreator;
  private final PosConfigOptions posConfigOptions;

  /** The protocol context. */
  protected final ProtocolContext protocolContext;

  protected final NodeSet nodeSet;
  protected final ContractCaller contractCaller;

  private final PosProtocolSchedule protocolSchedule;
  private final NodeKey nodeKey;
  private final PosRoundFactory.MessageFactory messageFactory; // used only to create stored local msgs
  private final PosMessageTransmitter transmitter;
  private final BftExtraDataCodec bftExtraDataCodec;
  private final PosBlockHeader parentHeader;
  private Propose propose;
  private final PosProposerSelector posProposerSelector;
  private final PosFinalState posFinalState;
  private final Address localAddress;
  private boolean isIgnoreSelectLeaderMessages;

  private static String ALGORITHM = "ECDSA";

  private PosBlockHeightManager posBlockHeightManager;

  private final ScheduledExecutorService executor =
          Executors.newSingleThreadScheduledExecutor(
                  r -> {
                    Thread t = new Thread(r, "pos-round-executor");
                    t.setDaemon(true); // don’t block JVM shutdown
                    return t;
                  });


  /**
   * Instantiates a new Pos round.
   *
   * @param roundState the round state
   * @param blockCreator the block creator
   * @param protocolContext the protocol context
   * @param protocolSchedule the protocol schedule
   * @param observers the observers
   * @param nodeKey the node key
   * @param messageFactory the message factory
//   * @param transmitter the transmitter
   * @param roundTimer the round timer
   * @param bftExtraDataCodec the bft extra data codec
   * @param parentHeader the parent header
   */
  public PosRound(
          final RoundState roundState,
          final PosBlockCreator blockCreator,
          final ProtocolContext protocolContext,
          final PosProtocolSchedule protocolSchedule,
          final Subscribers<PosMinedBlockObserver> observers,
          final NodeKey nodeKey,
          final PosRoundFactory.MessageFactory messageFactory,
          final PosMessageTransmitter transmitter,
          final RoundTimer roundTimer,
          PosConfigOptions posConfigOptions,
          final BftExtraDataCodec bftExtraDataCodec,
          final PosBlockHeader parentHeader,
          final ContractCaller contractCaller, NodeSet nodeSet,
          PosProposerSelector posProposerSelector, PosFinalState posFinalState
  ) {
    this.roundState = roundState;
    this.blockCreator = blockCreator;
    this.protocolContext = protocolContext;
    this.protocolSchedule = protocolSchedule;
    this.observers = observers;
    this.nodeKey = nodeKey;
    this.messageFactory = messageFactory;
    this.transmitter = transmitter;
    this.posConfigOptions = posConfigOptions;
    this.bftExtraDataCodec = bftExtraDataCodec;
    this.parentHeader = parentHeader;
    this.contractCaller = contractCaller;
    this.nodeSet = nodeSet;
    this.localAddress=Util.publicKeyToAddress(nodeKey.getPublicKey());
      this.posProposerSelector = posProposerSelector;
      this.posFinalState = posFinalState;
//      roundTimer.startTimer(getRoundIdentifier());
  }

  /**
   * Gets round identifier.
   *
   * @return the round identifier
   */
  public ConsensusRoundIdentifier getRoundIdentifier() {
    return roundState.getRoundIdentifier();
  }

  /**
   * Create and send proposal message.
   *
   * @param headerTimeStampSeconds the header time stamp seconds
   */
//  public void createAndSendProposalMessage(final long headerTimeStampSeconds) {
//    final Block block =
//            blockCreator.createBlock(headerTimeStampSeconds, this.parentHeader,Util.publicKeyToAddress(nodeKey.getPublicKey())).getBesuBlock();
//    final BftExtraData extraData = bftExtraDataCodec.decode(block.getHeader());
//    importBlockToChain(block);
//    updateRound(block);
//    printAllStake();
//    LOG.debug("Creating proposed block. round={}", roundState.getRoundIdentifier());
//    LOG.trace(
//            "Creating proposed block with extraData={} blockHeader={}", extraData, block.getHeader());
////    updateStateWithProposalAndTransmit(block, Optional.empty());
//
//  }

  public PosBlock createBlock(final long headerTimeStampSeconds) {
    LOG.info("function PosBlock createBlock");
    final Block block =
            blockCreator.createBlock(headerTimeStampSeconds, this.parentHeader,Util.publicKeyToAddress(nodeKey.getPublicKey())).getBesuBlock();
    final BftExtraData extraData = bftExtraDataCodec.decode(block.getHeader());
    LOG.trace(
            "Creating proposed block with extraData={} blockHeader={}", extraData, block.getHeader());
      return new PosBlock(block,roundState.getRoundIdentifier(),localAddress);
  }

  private void printStake(Block block){
    WorldStateArchive worldStateArchive = protocolContext.getWorldStateArchive();
    Blockchain blockchain = protocolContext.getBlockchain();

    BlockHeader header =block.getHeader();

    WorldState worldState =
            worldStateArchive
                    .get(header.getStateRoot(), header.getHash())
                    .orElseThrow(() -> new RuntimeException("Genesis state not available"));
    Address nodeAddress= Util.publicKeyToAddress(nodeKey.getPublicKey());
    Account account = worldState.get(nodeAddress);
    BigInteger balanceWei =
            account != null ? account.getBalance().toBigInteger() : BigInteger.ZERO;

    BigDecimal balanceEth = weiToEth(balanceWei);
    Address stakeManager = Address.fromHexString("0x1234567890123456789012345678901234567890");

    // Get stake from contract
    BigInteger stakeWei = getValidatorStake(worldState, stakeManager, nodeAddress);
    BigDecimal stakeEth = weiToEth(stakeWei);
    System.out.printf(
            "%-20s | %-42s | %-15s | %-15s%n",
            "Validator ID", "Address", "Balance (ETH)", "Stake (ETH)");
    System.out.println(
            "---------------------------------------------------------------------------");
    System.out.printf(
            "%-20s | %-42s | %-15s | %-15s%n",
            0, nodeAddress.toHexString(), balanceEth.toString(), stakeEth.toString());
  }

  private void printAllStake(){

    System.out.printf(
            "%-20s | %-42s | %-15s%n",
            "Validator ID", "Address", "Stake (ETH)");
    System.out.println(
            "---------------------------------------------------------------------------");
    nodeSet.getAllNodes().forEach(node -> {
      long stakedWei = node.getStakeInfo().getStakedAmount();
      BigDecimal stakeEth = weiToEth(BigInteger.valueOf(stakedWei));
      System.out.printf(
              "%-20s | %-42s |  %-15s%n",
              node.getId(), node.getAddress().toHexString(), stakeEth.toString());
    });


  }

  private BigDecimal weiToEth(BigInteger wei) {
    return new BigDecimal(wei).divide(new BigDecimal("1000000000000000000"), 6, RoundingMode.HALF_UP.ordinal());
  }

  private BigInteger getValidatorStake(WorldState worldState, Address contractAddress, Address validatorAddress) {
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
    Bytes32 slotHash = org.hyperledger.besu.crypto.Hash.keccak256(concatenated);

    // 3. Read storage value at computed slot
    UInt256 stakeValue =
            contractAccount.getStorageValue(UInt256.valueOf(slotHash.toUnsignedBigInteger()));
    return stakeValue.toBigInteger();
  }


private SignedData<ProposePayload> createProposePayload(PosBlock block, VRF.Proof proof) {
  ProposePayload proposePayload=messageFactory.createProposePayload(block.getHeader().getRoundIdentifier(),block.getHeader().getHeight(),block,proof);
  return createSignedData(proposePayload);
}

  public <M extends PosPayload> SignedData<M> createSignedData(M payload){
    LOG.debug("createSignedData");
    LOG.debug("hashForSignature: {}",payload.hashForSignature());
    SECPSignature sign = nodeKey.sign(payload.hashForSignature());
    return SignedData.create(payload, sign);
  }


  protected void createProposalAndTransmit(Clock clock,VRF.Proof proof) {
//      long headerTimeStampSeconds = Math.round(clock.millis() / 1000D);
//      LOG.debug("headerTimeStampSeconds: {}, parentHeader time:{}", headerTimeStampSeconds,parentHeader.getTimestamp());
//      long diff= headerTimeStampSeconds- parentHeader.getTimestamp();
//      if(diff<posConfigOptions.getBlockPeriodSeconds()/5){
//        Thread.sleep(((posConfigOptions.getBlockPeriodSeconds()/5) -diff)*1000);
//        LOG.debug("(posConfigOptions.getBlockPeriodSeconds()/5):{}",(posConfigOptions.getBlockPeriodSeconds()/5));
//        LOG.debug("diff:{}",diff);
//        LOG.debug("posConfigOptions.getBlockPeriodSeconds()/5) -diff):{}",((posConfigOptions.getBlockPeriodSeconds()/5) -diff));
//        headerTimeStampSeconds = Math.round(clock.millis() / 1000D);
//        LOG.debug("headerTimeStampSeconds{}",headerTimeStampSeconds);
//      }
      long MIN_GAP_SECONDS=posConfigOptions.getBlockPeriodSeconds()/5;
      long delayMs = Math.max(0L, (parentHeader.getTimestamp() + MIN_GAP_SECONDS) * 1000L - clock.millis());
      LOG.debug("createProposalAndTransmit");
      executor.schedule(() -> {
        Propose proposal = null;
        try {
          long tsSec = Math.max(parentHeader.getTimestamp() + MIN_GAP_SECONDS, TimeUnit.MILLISECONDS.toSeconds(clock.millis()));
          PosBlock posBlock = createBlock(tsSec);
          var roundIdentifier = posBlock.getPosBlockHeader().getRoundIdentifier();
          if (!posBlock.isEmpty()) {
            var proposePayload = createProposePayload(posBlock, proof);

            LOG.debug("Creating proposal and transmit for block");
            proposal = messageFactory.createPropose(proposePayload);

          } else {
            // handle the block times period
            final long currentTimeInMillis = posFinalState.getClock().millis();
            boolean emptyBlockExpired = posFinalState
                    .getBlockTimer()
                    .checkEmptyBlockExpired(parentHeader::getTimestamp, currentTimeInMillis);
            if (emptyBlockExpired) {
              LOG.trace(
                      "Block has no transactions and this node is a proposer so it will send a proposal: " + roundIdentifier);
              var proposePayload = createProposePayload(posBlock, proof);
              LOG.debug("Creating proposal and transmit for block2");
              proposal = messageFactory.createPropose(proposePayload);
            } else {
              LOG.trace(
                      "Block has no transactions but emptyBlockPeriodSeconds did not expired yet: "
                              + roundIdentifier);
              posFinalState
                      .getBlockTimer()
                      .resetTimerForEmptyBlock(
                              roundIdentifier, parentHeader::getTimestamp, currentTimeInMillis);
//          posFinalState.getRoundTimer().cancelTimer();
//          currentRound = Optional.empty();
            }
          }
          if (proposal != null) {
            transmitter.multicastProposal(proposal);
            roundState.setProposeMessage(proposal);
          }

        } catch (final SecurityModuleException e) {
          LOG.warn("Failed to create a signed Proposal, waiting for next round.", e);
        }
      }, delayMs, TimeUnit.MILLISECONDS);
  }

  public boolean importBlockToChain() {
    if (posProposerSelector.getCurrentProposer().isEmpty()){
      LOG.warn("No proposer selected for importBlockToChain");
      return false;
    }
    final PosBlock blockToImport =
            blockCreator.createSealedBlock(
                    roundState.getProposedBlock(),
                    roundState.getRoundIdentifier().getRoundNumber(),
                    roundState.getCommitSeals(),
                    posProposerSelector.getCurrentProposer().get()
            );

    final long blockNumber = blockToImport.getHeader().getBesuBlockHeader().getNumber();
    if (getRoundIdentifier().getRoundNumber() > 0) {
      LOG.info(
              "Importing proposed block to chain. round={}, hash={}",
              getRoundIdentifier(),
              blockToImport.getHash());
    } else {
      LOG.debug(
              "Importing proposed block to chain. round={}, hash={}",
              getRoundIdentifier(),
              blockToImport.getHash());
    }

    final PosBlockImporter blockImporter =
            protocolSchedule.getBlockImporter(blockToImport.getHeader());
    boolean isSuccess =
            blockImporter.importBlock(blockToImport);

    if(isSuccess) {
      notifyNewBlockListeners(blockToImport);
      return true;
    }else {
      LOG.error(
              "Failed to import proposed block to chain. block={} blockHeader={}",
              blockNumber,
              blockToImport.getHeader());
      return false;
    }
  }

//  private SECPSignature createCommitSeal(final PosBlock block) {
//    final PosBlock commitBlock = createCommitBlock(block);
//    final Hash commitHash = commitBlock.getHash();
//    return nodeKey.sign(commitHash);
//  }

//  private PosBlock createCommitBlock(final PosBlock block) {
//    return blockInterface.replaceRoundInBlock(block, getRoundIdentifier().getRoundNumber());
//  }


//  private void importBlockToChain(Block block) {
//    final Block blockToImport =
//            BftHelpers.createSealedBlock(
//                    bftExtraDataCodec,
//                    block,
//                    roundState.getRoundIdentifier().getRoundNumber(),
//                    roundState.getCommitSeals());
//
//    final long blockNumber = blockToImport.getHeader().getNumber();
//    final BftExtraData extraData = bftExtraDataCodec.decode(blockToImport.getHeader());
//    if (getRoundIdentifier().getRoundNumber() > 0) {
//      LOG.info(
//              "Importing block to chain. round={}, hash={}",
//              getRoundIdentifier(),
//              blockToImport.getHash());
//    } else {
//      LOG.debug(
//              "Importing block to chain. round={}, hash={}",
//              getRoundIdentifier(),
//              blockToImport.getHash());
//    }
//    LOG.trace("Importing block with extraData={}", extraData);
//    final BlockImporter blockImporter =
//            protocolSchedule.getByBlockHeader(blockToImport.getHeader()).getBlockImporter();
//    final BlockImportResult result =
//            blockImporter.importBlock(protocolContext, blockToImport, HeaderValidationMode.FULL);
//    if (!result.isImported()) {
//      LOG.error(
//              "Failed to import block to chain. block={} extraData={} blockHeader={}",
//              blockNumber,
//              extraData,
//              blockToImport.getHeader());
//    } else {
//      notifyNewBlockListeners(blockToImport);
//    }
//  }

  private SECPSignature createCommitSeal(final Block block) {
    final BlockHeader proposedHeader = block.getHeader();
    final BftExtraData extraData = bftExtraDataCodec.decode(proposedHeader);
    final Hash commitHash =
            new BftBlockHashing(bftExtraDataCodec)
                    .calculateDataHashForCommittedSeal(proposedHeader, extraData);
    return nodeKey.sign(commitHash);
  }

  private void notifyNewBlockListeners(final PosBlock block) {
    observers.forEach(obs -> obs.blockMined(block));
  }

  private void updateNodes(Block currentBlock){
    nodeSet.getAllNodes().forEach(node -> {
      BigInteger newStake= contractCaller.getValidatorStake(node.getAddress(),currentBlock);
      StakeInfo stakeInfo = new StakeInfo(newStake.longValue());
//      node.setStakeInfo(stakeInfo); //TODO uncomment

    });
  }

  public void sendSelectLeader(VRF.Proof proof,boolean isCandidate) {
    LOG.debug("Sending selectleader message. round={}", getRoundState().getRoundIdentifier());
    try {
      SelectLeaderPayload unsigned= messageFactory.createSelectLeaderPayload(getRoundState().getRoundIdentifier()
              ,getRoundState().getHeight(), proof ,isCandidate,nodeKey.getPublicKey());
      SignedData<SelectLeaderPayload> signed=createSignedData(unsigned);
      final SelectLeader selectLeader = messageFactory.createSelectLeader(signed);
      getRoundState().addSelectLeaderMessage(selectLeader);
      transmitter.multicastSelectLeader(selectLeader);
      if(checkThresholdWithoutSelf(roundState.getSelectLeaderMessages())){
        posBlockHeightManager.handleSelectLeaderMessage(selectLeader,false);
        LOG.debug("posBlockHeightManager{}", posBlockHeightManager.isFirstRoundStarted());
      }
    } catch (final SecurityModuleException e) {
      LOG.warn("Failed to create a signed selectleader; {}", e.getMessage());
    }
  }

  public void updateRound(Block block, Clock clock, int roundNumber){
    updateNodes(block);
    var maybeLeaderVRF= posProposerSelector.calculateVrf(roundNumber, Bytes32.wrap(block.getHash().toArray()));
    if(maybeLeaderVRF.isPresent()) {
      var seed = PosProposerSelector.seed(roundNumber, block.getHash());
      boolean isCandidate = posProposerSelector.canLeader(maybeLeaderVRF.get().proof(), seed, localAddress,nodeKey.getPublicKey());
      sendSelectLeader(maybeLeaderVRF.get().proof(), isCandidate);
    }
  }

  public boolean checkThreshold(Set<?> msg, boolean isVote) {
    if (isVote){
      return msg.size() >= getRoundState().getQuorum();

    }else{
      return msg.size()-1 >= getRoundState().getQuorum();

    }
  }

  private boolean checkThresholdWithoutSelf(Set<?> msg){
    return checkThreshold(msg,true);
  }

  private boolean nodeIsleader(Address leader){
    return leader.equals(Util.publicKeyToAddress(nodeKey.getPublicKey()));
  }

}
