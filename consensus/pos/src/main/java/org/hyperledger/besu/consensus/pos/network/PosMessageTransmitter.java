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
import org.hyperledger.besu.plugin.services.securitymodule.SecurityModuleException;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The Pos message transmitter. */
public class PosMessageTransmitter {

  private static final Logger LOG = LoggerFactory.getLogger(PosMessageTransmitter.class);

  private final PosRoundFactory.MessageFactory messageFactory;
  private final ValidatorMulticaster multicaster;
  private final Address localAddress;
  /**
   * Instantiates a new Pos message transmitter.
   *
   * @param messageFactory the message factory
   * @param multicaster the multicaster
   */
  public PosMessageTransmitter(
          final PosRoundFactory.MessageFactory messageFactory, final ValidatorMulticaster multicaster, Address localAddress) {
    this.messageFactory = messageFactory;
    this.multicaster = multicaster;
    this.localAddress = localAddress;
  }

  public void multicastProposal(Propose propose) {
    try {

      final ProposalMessageData message = ProposalMessageData.create(propose);
      LOG.debug("multicastProposal: {}", message);
      multicaster.send(message, Collections.singletonList(localAddress));


    } catch (final SecurityModuleException e) {
      LOG.warn("Failed to generate signature for Proposal (not sent): {} ", e.getMessage());
    }
  }

  public void multicastSelectLeader(SelectLeader selectLeader) {
    try {

      final SelectLeaderMessageData message = SelectLeaderMessageData.create(selectLeader);
      LOG.debug("multicastSelectLeader: {}", message);
      multicaster.send(message, Collections.singletonList(localAddress));
    } catch (final SecurityModuleException e) {
      LOG.warn("Failed to generate signature for select leader (not sent): {} ", e.getMessage());
    }
  }

  public void multicastVote(Vote vote) {
    try {
      final VoteMessageData message = VoteMessageData.create(vote);
      multicaster.send(message , Collections.singletonList(localAddress));
    } catch (final SecurityModuleException e) {
      LOG.warn("Failed to generate signature for Prepare (not sent): {} ", e.getMessage());
    }
  }

  public void multicastCommit(Commit commit) {
    try {
      final CommitMessageData message = CommitMessageData.create(commit);
      multicaster.send(message, Collections.singletonList(localAddress));
    } catch (final SecurityModuleException e) {
      LOG.warn("Failed to generate signature for Commit (not sent): {} ", e.getMessage());
    }
  }


  public void multicastRoundChange(ViewChange viewChange) {
    try {

      final ViewChangeMessageData message = ViewChangeMessageData.create(viewChange);

      multicaster.send(message, Collections.singletonList(localAddress));
      LOG.debug("multicastRoundChange  AND SEND: {}", message);
    } catch (final SecurityModuleException e) {
      LOG.warn("Failed to generate signature for RoundChange (not sent): {} ", e.getMessage());
    }
  }
}
