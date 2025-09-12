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

//import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.config.PosConfigOptions;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.events.RoundExpiry;
import org.hyperledger.besu.consensus.common.bft.messagewrappers.BftMessage;
import org.hyperledger.besu.consensus.common.bft.payload.SignedData;
import org.hyperledger.besu.consensus.pos.PosExtraData;
import org.hyperledger.besu.consensus.pos.PosExtraDataCodec;
import org.hyperledger.besu.consensus.pos.core.*;
import org.hyperledger.besu.consensus.pos.messagewrappers.*;
import org.hyperledger.besu.consensus.pos.network.PosMessageTransmitter;
import org.hyperledger.besu.consensus.pos.payload.*;

import java.time.Clock;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.collect.Maps;
import org.hyperledger.besu.consensus.pos.vrf.VRF;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.eth.sync.state.SyncState;
import org.hyperledger.besu.plugin.services.securitymodule.SecurityModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for starting/clearing Consensus rounds at a given block height. One of these is
 * created when a new block is imported to the chain. It immediately then creates a Round-0 object,
 * and sends a Propose message. If the round times out prior to importing a block, this class is
 * responsible for creating a RoundChange message and transmitting it.
 */
public class PosBlockHeightManager implements BasePosBlockHeightManager {

    private static final Logger LOG = LoggerFactory.getLogger(PosBlockHeightManager.class);

    private final PosRoundFactory roundFactory;
    private final PosBlockHeader parentHeader;
    private final PosRoundFactory.MessageFactory messageFactory;
    private final Map<Integer, RoundState> futureRoundStateBuffer = Maps.newHashMap();
    private final Clock clock;
    private final PosFinalState finalState;
    private final PosProposerSelector proposerSelector;
    private final PosMessageTransmitter transmitter;
    private final PosConfigOptions posConfig;
    private final Function<Map<String, Long>, RoundState> roundStateCreator;
    private final Blockchain blockchain;
    private Optional<PosRound> currentRound = Optional.empty();
    private boolean isEarlyRoundChangeEnabled = false;
    private final SyncState syncState;
    private final RoundChangeManager roundChangeManager;
    @Getter
    private boolean isFirstRoundStarted = false;
    private VRF.Proof leaderProof;
    // Store only 1 round change per round per validator
//    @VisibleForTesting
//    final Map<Address, ViewChange> receivedMessages = Maps.newLinkedHashMap();

    private final PeerPublicKeyFetcher peerPublicKeyFetcher;
    private int retryCounter =10;
    /**
     * Instantiates a new Pos block height manager.
     *
     * @param parentHeader    the parent header
     * @param finalState      the final state
     * @param posRoundFactory the pos round factory
     * @param clock           the clock
     * @param messageFactory  the message factory
     */
    public PosBlockHeightManager(
            final PosBlockHeader parentHeader,
            final PosFinalState finalState,
            final PosRoundFactory posRoundFactory,
            final Clock clock,
            final PosRoundFactory.MessageFactory messageFactory, PosProposerSelector proposerSelector, PosMessageTransmitter transmitter, PosConfigOptions posConfig, Blockchain blockchain, SyncState syncState, RoundChangeManager roundChangeManager, PeerPublicKeyFetcher peerPublicKeyFetcher
    ) {
        this.parentHeader = parentHeader;
        this.roundFactory = posRoundFactory;
        this.messageFactory = messageFactory;
        this.clock = clock;
        this.finalState = finalState;
        this.proposerSelector = proposerSelector;
        this.transmitter = transmitter;

        roundStateCreator = (infoMap) ->
                new RoundState(
                        new ConsensusRoundIdentifier(getChainHeight(), Math.toIntExact(infoMap.get("round"))),
                        finalState.getQuorum(),
                        infoMap.get("height"));
        this.posConfig = posConfig;
        this.blockchain = blockchain;
        this.syncState = syncState;
        this.roundChangeManager = roundChangeManager;
        this.peerPublicKeyFetcher = peerPublicKeyFetcher;

        final long nextBlockHeight = getChainHeight();

        PosExtraData posExtraData = readPosData();


        final ConsensusRoundIdentifier roundIdentifier;
        if(nextBlockHeight>1) {
            roundIdentifier = new ConsensusRoundIdentifier(nextBlockHeight, posExtraData.getRound() + 1);
        }else
            roundIdentifier = new ConsensusRoundIdentifier(nextBlockHeight, posExtraData.getRound());
        if(roundIdentifier.getRoundNumber()==0) {
            finalState.getBlockTimer().startTimer(roundIdentifier, parentHeader.getBesuHeader()::getTimestamp);
            setCurrentRound(roundIdentifier.getRoundNumber());
        }else
            startNewRound(roundIdentifier);
    }


    @Override
    public void handleBlockTimerExpiry(final ConsensusRoundIdentifier roundIdentifier) {
//    if (currentRound.isPresent()) {
//      // It is possible for the block timer to take longer than it should due to the precision of
//      // the timer in Java and the OS. This means occasionally the proposal can arrive before the
//      // block timer expiry and hence the round has already been set. There is no negative impact
//      // on the protocol in this case.
//      return;
//    }
//
//    long headerTimeStampSeconds = Math.round(clock.millis() / 1000D);
//    Block lastBlock = blockchain.getChainHeadBlock();
//    currentRound.get().updateRound(lastBlock,headerTimeStampSeconds);
//
//    startNewRound(roundIdentifier.getRoundNumber());
//
//    logValidatorChanges(posRound);
        finalState.getBlockTimer().cancelTimer();

        final long now = clock.millis() / 1000;
        if(retryCounter<10){
            finalState.getBlockTimer().startTimer(roundIdentifier, () -> now);
            retryCounter++;
            return;
        }
//        var blockPeriodSeconds=posConfig.getBlockPeriodSeconds();
//        final long nextBlockPeriodExpiryTime = now + (blockPeriodSeconds );
        var round =roundIdentifier;
        LOG.debug("before round {} ,roundIdentifier{}", round,roundIdentifier);
//        LOG.debug("round ");
        LOG.debug("syncState.isInSync() {}", syncState.isInSync());
        LOG.debug("peerPublicKeyFetcher.getEthPeers().peerCount() {}", peerPublicKeyFetcher.ethPeers().peerCount());
        if(currentRound.isEmpty() && !isFirstRoundStarted && roundIdentifier.getRoundNumber() ==0){
            setCurrentRound(0);
        }
        if (syncState.isInSync() && peerPublicKeyFetcher.ethPeers().peerCount() > 0) {
            round = new ConsensusRoundIdentifier(roundIdentifier.getSequenceNumber(), roundIdentifier.getRoundNumber()+1);
            LOG.debug("after round {} ,roundIdentifier{}", round,roundIdentifier);

           if (roundIdentifier.getRoundNumber() ==0 && !isFirstRoundStarted){
               LOG.debug("first round");
               isFirstRoundStarted = true;

               startNewRound(roundIdentifier);
           }else {
               doRoundChange(round.getRoundNumber());
               finalState.getBlockTimer().startTimer(round, () -> now);
           }
        }
        else{
            finalState.getBlockTimer().startTimer(round, () -> now);

        }
//    if (roundIdentifier.equals(posRound.getRoundIdentifier())) {
//        refreshRound(roundIdentifier.getRoundNumber());
//
//    }else {
//      LOG.trace(
//              "Block timer expired for a round ({}) other than current ({})",
//              roundIdentifier,
//              posRound.getRoundIdentifier());
//    }
    }

    //    private void logValidatorChanges(final PosRound posRound) {
//    if (posRound.getRoundIdentifier().getRoundNumber() == 0) {
//      final Collection<Address> previousValidators =
//              validatorProvider.getValidatorsForBlock(parentHeader);
//      final Collection<Address> validatorsForHeight =
//              validatorProvider.getValidatorsAfterBlock(parentHeader);
//      if (!(validatorsForHeight.containsAll(previousValidators))
//              || !(previousValidators.containsAll(validatorsForHeight))) {
//        LOG.info(
//                "POS Validator list change. Previous chain height {}: {}. Current chain height {}: {}.",
//                parentHeader.getNumber(),
//                previousValidators,
//                parentHeader.getNumber() + 1,
//                validatorsForHeight);
//      }
//    }
//    }
    public void roundExpired(final RoundExpiry expire) {
//    if (currentRound.isEmpty()) {
//      LOG.error(
//              "Received Round timer expiry before round is created timerRound={}", expire.getView());
//      return;
//    }
//
//    PosRound posRound = currentRound.get();
//    if (!expire.getView().equals(posRound.getRoundIdentifier())) {
//      LOG.trace(
//              "Ignoring Round timer expired which does not match current round. round={}, timerRound={}",
//              posRound.getRoundIdentifier(),
//              expire.getView());
//      return;
//    }
//
//        LOG.debug("round expired: $$");
//    doRoundChange(posRound.getRoundIdentifier().getRoundNumber() + 1);
    }

//    public void startRoundWith( final long headerTimestamp) {

    /// /    final Optional<Block> bestBlockFromRoundChange = roundChangeArtifacts.getBlock();
    /// /
    /// /    final RoundChangeCertificate roundChangeCertificate =
    /// /            roundChangeArtifacts.getRoundChangeCertificate();
//        final Block blockToPublish;
//        if (!bestBlockFromRoundChange.isPresent()) {
//            LOG.debug("Sending proposal with new block. round={}", roundState.getRoundIdentifier());
//            blockToPublish = blockCreator.createBlock(headerTimestamp, this.parentHeader).getBlock();
//        } else {
//            LOG.debug(
//                    "Sending proposal from VotedCertificate. round={}", roundState.getRoundIdentifier());
//
//            final BftBlockInterface bftBlockInterface =
//                    protocolContext.getConsensusContext(BftContext.class).getBlockInterface();
//            blockToPublish =
//                    bftBlockInterface.replaceRoundInBlock(
//                            bestBlockFromRoundChange.get(),
//                            getRoundIdentifier().getRoundNumber(),
//                            BftBlockHeaderFunctions.forCommittedSeal(bftExtraDataCodec));
//        }
//
//        updateStateWithProposalAndTransmit(blockToPublish, Optional.of(roundChangeCertificate));
//    }
    public void handleViewChangePayload(final ViewChange message) {
        final ConsensusRoundIdentifier targetRound = message.getRoundIdentifier();

        LOG.debug(
                "Round change from {}: block {}, round {}",
                message.getAuthor(),
                message.getRoundIdentifier().getSequenceNumber(),
                message.getRoundIdentifier().getRoundNumber());


        final MessageAge messageAge =
                determineAgeOfPayload(message.getRoundIdentifier().getRoundNumber());
        if (messageAge == MessageAge.PRIOR_ROUND) {
            LOG.debug("Received RoundChange Payload for a prior round. targetRound={}", targetRound);
            return;
        }
        Optional<Collection<ViewChange>> result = roundChangeManager.appendRoundChangeMessage(message);
//    finalState.getReceivedMessages().put(message.getAuthor(),message);

//        LOG.debug("finalState.getReceivedMessages().size() {}", finalState.getReceivedMessages().size());
//        LOG.debug("finalState.getQuorum() {}",finalState.getQuorum());

        if (!isEarlyRoundChangeEnabled) {
            if (result.isPresent()) {
                LOG.debug(
                        "Received sufficient RoundChange messages to change round to targetRound={}", targetRound);
                if (messageAge == MessageAge.FUTURE_ROUND) {
                    startNewRound(targetRound);

                }else
                    startNewRound(targetRound);
                LOG.debug("startNewRound with round{} ", targetRound.getRoundNumber());
            }
        }
    }

    private void startNewRound(ConsensusRoundIdentifier targetRound) {
        finalState.getBlockTimer().cancelTimer();
        long headerTimeStampSeconds = Math.round(clock.millis() / 1000D);
        finalState.getBlockTimer().startTimer(targetRound,()->headerTimeStampSeconds);
        proposerSelector.setCurrentLeader(Optional.empty());
        setCurrentRound(targetRound.getRoundNumber());
        callUpdateRound(targetRound.getRoundNumber());
    }

    private PosExtraData readPosData() {
        var extraData = blockchain.getChainHeadBlock().getHeader().getExtraData();
        return new PosExtraDataCodec().decodePosData(extraData);
    }


    private synchronized void doRoundChange(final int newRoundNumber) {

        LOG.debug("currentRound.isPresent() {}", currentRound.isPresent());
        if (currentRound.isPresent() && currentRound.get().getRoundIdentifier().getRoundNumber() >= newRoundNumber) {
            LOG.debug("currentRound.get().getRoundIdentifier().getRoundNumber(){}, newRoundNumber {} ",
                    currentRound.get().getRoundIdentifier().getRoundNumber(), newRoundNumber);
            return;
        }
        LOG.debug(
                "Round has expired or changing based on RC quorum,current round={} ,new round{}",
                currentRound.get().getRoundIdentifier(),newRoundNumber);

//    startNewRound(newRoundNumber);
//    refreshRound(newRoundNumber);
//    if (currentRound.isEmpty()) {
//      LOG.info("Failed to start round ");
//      return;
//    }
        PosRound posRoundNew = currentRound.get();
        var newRoundIdentifier = new ConsensusRoundIdentifier(currentRound.get().getRoundIdentifier().getSequenceNumber(), newRoundNumber);
        try {
            ViewChangePayload unsigned = messageFactory.createViewChangePayload(newRoundIdentifier, posRoundNew.getRoundState().getHeight());
            SignedData<ViewChangePayload> signedData = currentRound.get().createSignedData(unsigned);
            final ViewChange localViewChangeMessage = messageFactory.createViewChange(signedData);

//      handleViewChangePayload(localViewChangeMessage);

            transmitter.multicastRoundChange(localViewChangeMessage);
        } catch (final SecurityModuleException e) {
            LOG.warn("Failed to create signed RoundChange message.", e);
        }
    }

    private RoundState getRoundState() {
        return currentRound.map(PosRound::getRoundState).orElse(null);
    }
    public void consumeSelectLeaderMessage(final SelectLeader msg){
        actionOrBufferMessage(
                msg,
                currentRound.isPresent() ? this::handleSelectLeaderMessage : (ignore) -> {},
                RoundState::addSelectLeaderMessage);
    }

    public void consumeProposeMessage(final Propose msg){
        actionOrBufferMessage(
                msg,
                currentRound.isPresent() ? this::handleProposalMessage : (ignore) -> {},
                RoundState::addProposalMessage);
    }
    public void consumeVoteMessage(final Vote msg){
        actionOrBufferMessage(
                msg,
                currentRound.isPresent() ? this::handleVoteMessage: (ignore) -> {},
                RoundState::addVoteMessage);
    }
    public void consumeCommitMessage(final Commit msg){
        actionOrBufferMessage(
                msg,
                currentRound.isPresent() ? this::handleCommitMessage : (ignore) -> {},
                RoundState::addCommitMessage);
    }

    public void consumeViewChangeMessage(final ViewChange msg){
//        actionOrBufferMessage(
//                msg,
//                currentRound.isPresent() ? this::handleViewChangePayload : (ignore) -> {},
//                RoundState::addViewChangeMessage);
    }

    public void handleSelectLeaderMessage(final SelectLeader msg){
        handleSelectLeaderMessage(msg,true);
    }

    public void handleSelectLeaderMessage(final SelectLeader msg,boolean logReceived) {
        if (logReceived) {
            LOG.debug("Received a select leader message. round={}. author={}", msg.getRoundIdentifier(), msg.getAuthor());
        }
//        SelectLeaderPayload payload =msg.getSignedPayload().getPayload();
        if(determineAgeOfPayload(msg.getRoundIdentifier().getRoundNumber())==MessageAge.CURRENT_ROUND) {
            getRoundState().addSelectLeaderMessage(msg);
            LOG.debug("checkThreshold(getRoundState().getSelectLeaderMessages()) {}",checkThreshold(getRoundState().getSelectLeaderMessages(),false ));
            LOG.debug("proposerSelector.getCurrentProposer().isEmpty() {}",proposerSelector.getCurrentProposer().isEmpty());

            if (checkThreshold(getRoundState().getSelectLeaderMessages(),false) && proposerSelector.getCurrentProposer().isEmpty()) {

                LOG.debug("getRoundState().getSelectLeaderMessages() {}",Arrays.toString(getRoundState().getSelectLeaderMessages().toArray()));
                List<SelectLeader> candidates = filterLeaders(getRoundState().getSelectLeaderMessages(),
                        msg.getRoundIdentifier(), blockchain.getChainHeadHash());
                var seed = PosProposerSelector.seed(msg.getRoundIdentifier().getRoundNumber(), blockchain.getChainHeadHash());
                SelectLeader selected = null;
                if (candidates.isEmpty()) {
                    if (currentRound.isPresent()) {
                        LOG.debug("currentRound.get().getNodeSet()totalSize(){}", currentRound.get().getNodeSet().totalSize());
                        LOG.debug("getRoundState().getSelectLeaderMessages().size(){}", getRoundState().getSelectLeaderMessages().size());
                        if (getRoundState().getSelectLeaderMessages().size() == currentRound.get().getNodeSet().totalSize()) {
                            LOG.debug("candidates.isEmpty()");
                            handleBlockTimerExpiry(msg.getRoundIdentifier());
                            return;
                        }
                        LOG.debug("wait for more selectleader messages");
                        return;
                    }
                } else if (candidates.size() == 1) {
                    selected = candidates.getFirst();
                    LOG.debug("candidates.size()==1 {}", selected.getAuthor());
                    leaderProof= selected.getSignedPayload().getPayload().getProof();
                    proposerSelector.setCurrentLeader(Optional.of(selected.getAuthor()));

                } else {
                    LOG.debug("candidates.size()= {}", candidates.size());
                    Bytes32 selectedY = Bytes32.ZERO;
                    if(currentRound.isPresent()) {
                        LOG.debug("yes");
                        for (SelectLeader candidate : candidates) {
                            var proof = candidate.getSignedPayload().getPayload().getProof();
                            Optional<Node> optionalNode = currentRound.get().getNodeSet().getNode(candidate.getAuthor());
                            if(optionalNode.isPresent()) {
                                Node node = optionalNode.get();
                                var publicKey = node.getPublicKey();
                                var y = VRF.hash(publicKey, seed, proof);
                                LOG.debug("y vrf {}, author{}", y,candidate.getAuthor());
                                if (Objects.isNull(selected) || y.compareTo(selectedY) > 0) {
                                    LOG.debug("previous selected:{}", selected);
                                    selectedY = y;
                                    selected = candidate;
                                }//todo:, if y equal -> instead of ID, we select earlier msg
                            }
                        }
                        if (Objects.nonNull(selected)) {
                            leaderProof= selected.getSignedPayload().getPayload().getProof();
                            proposerSelector.setCurrentLeader(Optional.of(selected.getAuthor()));

                        }
                    }
                }
                if(Objects.nonNull(selected)) {
                    LOG.debug("leader is {}", selected.getAuthor());
                }
                if (proposerSelector.isLocalProposer() && currentRound.isPresent() && Objects.nonNull(selected)) {
                    System.out.println("im leader");

                    currentRound.get().createProposalAndTransmit(clock, selected.getSignedPayload().getPayload().getProof()
                    );
                }
            }
            else {
                if( proposerSelector.getCurrentProposer().isEmpty()) {
                    LOG.debug("SELECT LEADER not enough");
                }else {
                    if (proposerSelector.isLocalProposer()) {
                        System.out.println("im leader");
                        currentRound.get().createProposalAndTransmit(clock, leaderProof);
                    }
                }

            }

        }else {
            LOG.debug("invalid round in message");
        }
    }

    public List<SelectLeader> filterLeaders(Set<SelectLeader> leaders, ConsensusRoundIdentifier roundNumber,
                                                   Bytes32 prevBlockHash) {
        if(currentRound.isPresent()) {

            final long totalStake = currentRound.get().nodeSet.getAllNodes().stream().mapToLong(n ->
                    n.getStakeInfo().getStakedAmount()).sum();
            List<SelectLeader> condidates = new ArrayList<>();

            if (totalStake <= 0) {
                LOG.debug("total stake {} is below 0", totalStake);
                return condidates;
            }
            var seed = PosProposerSelector.seed(roundNumber.getRoundNumber(), prevBlockHash);


            leaders.forEach(leaderMsg -> {
                var proof = leaderMsg.getSignedPayload().getPayload().getProof();
                LOG.debug("leaderMsg.getSignedPayload().getPayload().getProof()={}", proof);
                boolean isCandidate = leaderMsg.getSignedPayload().getPayload().isCandidate();
                if (isCandidate && currentRound.get().getNodeSet().getNode(leaderMsg.getAuthor()).isPresent()) {
                    Node node = currentRound.get().getNodeSet().getNode(leaderMsg.getAuthor()).get();
                    LOG.debug("node{}", node.getAddress());
                    var publicKey = node.getPublicKey();
                    LOG.debug("node publickey{}", publicKey);
                    if(currentRound.isPresent() && Objects.isNull(publicKey)){
                        currentRound.get().updatePublicKey(node.getAddress());
                        LOG.debug("node publickey after update{}", publicKey);

                    }
                    if (VRF.verify(publicKey, seed, proof)) {
                        if (proposerSelector.canLeader(proof,seed,node.getAddress())) {
                            condidates.add(leaderMsg);
                        }
                    }
                }
            });
            return condidates;
        }
        return List.of();
    }


    public void handleProposalMessage(final Propose msg) {
        LOG.debug("Received a proposal message. round={}. author={}", msg.getRoundIdentifier(), msg.getAuthor());
        ProposePayload payload = msg.getSignedPayload().getPayload();
        final PosBlock block = payload.getProposedBlock();

        LOG.debug("publickeys: {}", Arrays.toString(peerPublicKeyFetcher.getConnectedPeerNodeIdsHex().toArray()));


        if (validateProposal(msg.getSignedPayload())) {
            proposerSelector.setCurrentLeader(Optional.of(msg.getAuthor()));
            LOG.debug("Valid a proposal message.");
            getRoundState().setProposeMessage(msg);
            sendVote(block);
        } else {
            LOG.debug("Invalid a proposal message.");
        }
    }

    private boolean validateProposal(SignedData<ProposePayload> payload) {
        int roundNumber = payload.getPayload().getRoundIdentifier().getRoundNumber();
        LOG.debug(" payload.getPayload().getRoundIdentifier().getRoundNumber() {} ", roundNumber);
        LOG.debug(" getRoundState().getRoundIdentifier().getRoundNumber() {} ", getRoundState().getRoundIdentifier().getRoundNumber());


        long headerTimeStampSeconds = payload.getPayload().getProposedBlock().getHeader().getTimestamp();
        long diff= headerTimeStampSeconds- parentHeader.getTimestamp();
        long systemTime = (long) (System.currentTimeMillis()/1000D);
        LOG.debug("roundNumber == getRoundState().getRoundIdentifier().getRoundNumber(){}",roundNumber == getRoundState().getRoundIdentifier().getRoundNumber());
        LOG.debug("diff >= (posConfig.getBlockPeriodSeconds() / 5)  ={}",diff >= (posConfig.getBlockPeriodSeconds() / 5) );
        LOG.debug("diff{}",diff);
        LOG.debug("headerTimeStampSeconds{}",headerTimeStampSeconds);
        LOG.debug("parentHeader.getTimestamp(){}",parentHeader.getTimestamp());
        LOG.debug("payload.getPayload().getHeight() == getRoundState().getHeight(){}",payload.getPayload().getHeight() == getRoundState().getHeight());
        LOG.debug("headerTimeStampSeconds-systemTime<1{}",headerTimeStampSeconds-systemTime<1);

        if(proposerSelector.getCurrentProposer().isPresent()) {
            LOG.debug("proposerSelector.getCurrentProposer().get().equals(payload.getAuthor()){}",proposerSelector.getCurrentProposer().get().equals(payload.getAuthor()));

            return roundNumber == getRoundState().getRoundIdentifier().getRoundNumber() &&
                    payload.getPayload().getHeight() == getRoundState().getHeight() &&
                    diff >= (posConfig.getBlockPeriodSeconds() / 5) &&
                    proposerSelector.getCurrentProposer().get().equals(payload.getAuthor()) &&
                    headerTimeStampSeconds-systemTime<1
                    ;
        }else {
            LOG.debug("proposerSelector.getCurrentProposer().isPresent()=false");
        }
        return false;
    }


    private void sendVote(final PosBlock block) {
        LOG.debug("Sending vote message. round={}", getRoundState().getRoundIdentifier());
        try {
            if(currentRound.isPresent()) {
                VotePayload unsigned = messageFactory.createVotePayload(block);
                SignedData<VotePayload> signedData = currentRound.get().createSignedData(unsigned);
                final Vote localVoteMessage = messageFactory.createVote(signedData);
                getRoundState().addVoteMessage(localVoteMessage);
                if (proposerSelector.getCurrentProposer().isPresent()) {
                    Address proposer = proposerSelector.getCurrentProposer().get();
                    List<Address> denylist = new ArrayList<>(currentRound.get().getNodeSet().getAllNodes().stream().map(Node::getAddress)
                            .toList());
                    denylist.remove(proposer);
                    LOG.debug("proposer: {} , denylist:{}", proposer, Arrays.toString(denylist.toArray()));
                    transmitter.multicastVote(localVoteMessage, denylist);

                }
            }
        } catch (final SecurityModuleException e) {
            LOG.warn("Failed to create a signed Vote; {}", e.getMessage());
        }
    }

    public void handleVoteMessage(final Vote msg) {
        LOG.debug("Received a vote message. round={}. author={}", getRoundState().getRoundIdentifier(), msg.getAuthor());
        getRoundState().addVoteMessage(msg);

        if (validateBlockHash(msg.getSignedPayload().getPayload().getDigest()) &&
                validateHeightAndRound(msg.getSignedPayload().getPayload()) &&
                proposerSelector.isLocalProposer()

        ) {
            if(checkThreshold(getRoundState().getVoteMessages(),true)){
                sendCommit(getRoundState().getProposedBlock());
            }else {
                LOG.debug("votes not enough");
            }
        } else {
            LOG.debug("Invalid a vote message.");
        }
    }

    private boolean checkThreshold(Set<?> msg,boolean isVote) {
        return currentRound.map(posRound -> posRound.checkThreshold(msg, isVote)).orElse(false);
    }

    private boolean validateHeightAndRound(PosPayload posPayload) {
        return posPayload.getHeight() == getRoundState().getHeight() &&
                posPayload.getRoundIdentifier().getRoundNumber() == getRoundState().getRoundIdentifier().getRoundNumber();
    }

    private boolean validateBlockHash(Hash blockHash) {
        if (Objects.isNull(getRoundState().getProposeMessage())) {
            LOG.debug("Propose message is null");

            return false;
        }
        PosBlock block = getRoundState().getProposeMessage().getSignedPayload().getPayload().getProposedBlock();
        return blockHash.equals(block.getHash());
    }

    private void sendCommit(final PosBlock block) {
        ConsensusRoundIdentifier roundIdentifier = getRoundState().getRoundIdentifier();
        LOG.debug("Sending Commit message. round={}", roundIdentifier);
        try {
            if (currentRound.isPresent()) {

                CommitPayload unsigned = messageFactory.createCommitPayload(block);
                SignedData<CommitPayload> signedData = currentRound.get().createSignedData(unsigned);
                final Commit localCommitMessage = messageFactory.createCommit(signedData);
                getRoundState().addCommitMessage(localCommitMessage);
                transmitter.multicastCommit(localCommitMessage);

                boolean result= false;
                retryCounter =0;
                finalState.getBlockTimer().cancelTimer();
                final long now = clock.millis() / 1000;
                finalState.getBlockTimer().startTimer(roundIdentifier,()->now);
                while (retryCounter < 10 && !result) {
                    result = currentRound.get().importBlockToChain();
                    if (!result) {
                        if (retryCounter < 10) {
                            TimeUnit.MILLISECONDS.sleep(10);
                        }
                    }
                }
//                startNewRound(new ConsensusRoundIdentifier(
//                        roundIdentifier.getSequenceNumber() + 1,
//                        roundIdentifier.getRoundNumber() + 1
//                ));
            }
        } catch (final SecurityModuleException e) {
            LOG.warn("Failed to create a signed Commit; {}", e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void handleCommitMessage(final Commit msg) {
        ConsensusRoundIdentifier roundIdentifier = getRoundState().getRoundIdentifier();
        LOG.debug("Received a commit message. round={}. author={}", roundIdentifier, msg.getAuthor());
        if( validateHeightAndRound(msg.getSignedPayload().getPayload()) &&
                proposerSelector.getCurrentProposer().isPresent() &&
                msg.getAuthor().equals(proposerSelector.getCurrentProposer().get())
        ) {
            retryCounter =0;
            getRoundState().addCommitMessage(msg);
            final long now = clock.millis() / 1000;
            finalState.getBlockTimer().cancelTimer();
            finalState.getBlockTimer().startTimer(roundIdentifier, ()->now);

        }else {
            LOG.debug("Invalid a commit message.");
        }
//        startNewRound(new ConsensusRoundIdentifier(
//                roundIdentifier.getSequenceNumber()+1,
//                roundIdentifier.getRoundNumber()+1
//        ));
    }


    private <P extends PosPayload, M extends BftMessage<P>> void actionOrBufferMessage(
            final M posMessage,
            final Consumer<M> inRoundHandler,
            final BiConsumer<RoundState, M> buffer) {
        final MessageAge messageAge =
                determineAgeOfPayload(posMessage.getRoundIdentifier().getRoundNumber());
        if (messageAge == MessageAge.CURRENT_ROUND) {
            inRoundHandler.accept(posMessage);
        } else if (messageAge == MessageAge.FUTURE_ROUND) {
            final ConsensusRoundIdentifier msgRoundId = posMessage.getRoundIdentifier();
            Map<String, Long> info = Map.of(
                    "round", (long) msgRoundId.getRoundNumber(),
                    "height", posMessage.getSignedPayload().getPayload().getHeight()
            );
            final RoundState roundstate =
                    futureRoundStateBuffer.computeIfAbsent(
                            msgRoundId.getRoundNumber(), k -> roundStateCreator.apply(info)
                    );
            buffer.accept(roundstate, posMessage);
        }
    }


    private void setCurrentRound(final int roundNumber) {
        if(roundNumber==0 && currentRound.isPresent()) {
            return;
        }
        LOG.debug("Starting new round {}", roundNumber);
        // validate the current round

        if (futureRoundStateBuffer.containsKey(roundNumber)) {
            currentRound = Optional.of(
                            roundFactory.createNewRoundWithState(parentHeader, futureRoundStateBuffer.get(roundNumber)));
            checkMessages();


            futureRoundStateBuffer.keySet().removeIf(k -> k <= roundNumber);
        } else {
            currentRound = Optional.of(roundFactory.createNewRound(parentHeader, roundNumber));
        }
        currentRound.get().setPosBlockHeightManager(this);
//    // discard roundChange messages from the current and previous rounds
//    roundChangeManager.discardRoundsPriorTo(currentRound.get().getRoundIdentifier());
    }

    private void checkMessages() {
        if (currentRound.isPresent()) {
            for (SelectLeader selectLeaderMessage : currentRound.get().getRoundState().getSelectLeaderMessages()) {
                handleSelectLeaderMessage(selectLeaderMessage);
            }
            for (Propose proposeMessage : currentRound.get().getRoundState().getProposeMessages()) {
                handleProposalMessage(proposeMessage);
            }
            for (Vote voteMessage : currentRound.get().getRoundState().getVoteMessages()) {
                handleVoteMessage(voteMessage);
            }
            for (Commit commitMessage : currentRound.get().getRoundState().getCommitMessages()) {
                handleCommitMessage(commitMessage);
            }
            for (ViewChange viewChangeMessage : currentRound.get().getRoundState().getViewChangeMessages()) {
                handleViewChangePayload(viewChangeMessage);
            }
        }
    }


    private void callUpdateRound(int roundNumber) {
        if(currentRound.isEmpty()) {
            setCurrentRound(roundNumber);
        }
        currentRound.get().updateRound(blockchain.getChainHeadBlock(), clock, roundNumber);

    }


    public long getChainHeight() {
        return parentHeader.getBesuHeader().getNumber() + 1;
    }

    public BlockHeader getParentBlockHeader() {
        return parentHeader.getBesuHeader();
    }

    public MessageAge determineAgeOfPayload(final int messageRoundNumber) {
        final int currentRoundNumber = currentRound.map(r -> r.getRoundIdentifier().getRoundNumber()).orElse(-1);
        if (messageRoundNumber > currentRoundNumber) {
            return MessageAge.FUTURE_ROUND;
        } else if (messageRoundNumber == currentRoundNumber) {
            return MessageAge.CURRENT_ROUND;
        }
        return MessageAge.PRIOR_ROUND;
    }

    /**
     * The enum Message age.
     */
    public enum MessageAge {
        /**
         * Prior round message age.
         */
        PRIOR_ROUND,
        /**
         * Current round message age.
         */
        CURRENT_ROUND,
        /**
         * Future round message age.
         */
        FUTURE_ROUND
    }


}