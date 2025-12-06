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
import org.hyperledger.besu.config.PosConfigOptions;
import org.hyperledger.besu.consensus.common.bft.BftExtraData;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.pos.messagedata.PosMessage;
import org.hyperledger.besu.crypto.SECPSignature;
import org.hyperledger.besu.ethereum.core.Block;
import org.hyperledger.besu.ethereum.eth.manager.EthPeers;

import org.hyperledger.besu.consensus.common.bft.events.RoundExpiry;
import org.hyperledger.besu.consensus.common.bft.messagewrappers.BftMessage;
import org.hyperledger.besu.consensus.common.bft.payload.SignedData;
import org.hyperledger.besu.consensus.pos.core.*;
import org.hyperledger.besu.consensus.pos.messagewrappers.*;
import org.hyperledger.besu.consensus.pos.network.PosMessageTransmitter;
import org.hyperledger.besu.consensus.pos.payload.*;

import java.time.Clock;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.collect.Maps;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.eth.sync.state.SyncState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for starting/clearing Consensus rounds at a given block height.
 * Implements Pure PoS with Voting Phase (>50% consensus).
 */
public class PosBlockHeightManager implements BasePosBlockHeightManager {

    private static final Logger LOG = LoggerFactory.getLogger(PosBlockHeightManager.class);

    private final PosRoundFactory roundFactory;
    private final BlockHeader parentHeader;
    private final Map<Integer, RoundState> futureRoundStateBuffer = Maps.newHashMap();
    private final Clock clock;
    private final PosFinalState finalState;
    private final PosProposerSelector proposerSelector;
    private final PosMessageTransmitter transmitter;
    private final Function<Map<String, Long>, RoundState> roundStateCreator;
    private final Blockchain blockchain;
    private final PosRoundFactory.MessageFactory messageFactory;
    private Optional<PosRound> currentRound = Optional.empty();

    private final EthPeers ethPeers;
    private final SyncState syncState;

    @Getter
    private boolean isFirstRoundStarted = false;

    private int retryCounter = 10;
    private boolean blockImported = false; // Flag to prevent double imports in the same round

    /**
     * Instantiates a new Pos block height manager.
     */
    public PosBlockHeightManager(
            final BlockHeader parentHeader,
            final PosFinalState finalState,
            final PosRoundFactory posRoundFactory,
            final Clock clock,
            final PosRoundFactory.MessageFactory messageFactory,
            PosProposerSelector proposerSelector,
            PosMessageTransmitter transmitter,
            PosConfigOptions posConfig,
            Blockchain blockchain,
            EthPeers ethPeers,
            SyncState syncState) {
        this.parentHeader = parentHeader;
        this.roundFactory = posRoundFactory;
        this.clock = clock;
        this.finalState = finalState;
        this.proposerSelector = proposerSelector;
        this.transmitter = transmitter;
        this.messageFactory = messageFactory;

        roundStateCreator = (infoMap) ->
                new RoundState(
                        new ConsensusRoundIdentifier(getChainHeight(), Math.toIntExact(infoMap.get("round"))),
                        finalState.getQuorum(),
                        infoMap.get("height"));
        this.blockchain = blockchain;
        this.ethPeers = ethPeers;
        this.syncState = syncState;

        final long nextBlockHeight = getChainHeight();

        // Phase 3: Seed derivation from parent block hash (Deterministic FTS)
        proposerSelector.setPreviousSeed(parentHeader.getHash());

        final ConsensusRoundIdentifier roundIdentifier;
        if (nextBlockHeight > 1) {
            roundIdentifier = new ConsensusRoundIdentifier(nextBlockHeight, (int) nextBlockHeight);
        } else {
            // For genesis handling or restart
            // Assuming we read extra data from parent to determine next round if needed
            // But usually for new height we start at 0.
            // Simplified for this context:
            roundIdentifier = new ConsensusRoundIdentifier(nextBlockHeight, (int) nextBlockHeight);
        }

        if (roundIdentifier.getRoundNumber() == 0) {
            finalState.getBlockTimer().startTimer(roundIdentifier, parentHeader::getTimestamp);
            setCurrentRound(roundIdentifier.getRoundNumber());
        } else {
            startNewRound(roundIdentifier);
        }
    }

    /**
     * Entry point for the RoundTimer expiry event.
     */
    @Override
    public void roundExpired(final RoundExpiry expire) {
        LOG.debug("Round expired event received for view: {}", expire.getView());

        if (currentRound.isPresent() && !expire.getView().equals(currentRound.get().getRoundIdentifier())) {
            LOG.trace("Ignoring Round timer expired which does not match current round.");
            return;
        }

        handleBlockTimerExpiry(expire.getView());
    }

    @Override
    public void handleBlockTimerExpiry(final ConsensusRoundIdentifier roundIdentifier) {
        finalState.getBlockTimer().cancelTimer();

        final long now = clock.millis() / 1000;

        if (retryCounter < 10) {
            finalState.getBlockTimer().startTimer(roundIdentifier, () -> now);
            retryCounter++;
            return;
        }

        var round = roundIdentifier;
        LOG.debug("Timer expired: before round {} , roundIdentifier {}", round, roundIdentifier);

        if (currentRound.isEmpty() && !isFirstRoundStarted && roundIdentifier.getRoundNumber() == 0) {
            setCurrentRound(0);
        }

        if (syncState.isInSync() && ethPeers.peerCount() > 0) {
            round = new ConsensusRoundIdentifier(roundIdentifier.getSequenceNumber(),
                    roundIdentifier.getRoundNumber() + 1);

            isFirstRoundStarted = true;
            startNewRound(round);

        } else {
            finalState.getBlockTimer().startTimer(round, () -> now);
        }
    }

    @Override
    public boolean checkValidState(int msgCode) {
        if (currentRound.isEmpty()) return false;
        State currentState = getRoundState().getCurrentState();

        // Allow Proposal processing in PROPOSE state
        if (currentState == State.PROPOSE && PosMessage.PROPOSE.getCode() == msgCode) {
            return true;
        }
        // Allow Voting in PROPOSE or VOTE state
        if ((currentState == State.PROPOSE || currentState == State.VOTE) && PosMessage.VOTE.getCode() == msgCode) {
            return true;
        }
        // Allow ViewChange
        return false;
    }

    private void startNewRound(ConsensusRoundIdentifier targetRound) {
        finalState.getBlockTimer().cancelTimer();
        long headerTimeStampSeconds = Math.round(clock.millis() / 1000D);
        finalState.getBlockTimer().startTimer(targetRound, () -> headerTimeStampSeconds);

        setCurrentRound(targetRound.getRoundNumber());
        getRoundState().setCurrentState(State.PROPOSE);
        blockImported = false; // Reset import flag for new round
        callUpdateRound(targetRound.getRoundNumber());

        Address expectedLeader = proposerSelector.getLeader(targetRound.getRoundNumber());
        proposerSelector.setCurrentLeader(Optional.ofNullable(expectedLeader));

        LOG.info("Round {} started. Expected Leader: {}", targetRound.getRoundNumber(), expectedLeader);

        assert expectedLeader != null;
        if (finalState.getLocalAddress().equals(expectedLeader) && currentRound.isPresent()) {
            LOG.info("We are the leader for Round {}. Proposing block...", targetRound.getRoundNumber());
            // This creates block, sends proposal, and effectively votes for itself implicitly or explicitly
            currentRound.get().createProposalAndTransmit(clock);
        }
    }

    private RoundState getRoundState() {
        return currentRound.map(PosRound::getRoundState).orElse(null);
    }

    // --- Message Consumption ---

    public void consumeProposeMessage(final Propose msg) {
        actionOrBufferMessage(
                msg,
                currentRound.isPresent() ? this::handleProposalMessage : (ignore) -> {
                },
                RoundState::addProposalMessage);
    }

    public void consumeVoteMessage(final Vote msg) {
        actionOrBufferMessage(
                msg,
                currentRound.isPresent() ? this::handleVoteMessage : (ignore) -> {
                },
                RoundState::addVoteMessage);
    }

    // --- Logic Implementation ---

    public void handleProposalMessage(final Propose msg) {
        LOG.debug("Received a proposal message. round={}. author={}", msg.getRoundIdentifier(), msg.getAuthor());

        if (validateProposal(msg.getSignedPayload())) {
            LOG.info("Valid proposal received from {}. Sending Vote...", msg.getAuthor());
            getRoundState().setProposeMessage(msg);

            // Advance state to VOTE
            getRoundState().setCurrentState(State.VOTE);

            // Send Vote for this proposal
            sendVote(msg.getSignedPayload().getPayload().getProposedBlock());

        } else {
            LOG.warn("Invalid proposal from {}.", msg.getAuthor());
        }
    }

    @Override
    public void handleVoteMessage(final Vote msg) {
        LOG.debug("Received a vote message. round={}. author={}", msg.getRoundIdentifier(), msg.getAuthor());

        // Add vote to the round state
        getRoundState().addVoteMessage(msg);

        // Try to reach consensus
        checkStateAndImport();
    }

    /**
     * Checks if we have a valid proposal and > 50% votes.
     * If so, imports the block.
     */
    private void checkStateAndImport() {
        if (currentRound.isEmpty() || blockImported) {
            return;
        }

        RoundState state = getRoundState();
        Propose proposal = state.getProposeMessage();

        // 1. Must have a valid proposal
        if (proposal == null) {
            return;
        }

        // 2. Count votes matching the proposed block hash
        Block proposedBlock = proposal.getSignedPayload().getPayload().getProposedBlock();
        org.hyperledger.besu.datatypes.Hash blockHash = proposedBlock.getHash();

        long matchingVotes = state.getVoteMessages().stream()
                .filter(v -> v.getDigest().equals(blockHash))
                .map(BftMessage::getAuthor)
                .distinct()
                .count();

        int totalValidators = currentRound.get().getNodeSet().totalSize();
        // Threshold: > 50%
        int threshold = (totalValidators / 2) + 1;

        LOG.debug("Consensus Check: Votes={}, Threshold={}", matchingVotes, threshold);

        if (matchingVotes >= threshold) {
            LOG.info("Consensus Reached (>50% votes). Importing block...");

            boolean success = currentRound.get().importBlockToChain();

            if (success) {
                blockImported = true;
                LOG.info("Block imported successfully. Round complete.");

                finalState.getBlockTimer().cancelTimer();
            } else {
                LOG.warn("Block failed to import.");
            }
        }
    }

    private void sendVote(Block block) {
        if (currentRound.isPresent()) {
            try {
                // Create Vote Payload
                VotePayload votePayload = messageFactory.createVotePayload(block,getRoundState().getRoundIdentifier());

                // Sign Vote
                SECPSignature signature = finalState.getNodeKey().sign(votePayload.hashForSignature());
                LOG.debug("after signature ,before signedVote");
                SignedData<VotePayload> signedVote = SignedData.create(votePayload, signature);

                // Wrap in Message
                Vote vote = messageFactory.createVote(signedVote);

                // Multicast to network
                transmitter.multicastVote(vote);

                // Add our own vote to local state so we count ourselves
                handleVoteMessage(vote);

            } catch (Exception e) {
                LOG.error("Failed to create/send vote", e);
            }
        }
    }

    private boolean validateProposal(SignedData<ProposePayload> payload) {
        int roundNumber = payload.getPayload().getRoundIdentifier().getRoundNumber();
        Address proposedAuthor = payload.getAuthor();

        if (roundNumber != getRoundState().getRoundIdentifier().getRoundNumber()) {
            return false;
        }

        Optional<Address> expectedLeader = proposerSelector.getCurrentProposer();
        if (expectedLeader.isEmpty() || !expectedLeader.get().equals(proposedAuthor)) {
            LOG.warn("Proposal rejected: Author {} is not the expected leader {}", proposedAuthor, expectedLeader);
            return false;
        }

        long headerTimestamp = payload.getPayload().getProposedBlock().getHeader().getTimestamp();
        long systemTime = (long) (System.currentTimeMillis() / 1000D);
        return headerTimestamp - systemTime <= 5;
    }

    // --- Helper Methods ---

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
                    "round", (long) msgRoundId.getRoundNumber()
            );
            final RoundState roundstate =
                    futureRoundStateBuffer.computeIfAbsent(
                            msgRoundId.getRoundNumber(), k -> roundStateCreator.apply(info)
                    );
            buffer.accept(roundstate, posMessage);
        }
    }

    private void setCurrentRound(final int roundNumber) {
        if (roundNumber == 0 && currentRound.isPresent()) {
            return;
        }
        LOG.debug("Starting new round {}", roundNumber);

        if (futureRoundStateBuffer.containsKey(roundNumber)) {
            currentRound = Optional.of(
                    roundFactory.createNewRoundWithState(parentHeader, futureRoundStateBuffer.get(roundNumber)));
            checkMessages();
            futureRoundStateBuffer.keySet().removeIf(k -> k <= roundNumber);
        } else {
            currentRound = Optional.of(roundFactory.createNewRound(parentHeader, roundNumber));
        }
        currentRound.get().setPosBlockHeightManager(this);
    }

    private void checkMessages() {
        if (currentRound.isPresent()) {
            for (Propose proposeMessage : currentRound.get().getRoundState().getProposeMessages()) {
                handleProposalMessage(proposeMessage);
            }
            // Also process buffered votes
            for (Vote voteMessage : currentRound.get().getRoundState().getVoteMessages()) {
                handleVoteMessage(voteMessage);
            }
        }
    }

    private void callUpdateRound(int roundNumber) {
        if (currentRound.isEmpty()) {
            setCurrentRound(roundNumber);
        }
        currentRound.get().updateNodes(blockchain.getChainHeadBlock());
    }

    public long getChainHeight() {
        LOG.debug("parentHeader{}", parentHeader);
        return parentHeader.getNumber() + 1;
    }

    public BlockHeader getParentBlockHeader() {
        return parentHeader;
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

    public enum MessageAge {
        PRIOR_ROUND,
        CURRENT_ROUND,
        FUTURE_ROUND
    }
}