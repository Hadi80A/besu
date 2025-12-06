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
package org.hyperledger.besu.consensus.pos.network;

import org.hyperledger.besu.consensus.common.bft.network.ValidatorMulticaster;
import org.hyperledger.besu.consensus.pos.messagedata.*;
import org.hyperledger.besu.consensus.pos.messagewrappers.*;
import org.hyperledger.besu.consensus.pos.statemachine.PosRoundFactory;
import org.hyperledger.besu.datatypes.Address;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Pos message transmitter.
 *
 * <p>Phase 5: Propagation
 * Responsible for multicasting consensus messages to the active validator set.
 * Ensures the sender (local node) is excluded from the transmission list.
 */
public class PosMessageTransmitter {

    private static final Logger LOG = LoggerFactory.getLogger(PosMessageTransmitter.class);

    private final ValidatorMulticaster multicaster;
    private final Address localAddress;

    /**
     * Instantiates a new Pos message transmitter.
     *
     * @param messageFactory the message factory (unused in transmitter but kept for API compatibility)
     * @param multicaster the multicaster
     * @param localAddress the local address
     */
    public PosMessageTransmitter(
            final PosRoundFactory.MessageFactory messageFactory,
            final ValidatorMulticaster multicaster,
            final Address localAddress) {
        this.multicaster = multicaster;
        this.localAddress = localAddress;
    }

    public void multicastProposal(final Propose propose) {
        final ProposalMessageData message = ProposalMessageData.create(propose);
        LOG.debug("Multicasting Proposal: Round={}, Author={}",
                propose.getRoundIdentifier(), propose.getAuthor());
        multicaster.send(message, getExcludedAddresses());
    }

    public void multicastVote(final Vote vote) {
        final VoteMessageData message = VoteMessageData.create(vote);
        LOG.trace("Multicasting Vote");
        multicaster.send(message, getExcludedAddresses());
    }

    private List<Address> getExcludedAddresses() {
        return Collections.singletonList(localAddress);
    }
}