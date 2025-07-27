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

import org.hyperledger.besu.consensus.common.bft.BftExtraDataCodec;
import org.hyperledger.besu.consensus.common.bft.BftProtocolSchedule;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.blockcreation.BftBlockCreatorFactory;
import org.hyperledger.besu.consensus.common.bft.statemachine.BftFinalState;
import org.hyperledger.besu.consensus.pos.core.NodeSet;
import org.hyperledger.besu.consensus.pos.payload.MessageFactory;
import org.hyperledger.besu.consensus.pos.validation.MessageValidatorFactory;
import org.hyperledger.besu.ethereum.ProtocolContext;
import org.hyperledger.besu.ethereum.blockcreation.BlockCreator;
import org.hyperledger.besu.ethereum.chain.MinedBlockObserver;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.util.Subscribers;

/** The Pos round factory. */
public class PosRoundFactory {

    private final BftFinalState finalState;
    private final BftBlockCreatorFactory<?> blockCreatorFactory;
    private final ProtocolContext protocolContext;
    private final BftProtocolSchedule protocolSchedule;
    private final Subscribers<MinedBlockObserver> minedBlockObservers;
    private final MessageValidatorFactory messageValidatorFactory;
    private final MessageFactory messageFactory;
    private final BftExtraDataCodec bftExtraDataCodec;
    private final ContractCaller contractCaller;
    private final NodeSet nodeSet;

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
            final BftFinalState finalState,
            final ProtocolContext protocolContext,
            final BftProtocolSchedule protocolSchedule,
            final Subscribers<MinedBlockObserver> minedBlockObservers,
            final MessageValidatorFactory messageValidatorFactory,
            final MessageFactory messageFactory,
            final BftExtraDataCodec bftExtraDataCodec, ContractCaller contractCaller, NodeSet nodeSet) {
        this.finalState = finalState;
        this.blockCreatorFactory = finalState.getBlockCreatorFactory();
        this.protocolContext = protocolContext;
        this.protocolSchedule = protocolSchedule;
        this.minedBlockObservers = minedBlockObservers;
        this.messageValidatorFactory = messageValidatorFactory;
        this.messageFactory = messageFactory;
        this.bftExtraDataCodec = bftExtraDataCodec;
        this.contractCaller = contractCaller;
        this.nodeSet = nodeSet;
    }

    /**
     * Create new pos round.
     *
     * @param parentHeader the parent header
     * @param round the round
     * @return the pos round
     */
    public PosRound createNewRound(final BlockHeader parentHeader, final int round) {
        long nextBlockHeight = parentHeader.getNumber() + 1;
        final ConsensusRoundIdentifier roundIdentifier =
                new ConsensusRoundIdentifier(nextBlockHeight, round);

        final RoundState roundState =
                new RoundState(
                        roundIdentifier,
                        finalState.getQuorum(),
                        messageValidatorFactory.createMessageValidator(roundIdentifier, parentHeader));

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
            final BlockHeader parentHeader, final RoundState roundState) {
        final BlockCreator blockCreator =
                blockCreatorFactory.create(roundState.getRoundIdentifier().getRoundNumber());
//
//        final PosMessageTransmitter messageTransmitter =
//                new PosMessageTransmitter(messageFactory, finalState.getValidatorMulticaster());

        return new PosRound(
                roundState,
                blockCreator,
                protocolContext,
                protocolSchedule,
                minedBlockObservers,
                finalState.getNodeKey(),
                messageFactory,
//                messageTransmitter,
                finalState.getRoundTimer(),
                bftExtraDataCodec,
                parentHeader,
                contractCaller,
                nodeSet);
    }
}
