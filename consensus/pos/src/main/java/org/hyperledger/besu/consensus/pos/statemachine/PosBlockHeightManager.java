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
import org.hyperledger.besu.consensus.common.bft.payload.Payload;
import org.hyperledger.besu.consensus.pos.core.PosBlockHeader;
import org.hyperledger.besu.consensus.pos.core.PosFinalState;
import org.hyperledger.besu.consensus.pos.messagewrappers.*;
import org.hyperledger.besu.consensus.pos.validation.MessageValidatorFactory;

import java.time.Clock;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import com.google.common.collect.Maps;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.core.BlockHeader;
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

    private Optional<PosRound> currentRound = Optional.empty();
    private boolean isEarlyRoundChangeEnabled = false;

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
            final PosRoundFactory.MessageFactory messageFactory, PosProposerSelector proposerSelector
    ) {
        this.parentHeader = parentHeader;
        this.roundFactory = posRoundFactory;
        this.messageFactory = messageFactory;
        this.clock = clock;
        this.finalState = finalState;
        this.proposerSelector = proposerSelector;


        final long nextBlockHeight = getChainHeight();
        final ConsensusRoundIdentifier roundIdentifier =
                new ConsensusRoundIdentifier(nextBlockHeight, 0);

        finalState.getBlockTimer().startTimer(roundIdentifier, parentHeader.getBesuHeader()::getTimestamp);
    }

    /**
     * Instantiates a new Pos block height manager. Secondary constructor with early round change
     * option.
     *
     * @param parentHeader the parent header
     * @param finalState the final state
     * @param posRoundFactory the pos round factory
     * @param clock the clock
     * @param messageFactory the message factory
     * @param isEarlyRoundChangeEnabled enable round change when f+1 RC messages are received
     */
    public PosBlockHeightManager(
            final PosBlockHeader parentHeader,
            final PosFinalState finalState,
            final PosRoundFactory posRoundFactory,
            final Clock clock,
            final PosRoundFactory.MessageFactory messageFactory, PosProposerSelector proposerSelector,
//          final PosValidatorProvider validatorProvider,

            final boolean isEarlyRoundChangeEnabled) {
        this(
                parentHeader,
                finalState,
                posRoundFactory,
                clock,
                messageFactory, proposerSelector
        );
        this.isEarlyRoundChangeEnabled = isEarlyRoundChangeEnabled;
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
//    startNewRound(0);
//
//    final PosRound posRound = currentRound.get();
//
//    logValidatorChanges(posRound);
//
//    if (roundIdentifier.equals(posRound.getRoundIdentifier())) {
//      buildBlockAndMaybePropose(roundIdentifier, posRound);
//    } else {
//      LOG.trace(
//              "Block timer expired for a round ({}) other than current ({})",
//              roundIdentifier,
//              posRound.getRoundIdentifier());
//    }
    }

    private void buildBlockAndMaybePropose(
            final ConsensusRoundIdentifier roundIdentifier, final PosRound posRound) {

//    // mining will be checked against round 0 as the current round is initialised to 0 above
//    final boolean isProposer =
//            finalState.isLocalNodeProposerForRound(posRound.getRoundIdentifier());
//
//    if (!isProposer) {
//      // nothing to do here...
//      LOG.trace("This node is not a proposer so it will not send a proposal: " + roundIdentifier);
//      return;
//    }
//
//    final long headerTimeStampSeconds = Math.round(clock.millis() / 1000D);
//    final PosBlock block = posRound.createBlock(headerTimeStampSeconds);
//    if (!block.isEmpty()) {
//      LOG.trace(
//              "Block is not empty and this node is a proposer so it will send a proposal: "
//                      + roundIdentifier);
//      posRound.updateStateWithProposeAndTransmit(block);
//    } else {
//      // handle the block times period
//      final long currentTimeInMillis = finalState.getClock().millis();
//      boolean emptyBlockExpired =
//              finalState
//                      .getBlockTimer()
//                      .checkEmptyBlockExpired(parentHeader::getTimestamp, currentTimeInMillis);
//      if (emptyBlockExpired) {
//        LOG.trace(
//                "Block has no transactions and this node is a proposer so it will send a proposal: "
//                        + roundIdentifier);
//        posRound.updateStateWithProposeAndTransmit(block);
//      } else {
//        LOG.trace(
//                "Block has no transactions but emptyBlockPeriodSeconds did not expired yet: "
//                        + roundIdentifier);
//        finalState
//                .getBlockTimer()
//                .resetTimerForEmptyBlock(
//                        roundIdentifier, parentHeader::getTimestamp, currentTimeInMillis);
//        finalState.getRoundTimer().cancelTimer();
//        currentRound = Optional.empty();
//      }
//    }
    }

    /**
     * If the list of validators for the next block to be proposed/imported has changed from the
     * previous block, log the change. Only log for round 0 (i.e. once per block).
     *
     * @param posRound The current round
     */
    private void logValidatorChanges(final PosRound posRound) {
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
    }
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

//    doRoundChange(posRound.getRoundIdentifier().getRoundNumber() + 1);
    }

    private synchronized void doRoundChange(final int newRoundNumber) {
//    if (currentRound.isPresent()
//            && currentRound.get().getRoundIdentifier().getRoundNumber() >= newRoundNumber) {
//      return;
//    }
//    LOG.debug(
//            "Round has expired or changing based on RC quorum, creating VotedCertificate and notifying peers. round={}",
//            currentRound.get().getRoundIdentifier());
//    final Optional<VotedCertificate> preparedCertificate = currentRound.get().constructVotedCertificate();
//
//    if (preparedCertificate.isPresent()) {
//      latestVotedCertificate = preparedCertificate;
//    }
//
//    startNewRound(newRoundNumber);
//    if (currentRound.isEmpty()) {
//      LOG.info("Failed to start round ");
//      return;
//    }
//    PosRound posRoundNew = currentRound.get();
//
//    try {
//      final RoundChange localRoundChange =
//              messageFactory.createRoundChange(
//                      posRoundNew.getRoundIdentifier(), latestVotedCertificate);
//
//      handleRoundChangePayload(localRoundChange);
//    } catch (final SecurityModuleException e) {
//      LOG.warn("Failed to create signed RoundChange message.", e);
//    }
//
//    transmitter.multicastRoundChange(posRoundNew.getRoundIdentifier(), latestVotedCertificate);
    }

    public void handleProposePayload(final Propose proposal) {
//    LOG.trace("Received a Propose Payload.");
//    final MessageAge messageAge =
//            determineAgeOfPayload(proposal.getRoundIdentifier().getRoundNumber());
//
//    if (messageAge == MessageAge.PRIOR_ROUND) {
//      LOG.trace("Received Propose Payload for a prior round={}", proposal.getRoundIdentifier());
//    } else {
//      if (messageAge == MessageAge.FUTURE_ROUND) {
//        if (!futureRoundProposeMessageValidator.validateProposeMessage(proposal)) {
//          LOG.info("Received future Propose which is illegal, no round change triggered.");
//          return;
//        }
//        startNewRound(proposal.getRoundIdentifier().getRoundNumber());
//      }
//      currentRound.ifPresent(r -> r.handleProposeMessage(proposal));
//    }
    }

    public void handleVotePayload(final Vote prepare) {
//    LOG.trace("Received a Vote Payload.");
//    actionOrBufferMessage(
//            prepare,
//            currentRound.isPresent() ? currentRound.get()::handleVoteMessage : (ignore) -> {},
//            RoundState::addVoteMessage);
    }


    public void handleCommitPayload(final Commit commit) {
//    LOG.trace("Received a Commit Payload.");
//    actionOrBufferMessage(
//            commit,
//            currentRound.isPresent() ? currentRound.get()::handleCommitMessage : (ignore) -> {},
//            RoundState::addCommitMessage);
    }

//    private <P extends Payload, M extends BftMessage<P>> void actionOrBufferMessage(
//            final M posMessage,
//            final Consumer<M> inRoundHandler,
//            final BiConsumer<RoundState, M> buffer) {
//        final MessageAge messageAge =
//                determineAgeOfPayload(posMessage.getRoundIdentifier().getRoundNumber());
//        if (messageAge == MessageAge.CURRENT_ROUND) {
//            inRoundHandler.accept(posMessage);
//        } else if (messageAge == MessageAge.FUTURE_ROUND) {
//            final ConsensusRoundIdentifier msgRoundId = posMessage.getRoundIdentifier();
//            final RoundState roundstate =
//                    futureRoundStateBuffer.computeIfAbsent(
//                            msgRoundId.getRoundNumber(), k ->  new RoundState(
//                                    msgRoundId,
//                                    finalState.getQuorum(),
//                                    posMessage
//                            ););
//            buffer.accept(roundstate, posMessage);
//        }
//    }

//    public void handleRoundChangePayload(final ViewChange message) {
//        final ConsensusRoundIdentifier targetRound = message.getRoundIdentifier();
//
//        LOG.debug(
//                "Round change from {}: block {}, round {}",
//                message.getAuthor(),
//                message.getRoundIdentifier().getSequenceNumber(),
//                message.getRoundIdentifier().getRoundNumber());
//
//        final MessageAge messageAge =
//                determineAgeOfPayload(message.getRoundIdentifier().getRoundNumber());
//        if (messageAge == MessageAge.PRIOR_ROUND) {
//            LOG.debug("Received RoundChange Payload for a prior round. targetRound={}", targetRound);
//            return;
//        }
//        Optional<Collection<ViewChange>> result=Optional.empty();
//        receivedMessages.put(finalState.getLocalAddress(),message);
//        if(receivedMessages.size()>=finalState.getQuorum()){
//            result = Optional.of(receivedMessages.values());
//        }
//
//        if (!isEarlyRoundChangeEnabled) {
//            if (result.isPresent()) {
//                LOG.debug(
//                        "Received sufficient RoundChange messages to change round to targetRound={}", targetRound);
//                if (messageAge == MessageAge.FUTURE_ROUND) {
//                    startNewRound(targetRound.getRoundNumber());
//                }
//                var proposer=proposerSelector.selectProposerForRound(targetRound);
//                if (proposer.equals(finalState.getLocalAddress())) {
//                    if (currentRound.isEmpty()) {
//                        startNewRound(0);
//                    }
//                    currentRound
//                            .get()
//                            .startRound(TimeUnit.MILLISECONDS.toSeconds(clock.millis()));
//                }
//            }
//        } else {
//            if (currentRound.isEmpty()) {
//                startNewRound(0);
//            }
//            int currentRoundNumber = currentRound.get().getRoundIdentifier().getRoundNumber();
//            // If this node is proposer for the current round, check if quorum is achieved for RC messages
//            // aiming this round
//            var proposer=proposerSelector.selectProposerForRound(targetRound);
//            if (targetRound.getRoundNumber() == currentRoundNumber
//                    && proposer.equals(finalState.getLocalAddress())
//                    && result.isPresent()) {
//
//                currentRound
//                        .get()
//                        .startRound(TimeUnit.MILLISECONDS.toSeconds(clock.millis()));
//            }
//
//            doRoundChange(nextHigherRound.get());
//            // check if f+1 RC messages for future rounds are received
//            //ToDo
////      PosRound posRound = currentRound.get();
////      Optional<Integer> nextHigherRound =
////              roundChangeManager.futureRCQuorumReceived(posRound.getRoundIdentifier());
////      if (nextHigherRound.isPresent()) {
////        LOG.info(
////                "Received sufficient RoundChange messages to change round to targetRound={}",
////                nextHigherRound.get());
////        doRoundChange(nextHigherRound.get());
////      }
//        }
//    }

    private void startNewRound(final int roundNumber) {

        LOG.debug("Starting new round {}", roundNumber);
//    // validate the current round

//    if (futureRoundStateBuffer.containsKey(roundNumber)) {
//      currentRound =
//              Optional.of(
//                      roundFactory.createNewRoundWithState(
//                              parentHeader, futureRoundStateBuffer.get(roundNumber)));
//      futureRoundStateBuffer.keySet().removeIf(k -> k <= roundNumber);
//    } else {
//      currentRound = Optional.of(roundFactory.createNewRound(parentHeader, roundNumber));
//    }
//    // discard roundChange messages from the current and previous rounds
//    roundChangeManager.discardRoundsPriorTo(currentRound.get().getRoundIdentifier());
    }


    public long getChainHeight() {
        return parentHeader.getBesuHeader().getNumber()+1;
    }

    public BlockHeader getParentBlockHeader() {
        return parentHeader.getBesuHeader();
    }

    private MessageAge determineAgeOfPayload(final int messageRoundNumber) {
//    final int currentRoundNumber =
//            currentRound.map(r -> r.getRoundIdentifier().getRoundNumber()).orElse(-1);
//    if (messageRoundNumber > currentRoundNumber) {
//      return MessageAge.FUTURE_ROUND;
//    } else if (messageRoundNumber == currentRoundNumber) {
//      return MessageAge.CURRENT_ROUND;
//    }
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