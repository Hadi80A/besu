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
import org.hyperledger.besu.consensus.pos.messagewrappers.Commit;
import org.hyperledger.besu.consensus.pos.messagewrappers.Propose;
import org.hyperledger.besu.consensus.pos.core.PosBlock;
import org.hyperledger.besu.consensus.pos.messagewrappers.Vote;
import org.hyperledger.besu.crypto.SECPSignature;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
  private final Set<Vote> voteMessages = Sets.newLinkedHashSet();
  private final Set<Commit> commitMessages = Sets.newLinkedHashSet();
  @Setter
  private State currentState;
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
    this.currentState=State.PROPOSE;
  }

//  public PosBlock getProposedBlock() {
//    return  proposeMessage.getProposedBlock();
//  }

  public void addVoteMessage(final Vote msg) {
    if (Objects.nonNull(proposeMessage)) {
      voteMessages.add(msg);
      LOG.trace("Round state added precommit message commit={}", msg);
    }
  }
  public void addCommitMessage(final Commit msg) {
    if (Objects.nonNull(proposeMessage)) {
      commitMessages.add(msg);
      LOG.trace("Round state added commit message commit={}", msg);
    }
  }

  public Collection<SECPSignature> getCommitSeals() {
    return Collections.emptyList();//todo
    return commitMessages.stream()
            .map(cp -> cp.getSignedPayload().getPayload().getCommitSeal())
            .collect(Collectors.toList());
  }

  public PosBlock getProposedBlock() {
    return proposeMessage.getSignedPayload().getPayload().getProposedBlock();
  }
}