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

import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.events.RoundExpiry;
import org.hyperledger.besu.consensus.pos.messagewrappers.*;
import org.hyperledger.besu.ethereum.core.BlockHeader;

/**
 * The NoOp block height manager.
 *
 * <p>Used when the local node is not a validator.
 * Effectively silences the Consensus State Machine for this node.
 */
public class NoOpBlockHeightManager implements BasePosBlockHeightManager {

    private final BlockHeader parentHeader;

    /**
     * Instantiates a new NoOp block height manager.
     *
     * @param parentHeader the parent header
     */
    public NoOpBlockHeightManager(final BlockHeader parentHeader) {
        this.parentHeader = parentHeader;
    }

    @Override
    public void handleBlockTimerExpiry(final ConsensusRoundIdentifier roundIdentifier) {}

    @Override
    public void roundExpired(final RoundExpiry expire) {}

    @Override
    public long getChainHeight() {
        return parentHeader.getNumber() + 1;
    }

    @Override
    public BlockHeader getParentBlockHeader() {
        return parentHeader;
    }

    // --- No-Op Implementation of Message Handlers ---

    @Override
    public void handleProposalMessage(final Propose msg) {}

    @Override
    public void handleVoteMessage(final Vote msg) {}

    @Override
    public void consumeProposeMessage(final Propose msg) {}

    @Override
    public void consumeVoteMessage(final Vote msg) {}

    /**
     * Returns false to indicate that no consensus messages are valid for processing
     * in this passive state.
     */
    @Override
    public boolean checkValidState(final int msgCode) {
        return false;
    }
}