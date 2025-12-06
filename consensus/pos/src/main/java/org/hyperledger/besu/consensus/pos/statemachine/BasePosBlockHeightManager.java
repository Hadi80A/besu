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

import org.hyperledger.besu.consensus.common.bft.statemachine.BaseBlockHeightManager;
import org.hyperledger.besu.consensus.pos.messagewrappers.*;

/**
 * The interface Base pos block height manager.
 * Defines the contract for processing consensus messages at a specific block height.
 *
 * <p>In the context of Pure PoS (LCR + DSS), BFT-specific phases (Vote, Commit)
 * are not functionally required but are retained here for architectural compatibility.
 */
public interface BasePosBlockHeightManager extends BaseBlockHeightManager {

    // --- Phase 4 & 5: Proposal Handling (Active in LCR) ---

    /**
     * Process a Proposal message that has been verified for the current round.
     */
    void handleProposalMessage(final Propose msg);

    /**
     * Consume a raw Proposal message from the network.
     */
    void consumeProposeMessage(final Propose msg);

    // --- Phase 2: Round/Time Slot Management (Active in LCR) ---



    // --- Legacy BFT / VRF Methods (No-Ops in LCR/FTS) ---
    // These are kept to satisfy the PosController's message dispatching logic.

    void handleVoteMessage(final Vote msg);

    void consumeVoteMessage(final Vote msg);

    /**
     * Checks if the message code is valid for the current state.
     * @param msgCode the message code
     * @return true if valid
     */
    boolean checkValidState(int msgCode);
}