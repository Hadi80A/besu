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

import org.hyperledger.besu.consensus.common.bft.BftHelpers;
import org.hyperledger.besu.consensus.common.bft.statemachine.BftFinalState;
import org.hyperledger.besu.consensus.pos.payload.MessageFactory;
import org.hyperledger.besu.consensus.pos.validation.MessageValidatorFactory;
import org.hyperledger.besu.ethereum.core.BlockHeader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The Pos block height manager factory. */
public class PosBlockHeightManagerFactory {

    private static final Logger LOG = LoggerFactory.getLogger(PosBlockHeightManagerFactory.class);

    private final PosRoundFactory roundFactory;
    private final BftFinalState finalState;
    private final MessageValidatorFactory messageValidatorFactory;
    private final MessageFactory messageFactory;

    /**
     * Instantiates a new Pos block height manager factory.
     *
     * @param finalState the final state
     * @param roundFactory the round factory
     * @param messageValidatorFactory the message validator factory
     * @param messageFactory the message factory
     */
    public PosBlockHeightManagerFactory(
            final BftFinalState finalState,
            final PosRoundFactory roundFactory,
            final MessageValidatorFactory messageValidatorFactory,
            final MessageFactory messageFactory) {
        this.roundFactory = roundFactory;
        this.finalState = finalState;
        this.messageValidatorFactory = messageValidatorFactory;
        this.messageFactory = messageFactory;
    }

    /**
     * Create base pos block height manager.
     *
     * @param parentHeader the parent header
     * @return the base pos block height manager
     */
    public BasePosBlockHeightManager create(final BlockHeader parentHeader) {
        if (finalState.isLocalNodeValidator()) {
            LOG.debug("Local node is a validator");
            return createFullBlockHeightManager(parentHeader);
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
            final BlockHeader parentHeader) {
        return new NoOpBlockHeightManager(parentHeader);
    }

    private BasePosBlockHeightManager createFullBlockHeightManager(final BlockHeader parentHeader) {
        return new PosBlockHeightManager(
                parentHeader,
                finalState,
//                new RoundChangeManager(
//                        BftHelpers.calculateRequiredValidatorQuorum(finalState.getValidators().size()),
//                        messageValidatorFactory.createRoundChangeMessageValidator(
//                                parentHeader.getNumber() + 1L, parentHeader)),
                roundFactory,
                finalState.getClock(),
                messageValidatorFactory,
                messageFactory);
    }
}
