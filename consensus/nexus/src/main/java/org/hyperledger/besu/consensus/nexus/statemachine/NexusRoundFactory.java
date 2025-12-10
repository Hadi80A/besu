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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hyperledger.besu.config.NexusConfigOptions;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.payload.SignedData;
import org.hyperledger.besu.consensus.nexus.NexusBlockCreator;
import org.hyperledger.besu.consensus.nexus.NexusBlockCreatorFactory;
import org.hyperledger.besu.consensus.nexus.NexusExtraDataCodec;
import org.hyperledger.besu.consensus.nexus.NexusProtocolSchedule;
import org.hyperledger.besu.consensus.nexus.bls.Bls;
import org.hyperledger.besu.consensus.nexus.core.*;
import org.hyperledger.besu.consensus.nexus.messagewrappers.*;
import org.hyperledger.besu.consensus.nexus.network.NexusMessageTransmitter;
import org.hyperledger.besu.consensus.nexus.payload.*;
import org.hyperledger.besu.consensus.nexus.validation.MessageValidatorFactory;
import org.hyperledger.besu.consensus.nexus.vrf.VRF;
import org.hyperledger.besu.crypto.SECPPublicKey;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.ProtocolContext;
import org.hyperledger.besu.util.Subscribers;

/** The Nexus round factory. */
public class NexusRoundFactory {

    private static final Logger log = LogManager.getLogger(NexusRoundFactory.class);
    private final NexusFinalState finalState;
    private final NexusBlockCreatorFactory blockCreatorFactory;
    private final ProtocolContext protocolContext;
    private final NexusProtocolSchedule protocolSchedule;
    private final NexusConfigOptions configOptions;
    private final Subscribers<NexusMinedBlockObserver> minedBlockObservers;
    private final MessageValidatorFactory messageValidatorFactory;
    private final MessageFactory messageFactory;
    private final NexusExtraDataCodec posExtraDataCodec;
    private final ContractCaller contractCaller;
    private final NodeSet nodeSet;
    private final NexusProposerSelector proposerSelector;

    /**
     * Instantiates a new Nexus round factory.
     *
     * @param finalState the final state
     * @param protocolContext the protocol context
     * @param protocolSchedule the protocol schedule
     * @param minedBlockObservers the mined block observers
     * @param messageValidatorFactory the message validator factory
     * @param messageFactory the message factory
     * @param posExtraDataCodec the bft extra data codec
     */
    public NexusRoundFactory(
            final NexusFinalState finalState,
            final ProtocolContext protocolContext,
            final NexusProtocolSchedule protocolSchedule, NexusConfigOptions configOptions,
            final Subscribers<NexusMinedBlockObserver> minedBlockObservers,
            final MessageValidatorFactory messageValidatorFactory,
            final MessageFactory messageFactory,
            final NexusExtraDataCodec posExtraDataCodec, ContractCaller contractCaller, NodeSet nodeSet, NexusProposerSelector proposerSelector) {
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
     * Create new nexus round.
     *
     * @param parentHeader the parent header
     * @param round the round
     * @return the nexus round
     */
    public NexusRound createNewRound(final NexusBlockHeader parentHeader, final int round) {
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
     * Create new Nexus round with state.
     *
     * @param parentHeader the parent header
     * @param roundState the round state
     * @return the nexus round
     */
    public NexusRound createNewRoundWithState(
            final NexusBlockHeader parentHeader, final RoundState roundState) {
        final NexusBlockCreator blockCreator =
                blockCreatorFactory.create(roundState.getRoundIdentifier().getRoundNumber());
//
        final NexusMessageTransmitter messageTransmitter =
                new NexusMessageTransmitter(messageFactory, finalState.getValidatorMulticaster(),finalState.getLocalAddress());

        return new NexusRound(
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
        public BlockAnnounce createBlockAnnounce(SignedData<BlockAnnouncePayload> payload) {
            return new BlockAnnounce(payload);
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

        public BlockAnnouncePayload createBlockAnnouncePayload(ConsensusRoundIdentifier round, long height,QuorumCertificate quorumCertificate) {
            return BlockAnnouncePayload.builder()
                    .roundIdentifier(round)
                    .height(height)
                    .quorumCertificate(quorumCertificate)
                    .build();
        }
        
        public CommitPayload createCommitPayload(NexusBlock block, QuorumCertificate quorumCertificate) {
            return CommitPayload.builder()
                    .roundIdentifier(block.getHeader().getRoundIdentifier())
                    .height(block.getHeader().getHeight())
                    .digest(block.getHash())
                    .quorumCertificate(quorumCertificate)
                    .build();
        }

        public ViewChangePayload createViewChangePayload(ConsensusRoundIdentifier roundIdentifier,long height) {
            log.debug("createViewChangePayload roundIdentifier={} height={}", roundIdentifier, height);
            return ViewChangePayload.builder()
                    .roundIdentifier(roundIdentifier)
                    .height(height)
                    .build();
        }


        public ProposePayload createProposePayload(ConsensusRoundIdentifier round, long height, NexusBlock block, VRF.Proof proof) {
            return ProposePayload.builder()
                    .roundIdentifier(round)
                    .height(height)
                    .proposedBlock(block)
                    .proof(proof)
                    .build();
        }

        public VotePayload createVotePayload(NexusBlock block, Bls.Signature blsSignature) {
            return VotePayload.builder()
                    .digest(block.getHash())
                    .roundIdentifier(block.getHeader().getRoundIdentifier())
                    .height(block.getHeader().getHeight())
                    .signature(blsSignature)
                    .build();
        }


    }
}
