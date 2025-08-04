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

import org.hyperledger.besu.consensus.pos.core.PosBlockHeader;
import org.hyperledger.besu.consensus.pos.core.PosFinalState;
import org.hyperledger.besu.consensus.pos.network.PosMessageTransmitter;
import org.hyperledger.besu.consensus.pos.validation.MessageValidatorFactory;

import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The Pos block height manager factory. */
public class PosBlockHeightManagerFactory {

    private static final Logger LOG = LoggerFactory.getLogger(PosBlockHeightManagerFactory.class);

    private final PosRoundFactory roundFactory;
    private final PosFinalState finalState;
    private final MessageValidatorFactory messageValidatorFactory;
    private final PosRoundFactory.MessageFactory messageFactory;
    private final PosProposerSelector posProposerSelector;

    /**
     * Instantiates a new Pos block height manager factory.
     *
     * @param finalState the final state
     * @param roundFactory the round factory
     * @param messageValidatorFactory the message validator factory
     * @param messageFactory the message factory
     */
    public PosBlockHeightManagerFactory(
            final PosFinalState finalState,
            final PosRoundFactory roundFactory,
            final MessageValidatorFactory messageValidatorFactory,
            final PosRoundFactory.MessageFactory messageFactory, PosProposerSelector posProposerSelector) {
        this.roundFactory = roundFactory;
        this.finalState = finalState;
        this.messageValidatorFactory = messageValidatorFactory;
        this.messageFactory = messageFactory;
        this.posProposerSelector = posProposerSelector;
    }

    /**
     * Create base pos block height manager.
     *
     * @param parentHeader the parent header
     * @return the base pos block height manager
     */
    public BasePosBlockHeightManager create(final PosBlockHeader parentHeader, Blockchain blockchain) {
        if (finalState.isLocalNodeValidator()) {
            LOG.debug("Local node is a validator");
            return createFullBlockHeightManager(parentHeader,blockchain);
        } else {
            LOG.debug("Local node is a non-validator");
            return createNoOpBlockHeightManager(parentHeader);
        }
    }

    /**
     * Create a no-op block height manager.
     *
     * @param parentHeader the parent header
     * @return the no-op height manager
     */
    protected BasePosBlockHeightManager createNoOpBlockHeightManager(
            final PosBlockHeader parentHeader) {
        return new NoOpBlockHeightManager(parentHeader);
    }

    private BasePosBlockHeightManager createFullBlockHeightManager(final PosBlockHeader parentHeader,Blockchain blockchain) {
        return new PosBlockHeightManager(
                parentHeader,
                finalState,
                roundFactory,
                finalState.getClock(),
                messageFactory,
                posProposerSelector,
                new PosMessageTransmitter(messageFactory, finalState.getValidatorMulticaster()),
                blockchain
        );
    }
}
