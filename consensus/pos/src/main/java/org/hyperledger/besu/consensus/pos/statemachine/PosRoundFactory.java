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
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.config.PosConfigOptions;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.payload.SignedData;
import org.hyperledger.besu.consensus.pos.PosBlockCreator;
import org.hyperledger.besu.consensus.pos.PosBlockCreatorFactory;
import org.hyperledger.besu.consensus.pos.PosExtraDataCodec;
import org.hyperledger.besu.consensus.pos.PosProtocolSchedule;
import org.hyperledger.besu.consensus.pos.core.*;
import org.hyperledger.besu.consensus.pos.messagewrappers.*;
import org.hyperledger.besu.consensus.pos.network.PosMessageTransmitter;
import org.hyperledger.besu.consensus.pos.payload.*;
import org.hyperledger.besu.consensus.pos.validation.MessageValidatorFactory;
import org.hyperledger.besu.crypto.SECPPublicKey;
import org.hyperledger.besu.ethereum.ProtocolContext;
import org.hyperledger.besu.ethereum.core.Block;
import org.hyperledger.besu.ethereum.core.BlockHeader;
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
    private final PosExtraDataCodec posExtraDataCodec;
    private final ContractCaller contractCaller;
    private final NodeSet nodeSet;
    private final PosProposerSelector proposerSelector;

    /**
     * Instantiates a new Pos round factory.
     */
    public PosRoundFactory(
            final PosFinalState finalState,
            final ProtocolContext protocolContext,
            final PosProtocolSchedule protocolSchedule, PosConfigOptions configOptions,
            final Subscribers<PosMinedBlockObserver> minedBlockObservers,
            final MessageValidatorFactory messageValidatorFactory,
            final MessageFactory messageFactory,
            final PosExtraDataCodec posExtraDataCodec,
            ContractCaller contractCaller,
            NodeSet nodeSet,
            PosProposerSelector proposerSelector) {
        this.finalState = finalState;
        this.blockCreatorFactory = finalState.getBlockCreatorFactory();
        this.protocolContext = protocolContext;
        this.protocolSchedule = protocolSchedule;
        this.configOptions = configOptions;
        this.minedBlockObservers = minedBlockObservers;
        this.messageValidatorFactory = messageValidatorFactory;
        this.messageFactory = messageFactory;
        this.posExtraDataCodec = posExtraDataCodec;
        this.contractCaller = contractCaller;
        this.nodeSet = nodeSet;
        this.proposerSelector = proposerSelector;
    }

    /**
     * Create new pos round.
     */
    public PosRound createNewRound(final BlockHeader parentHeader, final int round) {
        long nextBlockHeight = parentHeader.getNumber() + 1;
        final ConsensusRoundIdentifier roundIdentifier =
                new ConsensusRoundIdentifier(nextBlockHeight, round);

        final RoundState roundState =
                new RoundState(
                        roundIdentifier,
                        (int) finalState.getQuorum(),
                        nextBlockHeight
                );

        return createNewRoundWithState(parentHeader, roundState);
    }


    public PosRound createNewRoundWithState(
            final BlockHeader parentHeader, final RoundState roundState) {

        final int roundNumber = roundState.getRoundIdentifier().getRoundNumber();

        // Retrieve seed for FTS (Phase 3)
        // Note: Logic for seed retrieval may depend on whether it's round 0 or a future round.
        Bytes32 seed = proposerSelector.getSeedAtRound(
                roundNumber,
                parentHeader.getHash());

        final PosBlockCreator blockCreator = blockCreatorFactory.create(roundNumber, seed);

        final PosMessageTransmitter messageTransmitter =
                new PosMessageTransmitter(messageFactory, finalState.getValidatorMulticaster(), finalState.getLocalAddress());

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
                posExtraDataCodec,
                parentHeader,
                contractCaller,
                nodeSet,
                proposerSelector,
                finalState
        );
    }

    public static class MessageFactory {

        public Propose createPropose(SignedData<ProposePayload> payload) {
            return new Propose(payload);
        }
        public Vote createVote(SignedData<VotePayload> payload) {
            return new Vote(payload);
        }


        // Updated for LCR: Removed VRF Proof
        public ProposePayload createProposePayload( Block block,ConsensusRoundIdentifier roundIdentifier) {
            return ProposePayload.builder()
                    .proposedBlock(block)
                    .roundIdentifier(roundIdentifier)
                    .build();
        }

        // Updated for LCR: Removed BLS Signature
        public VotePayload createVotePayload(Block block,ConsensusRoundIdentifier roundIdentifier) {
            return VotePayload.builder()
                    .digest(block.getHash())
                    .roundIdentifier(roundIdentifier)
                    // BLS Signature removed
                    .build();
        }
    }
}