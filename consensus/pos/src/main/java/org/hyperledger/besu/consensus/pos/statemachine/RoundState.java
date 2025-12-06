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

import lombok.Getter;
import lombok.Setter;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.pos.messagewrappers.*;
import org.hyperledger.besu.crypto.SECPSignature;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.google.common.collect.Sets;
import org.hyperledger.besu.ethereum.core.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Round state defines the current status and messages received for a specific consensus round.
 *
 * <p>Updated for Pure PoS (LCR):
 * - Removed BLS signatures (DSS/ECDSA used instead).
 * - BFT collections (Votes, Commits) are retained for architectural compatibility
 *   but remain unused in the LCR flow (Nakamoto-style).
 */
@Getter
public class RoundState {
    private static final Logger LOG = LoggerFactory.getLogger(RoundState.class);

    private final ConsensusRoundIdentifier roundIdentifier;
    private final long quorum;

    @Setter
    private Propose proposeMessage;

    // LCR Message Storage
    private final Set<Propose> proposeMessages = Sets.newLinkedHashSet();

    // Legacy BFT Message Storage (Retained for Controller compatibility, unused in LCR)
    private final Set<Vote> voteMessages = Sets.newLinkedHashSet();

    @Setter
    private State currentState;

    private final long height;

    /**
     * Instantiates a new Round state.
     *
     * @param roundIdentifier the round identifier
     * @param quorum the quorum (unused in LCR but kept for API compatibility)
     * @param height the block height
     */
    public RoundState(
            final ConsensusRoundIdentifier roundIdentifier,
            final int quorum,
            long height) {
        this.roundIdentifier = roundIdentifier;
        this.quorum = quorum;
        this.height = height;
        // Default start state matching the PosBlockHeightManager logic
        this.currentState = State.PROPOSE;
    }

    public void addProposalMessage(final Propose msg) {
        proposeMessages.add(msg);
        LOG.trace("Round state added Propose message = {}", msg);
    }

    public void addVoteMessage(final Vote msg) {
        voteMessages.add(msg);
        LOG.trace("Round state added vote message = {}", msg);
    }

    /**
     * Returns the seals (signatures) required to finalize the block.
     * In LCR, the block is sealed by the Proposer at creation time.
     *
     * @return Empty list (as seals are already in the block header).
     */
    public Collection<SECPSignature> getCommitSeals() {
        return Collections.emptyList();
    }

    public Block getProposedBlock() {
        return proposeMessage != null ? proposeMessage.getSignedPayload().getPayload().getProposedBlock() : null;
    }
}