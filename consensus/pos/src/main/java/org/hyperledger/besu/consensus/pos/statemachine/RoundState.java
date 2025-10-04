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
import org.hyperledger.besu.consensus.pos.bls.Bls;
import org.hyperledger.besu.consensus.pos.messagedata.PosMessage;
import org.hyperledger.besu.consensus.pos.messagewrappers.*;
import org.hyperledger.besu.consensus.pos.core.PosBlock;
import org.hyperledger.besu.crypto.SECPSignature;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The Round state defines how a round will operate. */
// Data items used to define how a round will operate
@Getter
public class RoundState {
  private static final Logger LOG = LoggerFactory.getLogger(RoundState.class);

  private final ConsensusRoundIdentifier roundIdentifier;
  private final long quorum;

  @Setter
  private Propose proposeMessage;

  // Must track the actual Prepare message, not just the sender, as these may need to be reused
  // to send out in a PrepareCertificate.
  private final Set<Propose> proposeMessages = Sets.newLinkedHashSet();
  private final Set<Vote> voteMessages = Sets.newLinkedHashSet();
  private final Set<SelectLeader> selectLeaderMessages = Sets.newLinkedHashSet();
  private final Set<Commit> commitMessages = Sets.newLinkedHashSet();
  private final Set<BlockAnnounce> blockAnnounceMessages = Sets.newLinkedHashSet();
  private final Set<ViewChange> viewChangeMessages = Sets.newLinkedHashSet();
  private final Set<Bls.Signature>  blsSignaturesMessages = Sets.newLinkedHashSet();
  @Setter
  private PosMessage currentState;
  private final long height;

  /**
   * Instantiates a new Round state.
   *
   * @param roundIdentifier the round identifier
   * @param quorum the quorum
   */
  public RoundState(
          final ConsensusRoundIdentifier roundIdentifier,
          final int quorum, long height) {
    this.roundIdentifier = roundIdentifier;
    this.quorum = quorum;
    this.height = height;
    this.currentState=PosMessage.SELECT_LEADER;
  }

  public void addSelectLeaderMessage(final SelectLeader msg) {
//    if (Objects.nonNull(proposeMessage)) {
      selectLeaderMessages.add(msg);
      LOG.trace("Round state added selectleader message ={}", msg);
//    }
  }

  public void addProposalMessage(final Propose msg) {
//    if (Objects.nonNull(proposeMessage)) {
    proposeMessages.add(msg);
    LOG.trace("Round state added Propose message ={}", msg);
//    }
  }


  public void addVoteMessage(final Vote msg) {
    if (Objects.nonNull(proposeMessage)) {
      voteMessages.add(msg);
      LOG.trace("Round state added vote message ={}", msg);
    }
  }
  public void addCommitMessage(final Commit msg) {
    if (Objects.nonNull(proposeMessage)) {
      commitMessages.add(msg);
      LOG.trace("Round state added commit message ={}", msg);
    }
  }

  public void addBlockAnnounceMessage(final BlockAnnounce msg) {
    if (Objects.nonNull(blockAnnounceMessages)) {
      blockAnnounceMessages.add(msg);
      LOG.trace("Round state added BlockAnnounce message ={}", msg);
    }
  }

    public void addBlsSignatureMessage(final Bls.Signature msg) {
        blsSignaturesMessages.add(msg);
        LOG.trace("Round state added blsSignaturesMessages  ={}", msg);
    }

  public void addViewChangeMessage(final ViewChange msg) {
      viewChangeMessages.add(msg);
      LOG.trace("Round state added viewChangeMessages message ={}", msg);
  }

  public Collection<SECPSignature> getCommitSeals() {
    return Collections.emptyList();//todo
  }



  public PosBlock getProposedBlock() {
    return proposeMessage.getSignedPayload().getPayload().getProposedBlock();
  }
}