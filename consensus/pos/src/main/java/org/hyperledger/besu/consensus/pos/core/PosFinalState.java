/*
 * Copyright contributors to Besu.
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
package org.hyperledger.besu.consensus.pos.core;

import lombok.Getter;
import org.hyperledger.besu.consensus.common.bft.BftHelpers;
import org.hyperledger.besu.consensus.common.bft.BlockTimer;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.RoundTimer;
import org.hyperledger.besu.consensus.common.bft.blockcreation.ProposerSelector;
import org.hyperledger.besu.consensus.common.bft.network.ValidatorMulticaster;
import org.hyperledger.besu.consensus.common.bft.statemachine.BftFinalState;
import org.hyperledger.besu.consensus.common.validator.ValidatorProvider;
import org.hyperledger.besu.consensus.pos.PosBlockCreatorFactory;
import org.hyperledger.besu.cryptoservices.NodeKey;
import org.hyperledger.besu.datatypes.Address;

import java.time.Clock;
import java.util.Collection;

/**
 * Holds the static/final state and configuration for the PoS consensus engine.
 * Shared across different rounds and block height managers.
 */
@Getter
public class PosFinalState {

    private final ValidatorProvider validatorProvider;
    private final NodeKey nodeKey;
    private final Address localAddress;
    private final ProposerSelector proposerSelector;
    private final ValidatorMulticaster validatorMulticaster;
    private final RoundTimer roundTimer;
    private final BlockTimer blockTimer;
    private final PosBlockCreatorFactory blockCreatorFactory;
    private final Clock clock;
    private final BftFinalState bftFinalState;

    /**
     * Constructs a new POS final state.
     *
     * @param validatorProvider the validator provider
     * @param nodeKey the node key
     * @param localAddress the local address
     * @param proposerSelector the generic proposer selector (interface)
     * @param validatorMulticaster the validator multicaster
     * @param roundTimer the round timer
     * @param blockTimer the block timer
     * @param blockCreatorFactory the block creator factory
     * @param clock the clock
     * @param bftFinalState the underlying BFT final state wrapper
     */
    public PosFinalState(
            final ValidatorProvider validatorProvider,
            final NodeKey nodeKey,
            final Address localAddress,
            final ProposerSelector proposerSelector,
            final ValidatorMulticaster validatorMulticaster,
            final RoundTimer roundTimer,
            final BlockTimer blockTimer,
            final PosBlockCreatorFactory blockCreatorFactory,
            final Clock clock,
            final BftFinalState bftFinalState) {
        this.validatorProvider = validatorProvider;
        this.nodeKey = nodeKey;
        this.localAddress = localAddress;
        this.proposerSelector = proposerSelector;
        this.validatorMulticaster = validatorMulticaster;
        this.roundTimer = roundTimer;
        this.blockTimer = blockTimer;
        this.blockCreatorFactory = blockCreatorFactory;
        this.clock = clock;
        this.bftFinalState = bftFinalState;
    }

    /**
     * Gets the current set of validators.
     *
     * @return the validators
     */
    public Collection<Address> getValidators() {
        return validatorProvider.getValidatorsAtHead();
    }

    /**
     * Checks if the local node is a validator.
     *
     * @return true if local node is in the validator set
     */
    public boolean isLocalNodeValidator() {
        return getValidators().contains(localAddress);
    }

    /**
     * Calculates the quorum required for Round Change (ViewChange).
     * Typically 2/3 of the validator count.
     *
     * @return the quorum size
     */
    public int getQuorum() {
        return BftHelpers.calculateRequiredValidatorQuorum(getValidators().size());
    }

    /**
     * Checks if local node is the proposer for the given round (using generic selector).
     *
     * @param roundIdentifier the round identifier
     * @return true if local node is proposer
     */
    public boolean isLocalNodeProposerForRound(final ConsensusRoundIdentifier roundIdentifier) {
        return getProposerForRound(roundIdentifier).equals(localAddress);
    }

    /**
     * Gets proposer for a specific round (using generic selector).
     *
     * @param roundIdentifier the round identifier
     * @return the proposer for round
     */
    public Address getProposerForRound(final ConsensusRoundIdentifier roundIdentifier) {
        return proposerSelector.selectProposerForRound(roundIdentifier);
    }
}