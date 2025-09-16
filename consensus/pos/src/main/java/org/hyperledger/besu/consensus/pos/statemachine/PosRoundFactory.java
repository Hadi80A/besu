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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hyperledger.besu.config.PosConfigOptions;
import org.hyperledger.besu.consensus.common.bft.BftExtraDataCodec;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.payload.SignedData;
import org.hyperledger.besu.consensus.pos.PosBlockCreator;
import org.hyperledger.besu.consensus.pos.PosBlockCreatorFactory;
import org.hyperledger.besu.consensus.pos.PosProtocolSchedule;
import org.hyperledger.besu.consensus.pos.core.*;
import org.hyperledger.besu.consensus.pos.messagewrappers.*;
import org.hyperledger.besu.consensus.pos.network.PosMessageTransmitter;
import org.hyperledger.besu.consensus.pos.payload.*;
import org.hyperledger.besu.consensus.pos.validation.MessageValidatorFactory;
import org.hyperledger.besu.consensus.pos.vrf.VRF;
import org.hyperledger.besu.crypto.SECPPublicKey;
import org.hyperledger.besu.ethereum.ProtocolContext;
import org.hyperledger.besu.util.Subscribers;

/** The Pos round factory. */
public class PosRoundFactory {

    private static final Logger log = LogManager.getLogger(PosRoundFactory.class);
    private final PosFinalState finalState;
    private final PosBlockCreatorFactory blockCreatorFactory;
    private final ProtocolContext protocolContext;
    private final PosProtocolSchedule protocolSchedule;
    private final PosConfigOptions configOptions;
    private final Subscribers<PosMinedBlockObserver> minedBlockObservers;
    private final MessageValidatorFactory messageValidatorFactory;
    private final MessageFactory messageFactory;
    private final BftExtraDataCodec bftExtraDataCodec;
    private final ContractCaller contractCaller;
    private final NodeSet nodeSet;
    private final PosProposerSelector proposerSelector;

    /**
     * Instantiates a new Pos round factory.
     *
     * @param finalState the final state
     * @param protocolContext the protocol context
     * @param protocolSchedule the protocol schedule
     * @param minedBlockObservers the mined block observers
     * @param messageValidatorFactory the message validator factory
     * @param messageFactory the message factory
     * @param bftExtraDataCodec the bft extra data codec
     */
    public PosRoundFactory(
            final PosFinalState finalState,
            final ProtocolContext protocolContext,
            final PosProtocolSchedule protocolSchedule, PosConfigOptions configOptions,
            final Subscribers<PosMinedBlockObserver> minedBlockObservers,
            final MessageValidatorFactory messageValidatorFactory,
            final MessageFactory messageFactory,
            final BftExtraDataCodec bftExtraDataCodec, ContractCaller contractCaller, NodeSet nodeSet, PosProposerSelector proposerSelector) {
        this.finalState = finalState;
        this.blockCreatorFactory = finalState.getBlockCreatorFactory();
        this.protocolContext = protocolContext;
        this.protocolSchedule = protocolSchedule;
        this.configOptions = configOptions;
        this.minedBlockObservers = minedBlockObservers;
        this.messageValidatorFactory = messageValidatorFactory;
        this.messageFactory = messageFactory;
        this.bftExtraDataCodec = bftExtraDataCodec;
        this.contractCaller = contractCaller;
        this.nodeSet = nodeSet;
        this.proposerSelector = proposerSelector;
    }

    /**
     * Create new pos round.
     *
     * @param parentHeader the parent header
     * @param round the round
     * @return the pos round
     */
    public PosRound createNewRound(final PosBlockHeader parentHeader, final int round) {
        long nextBlockHeight = parentHeader.getBesuBlockHeader().getNumber() + 1;
        final ConsensusRoundIdentifier roundIdentifier =
                new ConsensusRoundIdentifier(nextBlockHeight, round);

        final RoundState roundState =
                new RoundState(
                        roundIdentifier,
                        finalState.getQuorum(),
                        nextBlockHeight
                        );

        return createNewRoundWithState(parentHeader, roundState);
    }

    /**
     * Create new Pos round with state.
     *
     * @param parentHeader the parent header
     * @param roundState the round state
     * @return the pos round
     */
    public PosRound createNewRoundWithState(
            final PosBlockHeader parentHeader, final RoundState roundState) {
        final PosBlockCreator blockCreator =
                blockCreatorFactory.create(roundState.getRoundIdentifier().getRoundNumber());
//
        final PosMessageTransmitter messageTransmitter =
                new PosMessageTransmitter(messageFactory, finalState.getValidatorMulticaster(),finalState.getLocalAddress());

        return new PosRound(
                roundState,
                blockCreator,
                protocolContext,
                protocolSchedule,
                minedBlockObservers,
                finalState.getNodeKey(),
                messageFactory,
                messageTransmitter,
                finalState.getRoundTimer(),
                configOptions,
                bftExtraDataCodec,
                parentHeader,
                contractCaller,
                nodeSet,
                proposerSelector,
                finalState
                );
    }

    public static class MessageFactory{

        public Propose createPropose(SignedData<ProposePayload> payload) {
            return new Propose(payload);
        }
        public Vote createVote(SignedData<VotePayload> payload) {
            return new Vote(payload);
        }

        public Commit createCommit(SignedData<CommitPayload> payload) {
            return new Commit(payload);
        }
        public ViewChange createViewChange(SignedData<ViewChangePayload> payload) {
            return new ViewChange(payload);
        }
        public SelectLeader createSelectLeader(SignedData<SelectLeaderPayload> payload) {
            return new SelectLeader(payload);
        }


        public SelectLeaderPayload createSelectLeaderPayload(ConsensusRoundIdentifier round, long height, VRF.Proof proof,
                                                             boolean isCandidate, SECPPublicKey publicKey) {
            return SelectLeaderPayload.builder()
                    .roundIdentifier(round)
                    .height(height)
                    .proof(proof)
                    .isCandidate(isCandidate)
                    .publicKey(publicKey)
                    .build();
        }
        
        public CommitPayload createCommitPayload(PosBlock block) {
            return CommitPayload.builder()
                    .block(block)
                    .roundIdentifier(block.getHeader().getRoundIdentifier())
                    .height(block.getHeader().getHeight())
                    .build();
        }

        public ViewChangePayload createViewChangePayload(ConsensusRoundIdentifier roundIdentifier,long height) {
            log.debug("createViewChangePayload roundIdentifier={} height={}", roundIdentifier, height);
            return ViewChangePayload.builder()
                    .roundIdentifier(roundIdentifier)
                    .height(height)
                    .build();
        }


        public ProposePayload createProposePayload(ConsensusRoundIdentifier round, long height, PosBlock block, VRF.Proof proof) {
            return ProposePayload.builder()
                    .roundIdentifier(round)
                    .height(height)
                    .proposedBlock(block)
                    .proof(proof)
                    .build();
        }

        public VotePayload createVotePayload(PosBlock block) {
            return VotePayload.builder()
                    .digest(block.getHash())
                    .roundIdentifier(block.getHeader().getRoundIdentifier())
                    .height(block.getHeader().getHeight())
                    .build();
        }


    }
}
