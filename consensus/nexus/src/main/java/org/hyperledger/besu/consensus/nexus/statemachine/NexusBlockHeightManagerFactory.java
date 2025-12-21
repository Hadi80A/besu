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

import org.hyperledger.besu.config.NexusConfigOptions;
import org.hyperledger.besu.consensus.nexus.bls.Bls;
import org.hyperledger.besu.consensus.nexus.core.NexusBlockHeader;
import org.hyperledger.besu.consensus.nexus.core.NexusFinalState;
import org.hyperledger.besu.consensus.nexus.metrics.NexusMetricCalculator;
import org.hyperledger.besu.consensus.nexus.network.NexusMessageTransmitter;
import org.hyperledger.besu.consensus.nexus.validation.MessageValidatorFactory;

import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.eth.manager.EthPeers;
import org.hyperledger.besu.ethereum.eth.sync.state.SyncState;
import org.hyperledger.besu.ethereum.eth.transactions.TransactionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The Nexus block height manager factory. */
public class NexusBlockHeightManagerFactory {

    private static final Logger LOG = LoggerFactory.getLogger(NexusBlockHeightManagerFactory.class);

    private final NexusRoundFactory roundFactory;
    private final TransactionPool transactionPool;
    private final NexusFinalState finalState;
    private final MessageValidatorFactory messageValidatorFactory;
    private final NexusConfigOptions posConfig;
    private final NexusRoundFactory.MessageFactory messageFactory;
    private final NexusProposerSelector posProposerSelector;
    private final Bls.KeyPair blsKeyPair;
    private final EthPeers ethPeers;
    private final SyncState syncState;
    /**
     * Instantiates a new Nexus block height manager factory.
     *
     * @param finalState              the final state
     * @param roundFactory            the round factory
     * @param messageValidatorFactory the message validator factory
     * @param messageFactory          the message factory
     * @param blsKeyPair
     */
    public NexusBlockHeightManagerFactory(
            final NexusFinalState finalState,
            final NexusRoundFactory roundFactory, TransactionPool transactionPool,
            final MessageValidatorFactory messageValidatorFactory, NexusConfigOptions posConfig,
            final NexusRoundFactory.MessageFactory messageFactory, NexusProposerSelector posProposerSelector, EthPeers ethPeers, SyncState syncState, Bls.KeyPair blsKeyPair) {
        this.roundFactory = roundFactory;
        this.finalState = finalState;
        this.transactionPool = transactionPool;
        this.messageValidatorFactory = messageValidatorFactory;
        this.posConfig = posConfig;
        this.messageFactory = messageFactory;
        this.posProposerSelector = posProposerSelector;
        this.ethPeers = ethPeers;
        this.syncState = syncState;
        this.blsKeyPair = blsKeyPair;
    }

    /**
     * Create base nexus block height manager.
     *
     * @param parentHeader the parent header
     * @return the base nexus block height manager
     */
    public BaseNexusBlockHeightManager create(final NexusBlockHeader parentHeader, Blockchain blockchain) {
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
    protected BaseNexusBlockHeightManager createNoOpBlockHeightManager(
            final NexusBlockHeader parentHeader) {
        return new NoOpBlockHeightManager(parentHeader);
    }

    private BaseNexusBlockHeightManager createFullBlockHeightManager(final NexusBlockHeader parentHeader,Blockchain blockchain) {
        return new NexusBlockHeightManager(
                transactionPool,
                parentHeader,
                finalState,
                roundFactory,
                finalState.getClock(),
                messageFactory,
                posProposerSelector,
                new NexusMessageTransmitter(messageFactory, finalState.getValidatorMulticaster(),finalState.getLocalAddress()),
                posConfig,
                blockchain,
                ethPeers,
                syncState,
                blsKeyPair
                );
    }
}
