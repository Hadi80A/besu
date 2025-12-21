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
package org.hyperledger.besu.consensus.nexus.statemachine;

import lombok.Getter;
import lombok.Setter;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.config.NexusConfigOptions;
import org.hyperledger.besu.consensus.common.bft.BftBlockHashing;
import org.hyperledger.besu.consensus.common.bft.BftExtraData;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.RoundTimer;
import org.hyperledger.besu.consensus.common.bft.payload.SignedData;
import org.hyperledger.besu.consensus.nexus.*;
import org.hyperledger.besu.consensus.nexus.core.*;
import org.hyperledger.besu.consensus.nexus.messagewrappers.Propose;
import org.hyperledger.besu.consensus.nexus.messagewrappers.SelectLeader;
import org.hyperledger.besu.consensus.nexus.metrics.NexusMetricCalculator;
import org.hyperledger.besu.consensus.nexus.network.NexusMessageTransmitter;
import org.hyperledger.besu.consensus.nexus.payload.NexusPayload;
import org.hyperledger.besu.consensus.nexus.payload.ProposePayload;
import org.hyperledger.besu.consensus.nexus.payload.SelectLeaderPayload;
import org.hyperledger.besu.consensus.nexus.vrf.VRF;
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
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** The Nexus round. */
@Setter
@Getter
public class NexusRound {

  private static final Logger LOG = LoggerFactory.getLogger(NexusRound.class);

  private final Subscribers<NexusMinedBlockObserver> observers;
  private final RoundState roundState;
  private final NexusBlockCreator blockCreator;
  private final NexusConfigOptions nexusConfigOptions;

  /** The protocol context. */
  protected final ProtocolContext protocolContext;

  protected final NodeSet nodeSet;
  protected final ContractCaller contractCaller;

  private final NexusProtocolSchedule protocolSchedule;
  private final NodeKey nodeKey;
  private final NexusRoundFactory.MessageFactory messageFactory; // used only to create stored local msgs
  private final NexusMessageTransmitter transmitter;
  private final NexusExtraDataCodec nexusExtraDataCodec;
  private final NexusBlockHeader parentHeader;
  private Propose propose;
  private final NexusProposerSelector nexusProposerSelector;
  private final NexusFinalState nexusFinalState;
  private final Address localAddress;
  private boolean isIgnoreSelectLeaderMessages;
  private boolean validCommit=false;

  private static String ALGORITHM = "ECDSA";

  private NexusBlockHeightManager nexusBlockHeightManager;

  private final ScheduledExecutorService executor =
          Executors.newSingleThreadScheduledExecutor(
                  r -> {
                    Thread t = new Thread(r, "nexus-round-executor");
                    t.setDaemon(true); // don’t block JVM shutdown
                    return t;
                  });


  /**
   * Instantiates a new Nexus round.
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
   * @param nexusExtraDataCodec the bft extra data codec
   * @param parentHeader the parent header
   */
  public NexusRound(
          final RoundState roundState,
          final NexusBlockCreator blockCreator,
          final ProtocolContext protocolContext,
          final NexusProtocolSchedule protocolSchedule,
          final Subscribers<NexusMinedBlockObserver> observers,
          final NodeKey nodeKey,
          final NexusRoundFactory.MessageFactory messageFactory,
          final NexusMessageTransmitter transmitter,
          final RoundTimer roundTimer,
          NexusConfigOptions nexusConfigOptions,
          final NexusExtraDataCodec nexusExtraDataCodec,
          final NexusBlockHeader parentHeader,
          final ContractCaller contractCaller, NodeSet nodeSet,
          NexusProposerSelector nexusProposerSelector, NexusFinalState nexusFinalState
  ) {
    this.roundState = roundState;
    this.blockCreator = blockCreator;
    this.protocolContext = protocolContext;
    this.protocolSchedule = protocolSchedule;
    this.observers = observers;
    this.nodeKey = nodeKey;
    this.messageFactory = messageFactory;
    this.transmitter = transmitter;
    this.nexusConfigOptions = nexusConfigOptions;
    this.nexusExtraDataCodec = nexusExtraDataCodec;
    this.parentHeader = parentHeader;
    this.contractCaller = contractCaller;
    this.nodeSet = nodeSet;
    this.localAddress=Util.publicKeyToAddress(nodeKey.getPublicKey());
      this.nexusProposerSelector = nexusProposerSelector;
      this.nexusFinalState = nexusFinalState;
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

  public NexusBlock createBlock(final long headerTimeStampSeconds) {
    LOG.info("function NexusBlock createBlock");
    final Block block =
            blockCreator.createBlock(headerTimeStampSeconds, this.parentHeader,Util.publicKeyToAddress(nodeKey.getPublicKey())).getBesuBlock();
    LOG.debug("created block ");

      return new NexusBlock(block,roundState.getRoundIdentifier(),localAddress);
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


private SignedData<ProposePayload> createProposePayload(NexusBlock block, VRF.Proof proof) {
  ProposePayload proposePayload=messageFactory.createProposePayload(block.getHeader().getRoundIdentifier(),block.getHeader().getHeight(),block,proof);
  return createSignedData(proposePayload);
}

  public <M extends NexusPayload> SignedData<M> createSignedData(M payload){
    LOG.debug("createSignedData");
    LOG.debug("hashForSignature: {}",payload.hashForSignature());
    SECPSignature sign = nodeKey.sign(payload.hashForSignature());
    return SignedData.create(payload, sign);
  }


  protected void createProposalAndTransmit(Clock clock,VRF.Proof proof) {
      long MIN_GAP_SECONDS= nexusConfigOptions.getBlockPeriodSeconds()/5;
      long delayMs = Math.max(0L, (parentHeader.getTimestamp() + MIN_GAP_SECONDS) * 1000L - clock.millis());
      LOG.debug("createProposalAndTransmit");
      executor.schedule(() -> {
        Propose proposal = null;
        try {
          long tsSec = Math.max(parentHeader.getTimestamp() + MIN_GAP_SECONDS, TimeUnit.MILLISECONDS.toSeconds(clock.millis()));
          NexusBlock nexusBlock = createBlock(tsSec);
          var roundIdentifier = nexusBlock.getNexusBlockHeader().getRoundIdentifier();
          if (!nexusBlock.isEmpty()) {
            var proposePayload = createProposePayload(nexusBlock, proof);

            LOG.debug("Creating proposal and transmit for block");
            proposal = messageFactory.createPropose(proposePayload);

          } else {
            // handle the block times period
            final long currentTimeInMillis = nexusFinalState.getClock().millis();
            boolean emptyBlockExpired = nexusFinalState
                    .getBlockTimer()
                    .checkEmptyBlockExpired(parentHeader::getTimestamp, currentTimeInMillis);
            if (emptyBlockExpired) {
              LOG.debug(
                      "Block has no transactions and this node is a proposer so it will send a proposal: " + roundIdentifier);
              var proposePayload = createProposePayload(nexusBlock, proof);
              LOG.debug("Creating proposal and transmit for block2");
              proposal = messageFactory.createPropose(proposePayload);
            } else {
              LOG.trace(
                      "Block has no transactions but emptyBlockPeriodSeconds did not expired yet: "
                              + roundIdentifier);
              nexusFinalState
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
              NexusMetricCalculator nexusMetricCalculator = nexusFinalState.getNexusMetricCalculator();
              nexusMetricCalculator.recordProposalArrival(nexusBlock.getBesuBlock());
          }

        } catch (final SecurityModuleException e) {
          LOG.warn("Failed to create a signed Proposal, waiting for next round.", e);
        }
      }, delayMs, TimeUnit.MILLISECONDS);
  }

  public boolean importBlockToChain(QuorumCertificate quorumCertificate, Bytes32 seed) {
    if (nexusProposerSelector.getCurrentProposer().isEmpty()){
      LOG.warn("No proposer selected for importBlockToChain");
      return false;
    }
    final NexusBlock blockToImport =
            blockCreator.createSealedBlock(
                    roundState.getProposedBlock(),
                    roundState.getRoundIdentifier().getRoundNumber(),
                    roundState.getCommitSeals(),
                    nexusProposerSelector.getCurrentProposer().get(),
                    quorumCertificate,
                    seed
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

    final NexusBlockImporter blockImporter =
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

//  private SECPSignature createCommitSeal(final NexusBlock block) {
//    final NexusBlock commitBlock = createCommitBlock(block);
//    final Hash commitHash = commitBlock.getHash();
//    return nodeKey.sign(commitHash);
//  }

//  private NexusBlock createCommitBlock(final NexusBlock block) {
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
    final BftExtraData extraData = nexusExtraDataCodec.decodeNexus(proposedHeader);
    final Hash commitHash =
            new BftBlockHashing(nexusExtraDataCodec).calculateDataHashForCommittedSeal(proposedHeader, extraData);
    return nodeKey.sign(commitHash);
  }

  private void notifyNewBlockListeners(final NexusBlock block) {
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
        nexusBlockHeightManager.handleSelectLeaderMessage(selectLeader,false);
        LOG.debug("posBlockHeightManager{}", nexusBlockHeightManager.isFirstRoundStarted());
      }
    } catch (final SecurityModuleException e) {
      LOG.warn("Failed to create a signed selectleader; {}", e.getMessage());
    }
  }

  public void updateRound(Block block, Clock clock, int roundNumber){
    updateNodes(block);
    LOG.debug("roundNumber-1{}, Bytes32.wrap(block.getHash().toArray()){},\n" +
            "(block.getHeader().getNumber())-1{},posProposerSelector.getSeedAtRound(roundNumber-1){}",
            roundNumber, Bytes32.wrap(block.getHash().toArray()),
            block.getHeader().getNumber()+1,
            nexusProposerSelector.getSeedAtRound(roundNumber-1,block.getHash(),block.getHeader().getNumber()));
    var maybeLeaderVRF= nexusProposerSelector.calculateVrf(roundNumber, Bytes32.wrap(block.getHash().toArray()),
            block.getHeader().getNumber()+1, nexusProposerSelector.getSeedAtRound(roundNumber-1,block.getHash(),block.getHeader().getNumber()) );
    if(maybeLeaderVRF.isPresent()) {
      var seed = nexusProposerSelector.getSeedAtRound(roundNumber, block.getHash(), block.getHeader().getNumber()+1);
      boolean isCandidate = nexusProposerSelector.canLeader(maybeLeaderVRF.get().proof(), seed, localAddress,nodeKey.getPublicKey());
      sendSelectLeader(maybeLeaderVRF.get().proof(), isCandidate);
    }
  }

  public boolean checkThreshold(Set<?> msg, boolean isVote) {
    if (isVote){
      return msg.size()+1 >= getRoundState().getQuorum();

    }else{
      return msg.size() >= getRoundState().getQuorum();

    }
  }

  private boolean checkThresholdWithoutSelf(Set<?> msg){
    return checkThreshold(msg,true);
  }

  private boolean nodeIsleader(Address leader){
    return leader.equals(Util.publicKeyToAddress(nodeKey.getPublicKey()));
  }

}
