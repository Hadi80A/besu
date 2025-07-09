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
package org.hyperledger.besu.consensus.pactus.network;

import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.network.ValidatorMulticaster;
import org.hyperledger.besu.consensus.pactus.factory.PactusRoundFactory;
import org.hyperledger.besu.consensus.pactus.messagedata.CommitMessageData;
import org.hyperledger.besu.consensus.pactus.messagedata.PreCommitMessageData;
import org.hyperledger.besu.consensus.pactus.messagedata.PrepareMessageData;
import org.hyperledger.besu.consensus.pactus.messagedata.ProposalMessageData;
import org.hyperledger.besu.consensus.pactus.messagewrappers.*;
import org.hyperledger.besu.crypto.SECPSignature;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.plugin.services.securitymodule.SecurityModuleException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The Pactus message transmitter. */
public class PactusMessageTransmitter {

  private static final Logger LOG = LoggerFactory.getLogger(PactusMessageTransmitter.class);

  private final PactusRoundFactory.MessageFactory messageFactory;
  private final ValidatorMulticaster multicaster;

  /**
   * Instantiates a new Pactus message transmitter.
   *
   * @param messageFactory the message factory
   * @param multicaster the multicaster
   */
  public PactusMessageTransmitter(
          final PactusRoundFactory.MessageFactory messageFactory, final ValidatorMulticaster multicaster) {
    this.messageFactory = messageFactory;
    this.multicaster = multicaster;
  }

  /**
   * Multicast proposal
   */
  public void multicastProposal(final Proposal propose) {
    try {
      final ProposalMessageData message = ProposalMessageData.create(propose);
      multicaster.send(message);
    } catch (final SecurityModuleException e) {
      LOG.warn("Failed to generate signature for Proposal (not sent): {} ", e.getMessage());
    }
  }

  public void multicastPrepare(Prepare prepare) {
    try {
//      final Prepare data = messageFactory.createPrepare(roundIdentifier, digest);
      final PrepareMessageData message = PrepareMessageData.create(prepare);
      multicaster.send(message);
    } catch (final SecurityModuleException e) {
      LOG.warn("Failed to generate signature for Prepare (not sent): {} ", e.getMessage());
    }
  }
  public void multicastPreCommit(PreCommit preCommit) {
    try {
//      final Prepare data = messageFactory.createPrepare(roundIdentifier, digest);
      final PreCommitMessageData message =PreCommitMessageData.create(preCommit);
      multicaster.send(message);
    } catch (final SecurityModuleException e) {
      LOG.warn("Failed to generate signature for PreCommit (not sent): {} ", e.getMessage());
    }
  }

  public void multicastCommit(Commit commit) {
    try {
      final CommitMessageData message =CommitMessageData.create(commit);
      multicaster.send(message);
    } catch (final SecurityModuleException e) {
      LOG.warn("Failed to generate signature for Commit (not sent): {} ", e.getMessage());
    }
  }

  public void multicastRoundChange(
          final ConsensusRoundIdentifier roundIdentifier) {
    try {
      final RoundChange data =
              messageFactory.createRoundChange(roundIdentifier);

      final RoundChangeMessageData message = RoundChangeMessageData.create(data);

      multicaster.send(message);
    } catch (final SecurityModuleException e) {
      LOG.warn("Failed to generate signature for RoundChange (not sent): {} ", e.getMessage());
    }
  }
}

//public class PactusMessageTransmitter {
//
//  private final Network network;
//
//  public PactusMessageTransmitter(final Network network) {
//    this.network = network;
//  }
//
//  /**
//   * Broadcast a proposal to all peers.
//   */
//  public void multicastProposal(final Proposal proposal) {
//    network.broadcast("pactus/proposal", proposal);
//  }
//
//  /**
//   * Broadcast a pre-commit message to all peers.
//   */
//  public void multicastPreCommit(final PreCommit preCommit) {
//    network.broadcast("pactus/precommit", preCommit);
//  }
//
//  /**
//   * Broadcast a commit message to all peers.
//   */
//  public void multicastCommit(final Commit commit) {
//    network.broadcast("pactus/commit", commit);
//  }
//
//  /**
//   * Broadcast a final certificate to all peers.
//   */
//  public void multicastCertificate(final Certificate certificate) {
//    network.broadcast("pactus/certificate", certificate);
//  }
//
//  /**
//   * Send a proposal message to a specific peer.
//   */
//  public void unicastProposal(final Peer peer, final Proposal proposal) {
//    network.send(peer, "pactus/proposal", proposal);
//  }
//
//  /**
//   * Send a commit message to a specific peer.
//   */
//  public void unicastCommit(final Peer peer, final Commit commit) {
//    network.send(peer, "pactus/commit", commit);
//  }
//
//  /**
//   * Send a pre-commit message to a specific peer.
//   */
//  public void unicastPreCommit(final Peer peer, final PreCommit preCommit) {
//    network.send(peer, "pactus/precommit", preCommit);
//  }
//
//  /**
//   * Send a certificate message to a specific peer.
//   */
//  public void unicastCertificate(final Peer peer, final Certificate certificate) {
//    network.send(peer, "pactus/certificate", certificate);
//  }
//
//  /**
//   * Send a batch of proposals or consensus messages to a group of peers.
//   */
//  public void multicastToGroup(Collection<Peer> peers, String messageType, Object message) {
//    for (Peer peer : peers) {
//      network.send(peer, messageType, message);
//    }
//  }
//}
