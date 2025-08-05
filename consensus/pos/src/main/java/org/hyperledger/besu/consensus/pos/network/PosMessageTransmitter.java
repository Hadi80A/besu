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

import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.network.ValidatorMulticaster;
import org.hyperledger.besu.consensus.common.bft.payload.SignedData;
import org.hyperledger.besu.consensus.pos.core.PosBlock;
import org.hyperledger.besu.consensus.pos.messagedata.CommitMessageData;
import org.hyperledger.besu.consensus.pos.messagedata.ProposalMessageData;
import org.hyperledger.besu.consensus.pos.messagedata.ViewChangeMessageData;
import org.hyperledger.besu.consensus.pos.messagedata.VoteMessageData;
import org.hyperledger.besu.consensus.pos.messagewrappers.Commit;
import org.hyperledger.besu.consensus.pos.messagewrappers.Propose;
import org.hyperledger.besu.consensus.pos.messagewrappers.ViewChange;
import org.hyperledger.besu.consensus.pos.messagewrappers.Vote;
import org.hyperledger.besu.consensus.pos.payload.CommitPayload;
import org.hyperledger.besu.consensus.pos.payload.ViewChangePayload;
import org.hyperledger.besu.consensus.pos.payload.VotePayload;
import org.hyperledger.besu.consensus.pos.statemachine.PosRoundFactory;
import org.hyperledger.besu.crypto.SECPSignature;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.plugin.services.securitymodule.SecurityModuleException;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The Pos message transmitter. */
public class PosMessageTransmitter {

  private static final Logger LOG = LoggerFactory.getLogger(PosMessageTransmitter.class);

  private final PosRoundFactory.MessageFactory messageFactory;
  private final ValidatorMulticaster multicaster;

  /**
   * Instantiates a new Pos message transmitter.
   *
   * @param messageFactory the message factory
   * @param multicaster the multicaster
   */
  public PosMessageTransmitter(
          final PosRoundFactory.MessageFactory messageFactory, final ValidatorMulticaster multicaster) {
    this.messageFactory = messageFactory;
    this.multicaster = multicaster;
  }

  public void multicastProposal(Propose propose) {
    try {

      final ProposalMessageData message = ProposalMessageData.create(propose);
      LOG.debug("multicastProposal: {}", message);
      multicaster.send(message);
    } catch (final SecurityModuleException e) {
      LOG.warn("Failed to generate signature for Proposal (not sent): {} ", e.getMessage());
    }
  }

  public void multicastVote(Vote vote) {
    try {
      final VoteMessageData message = VoteMessageData.create(vote);

      multicaster.send(message);
    } catch (final SecurityModuleException e) {
      LOG.warn("Failed to generate signature for Prepare (not sent): {} ", e.getMessage());
    }
  }

  public void multicastCommit(Commit commit) {
    try {

      final CommitMessageData message = CommitMessageData.create(commit);

      multicaster.send(message);
    } catch (final SecurityModuleException e) {
      LOG.warn("Failed to generate signature for Commit (not sent): {} ", e.getMessage());
    }
  }


  public void multicastRoundChange(ViewChange viewChange) {
    try {

      final ViewChangeMessageData message = ViewChangeMessageData.create(viewChange);

      multicaster.send(message);
    } catch (final SecurityModuleException e) {
      LOG.warn("Failed to generate signature for RoundChange (not sent): {} ", e.getMessage());
    }
  }
}
