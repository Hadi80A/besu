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

import com.google.common.annotations.VisibleForTesting;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.events.RoundExpiry;
import org.hyperledger.besu.consensus.common.bft.messagewrappers.BftMessage;
import org.hyperledger.besu.consensus.common.bft.payload.SignedData;
import org.hyperledger.besu.consensus.pos.core.PosBlock;
import org.hyperledger.besu.consensus.pos.core.PosBlockHeader;
import org.hyperledger.besu.consensus.pos.core.PosFinalState;
import org.hyperledger.besu.consensus.pos.messagewrappers.*;
import org.hyperledger.besu.consensus.pos.network.PosMessageTransmitter;
import org.hyperledger.besu.consensus.pos.payload.*;

import java.time.Clock;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import com.google.common.collect.Maps;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.core.Block;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.plugin.services.securitymodule.SecurityModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for starting/clearing Consensus rounds at a given block height. One of these is
 * created when a new block is imported to the chain. It immediately then creates a Round-0 object,
 * and sends a Propose message. If the round times out prior to importing a block, this class is
 * responsible for creating a RoundChange message and transmitting it.
 */
public class PosBlockHeightManager implements BasePosBlockHeightManager  {

    private static final Logger LOG = LoggerFactory.getLogger(PosBlockHeightManager.class);

    private final PosRoundFactory roundFactory;
    private final PosBlockHeader parentHeader;
    private final PosRoundFactory.MessageFactory messageFactory;
    private final Map<Integer, RoundState> futureRoundStateBuffer = Maps.newHashMap();
    private final Clock clock;
    private final PosFinalState finalState;
    private final PosProposerSelector proposerSelector;
    private final PosMessageTransmitter transmitter;
    private final Function<Map<String,Long>, RoundState> roundStateCreator;
    private final Blockchain blockchain;
    private Optional<PosRound> currentRound = Optional.empty();
    private boolean isEarlyRoundChangeEnabled = false;
    private final RoundChangeManager roundChangeManager;

    // Store only 1 round change per round per validator
    @VisibleForTesting
    final Map<Address, ViewChange> receivedMessages = Maps.newLinkedHashMap();



    /**
     * Instantiates a new Pos block height manager.
     *
     * @param parentHeader the parent header
     * @param finalState the final state
     * @param posRoundFactory the pos round factory
     * @param clock the clock
     * @param messageFactory the message factory
     */
    public PosBlockHeightManager(
            final PosBlockHeader parentHeader,
            final PosFinalState finalState,
            final PosRoundFactory posRoundFactory,
            final Clock clock,
            final PosRoundFactory.MessageFactory messageFactory, PosProposerSelector proposerSelector, PosMessageTransmitter transmitter, Blockchain blockchain, RoundChangeManager roundChangeManager
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
                                new ConsensusRoundIdentifier(0, Math.toIntExact(infoMap.get("round"))),
                                finalState.getQuorum(),
                                infoMap.get("height"));
        this.blockchain = blockchain;
        this.roundChangeManager = roundChangeManager;

        final long nextBlockHeight = getChainHeight();
        final ConsensusRoundIdentifier roundIdentifier =
                new ConsensusRoundIdentifier(nextBlockHeight, 0);

        finalState.getBlockTimer().startTimer(roundIdentifier, parentHeader.getBesuHeader()::getTimestamp);
    }

   
    @Override
    public void handleBlockTimerExpiry(final ConsensusRoundIdentifier roundIdentifier) {
    if (currentRound.isPresent()) {
      // It is possible for the block timer to take longer than it should due to the precision of
      // the timer in Java and the OS. This means occasionally the proposal can arrive before the
      // block timer expiry and hence the round has already been set. There is no negative impact
      // on the protocol in this case.
      return;
    }
//
    startNewRound(0);
    long headerTimeStampSeconds = Math.round(clock.millis() / 1000D);

    Block lastBlock = blockchain.getChainHeadBlock();
    currentRound.get().updateRound(lastBlock,headerTimeStampSeconds);
//
    final PosRound posRound = currentRound.get();
//
//    logValidatorChanges(posRound);
//
    if (roundIdentifier.equals(posRound.getRoundIdentifier())) {
      posRound.createProposalAndTransmit(headerTimeStampSeconds);
    } else {
      LOG.trace(
              "Block timer expired for a round ({}) other than current ({})",
              roundIdentifier,
              posRound.getRoundIdentifier());
    }
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
    if (currentRound.isEmpty()) {
      LOG.error(
              "Received Round timer expiry before round is created timerRound={}", expire.getView());
      return;
    }

    PosRound posRound = currentRound.get();
    if (!expire.getView().equals(posRound.getRoundIdentifier())) {
      LOG.trace(
              "Ignoring Round timer expired which does not match current round. round={}, timerRound={}",
              posRound.getRoundIdentifier(),
              expire.getView());
      return;
    }

    doRoundChange(posRound.getRoundIdentifier().getRoundNumber() + 1);
    }

//    public void startRoundWith( final long headerTimestamp) {
////    final Optional<Block> bestBlockFromRoundChange = roundChangeArtifacts.getBlock();
////
////    final RoundChangeCertificate roundChangeCertificate =
////            roundChangeArtifacts.getRoundChangeCertificate();
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

    final PosBlockHeightManager.MessageAge messageAge =
            determineAgeOfPayload(message.getRoundIdentifier().getRoundNumber());
    if (messageAge == PosBlockHeightManager.MessageAge.PRIOR_ROUND) {
        LOG.debug("Received RoundChange Payload for a prior round. targetRound={}", targetRound);
        return;
    }
    Optional<Collection<ViewChange>> result=Optional.empty();
    finalState.getReceivedMessages().put(finalState.getLocalAddress(),message);
    if(finalState.getReceivedMessages().size()>=finalState.getQuorum()){
        result = Optional.of(finalState.getReceivedMessages().values());
    }

    if (!isEarlyRoundChangeEnabled) {
        if (result.isPresent()) {
            LOG.debug(
                    "Received sufficient RoundChange messages to change round to targetRound={}", targetRound);
            if (messageAge == PosBlockHeightManager.MessageAge.FUTURE_ROUND) {
                startNewRound(targetRound.getRoundNumber());
            }
            var proposer=proposerSelector.selectLeader();
            if (proposer.equals(finalState.getLocalAddress())) {
                if (currentRound.isEmpty()) {
                    startNewRound(0);
                }
                    startNewRound(currentRound.get().getRoundIdentifier().getRoundNumber());
            }
        }
    } else {
        if (currentRound.isEmpty()) {
            startNewRound(0);
        }
        int currentRoundNumber = currentRound.get().getRoundIdentifier().getRoundNumber();
        // If this node is proposer for the current round, check if quorum is achieved for RC messages
        // aiming this round
        var proposer=proposerSelector.getCurrentProposer();
        if (targetRound.getRoundNumber() == currentRoundNumber
                && proposer.equals(finalState.getLocalAddress())
                && result.isPresent()) {

            startNewRound(currentRoundNumber);
        }

        doRoundChange(currentRoundNumber+1);
        // check if f+1 RC messages for future rounds are received
          PosRound posRound = currentRound.get();//todo
          Optional<Integer> nextHigherRound =
                  roundChangeManager.futureRCQuorumReceived(posRound.getRoundIdentifier());
          if (nextHigherRound.isPresent()) {
            LOG.info(
                    "Received sufficient RoundChange messages to change round to targetRound={}",
                    nextHigherRound.get());
            doRoundChange(nextHigherRound.get());
          }
    }
}



    private synchronized void doRoundChange(final int newRoundNumber) {
    if (currentRound.isPresent() && currentRound.get().getRoundIdentifier().getRoundNumber() >= newRoundNumber) {
      return;
    }
    LOG.debug(
            "Round has expired or changing based on RC quorum, round={}", currentRound.get().getRoundIdentifier());

    startNewRound(newRoundNumber);
    if (currentRound.isEmpty()) {
      LOG.info("Failed to start round ");
      return;
    }
    PosRound posRoundNew = currentRound.get();

    try {
        ViewChangePayload unsigned= messageFactory.createViewChangePayload(posRoundNew.getRoundIdentifier(),posRoundNew.getRoundState().getHeight());
        SignedData<ViewChangePayload> signedData= currentRound.get().createSignedData(unsigned);
        final ViewChange localViewChangeMessage = messageFactory.createViewChange(signedData);

      handleViewChangePayload(localViewChangeMessage);

      transmitter.multicastRoundChange(localViewChangeMessage);
    } catch (final SecurityModuleException e) {
      LOG.warn("Failed to create signed RoundChange message.", e);
    }
    }

    private RoundState getRoundState(){
        return currentRound.get().getRoundState();
    }
    /**
     * Handle proposal message.
     *
     * @param msg the msg
     */
    public void handleProposalMessage(final Propose msg) {
        LOG.debug("Received a proposal message. round={}. author={}", currentRound.get().getRoundState(),
                msg.getAuthor());
        ProposePayload payload=msg.getSignedPayload().getPayload();
        final PosBlock block = payload.getProposedBlock();
        if (validateProposer(msg.getSignedPayload())){
            LOG.debug("Valid a proposal message.");
            getRoundState().setProposeMessage(msg);
            sendVote(block);
        }else {
            LOG.debug("Invalid a proposal message.");
        }
    }
    private boolean validateProposer(SignedData<ProposePayload> payload){
        return payload.getAuthor().equals(proposerSelector.getCurrentProposer()) &&
                payload.getPayload().getRoundIdentifier().getRoundNumber() == getRoundState().getRoundIdentifier().getRoundNumber() &&
                payload.getPayload().getHeight() == getRoundState().getHeight();

    }


    public void handleVoteMessage(final Vote msg) {
        LOG.debug("Received a vote message. round={}. author={}", getRoundState().getRoundIdentifier(), msg.getAuthor());
        getRoundState().addVoteMessage(msg);
        if(validateBlockHash(msg.getSignedPayload().getPayload().getDigest()) &&
                checkThreshold(getRoundState().getVoteMessages()) &&
                validateHeightAndRound(msg.getSignedPayload().getPayload())
        ) {
            sendCommit(getRoundState().getProposedBlock());
        }else {
            LOG.debug("Invalid a vote message.");
        }
    }
    private boolean checkThreshold(Set<?> msg){
        return msg.size()>=getRoundState().getQuorum();
    }


    private boolean validateHeightAndRound(PosPayload posPayload) {
        return posPayload.getHeight()==getRoundState().getHeight() &&
                posPayload.getRoundIdentifier().getRoundNumber()==getRoundState().getRoundIdentifier().getRoundNumber();
    }
    private boolean validateBlockHash(Hash blockHash) {
        PosBlock block=getRoundState().getProposeMessage().getSignedPayload().getPayload().getProposedBlock();
        return blockHash.equals(block.getHash());
    }
    private void sendVote(final PosBlock block) {
        LOG.debug("Sending vote message. round={}", getRoundState().getRoundIdentifier());
        try {
            VotePayload unsigned= messageFactory.createVotePayload(block);
            SignedData<VotePayload> signedData= currentRound.get().createSignedData(unsigned);
            final Vote localVoteMessage = messageFactory.createVote(signedData);
            getRoundState().addVoteMessage(localVoteMessage);
            transmitter.multicastVote(localVoteMessage);
        } catch (final SecurityModuleException e) {
            LOG.warn("Failed to create a signed Vote; {}", e.getMessage());
        }
    }


    private void sendCommit(final PosBlock block) {
        LOG.debug("Sending Commit message. round={}", getRoundState().getRoundIdentifier());
        try {
            CommitPayload unsigned= messageFactory.createCommitPayload(block);
            SignedData<CommitPayload> signedData= currentRound.get().createSignedData(unsigned);
            final Commit localCommitMessage = messageFactory.createCommit(signedData);
            getRoundState().addCommitMessage(localCommitMessage);
            transmitter.multicastCommit(localCommitMessage);
        } catch (final SecurityModuleException e) {
            LOG.warn("Failed to create a signed Commit; {}", e.getMessage());
        }
    }

    public void handleCommitMessage(final Commit msg) {
        LOG.debug("Received a commit message. round={}. author={}", getRoundState().getRoundIdentifier(),
                msg.getAuthor());
        getRoundState().addCommitMessage(msg);
        currentRound.get().importBlockToChain();
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
            Map<String,Long> info=Map.of(
                    "round", (long) msgRoundId.getRoundNumber(),
                    "height",posMessage.getSignedPayload().getPayload().getHeight()
            );
            final RoundState roundstate =
                    futureRoundStateBuffer.computeIfAbsent(
                            msgRoundId.getRoundNumber(), k ->  roundStateCreator.apply(info)
                    );
            buffer.accept(roundstate, posMessage);
        }
    }


    private void startNewRound(final int roundNumber) {

        LOG.debug("Starting new round {}", roundNumber);
    // validate the current round

    if (futureRoundStateBuffer.containsKey(roundNumber)) {
      currentRound =
              Optional.of(
                      roundFactory.createNewRoundWithState(
                              parentHeader, futureRoundStateBuffer.get(roundNumber)));
      futureRoundStateBuffer.keySet().removeIf(k -> k <= roundNumber);
    } else {
      currentRound = Optional.of(roundFactory.createNewRound(parentHeader, roundNumber));
    }
//    // discard roundChange messages from the current and previous rounds
//    roundChangeManager.discardRoundsPriorTo(currentRound.get().getRoundIdentifier());
    }


    public long getChainHeight() {
        return parentHeader.getBesuHeader().getNumber()+1;
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

    /** The enum Message age. */
    public enum MessageAge {
        /** Prior round message age. */
        PRIOR_ROUND,
        /** Current round message age. */
        CURRENT_ROUND,
        /** Future round message age. */
        FUTURE_ROUND
    }


}