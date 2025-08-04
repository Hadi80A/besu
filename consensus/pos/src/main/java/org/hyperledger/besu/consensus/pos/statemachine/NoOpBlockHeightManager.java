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
import org.hyperledger.besu.consensus.pos.core.PosBlockHeader;
import org.hyperledger.besu.consensus.pos.messagewrappers.Commit;
import org.hyperledger.besu.consensus.pos.messagewrappers.Propose;
import org.hyperledger.besu.consensus.pos.messagewrappers.Vote;
import org.hyperledger.besu.ethereum.core.BlockHeader;

/** The NoOp block height manager. */
public class NoOpBlockHeightManager implements BasePosBlockHeightManager {

  private final PosBlockHeader parentHeader;

  /**
   * Instantiates a new NoOp block height manager.
   *
   * @param parentHeader the parent header
   */
  public NoOpBlockHeightManager(final PosBlockHeader parentHeader) {
    this.parentHeader = parentHeader;
  }

  @Override
  public void handleBlockTimerExpiry(final ConsensusRoundIdentifier roundIdentifier) {}

  @Override
  public void roundExpired(final RoundExpiry expire) {}

//  @Override
//  public void handleProposalPayload(final Proposal proposal) {}
//
//  @Override
//  public void handlePreparePayload(final Prepare prepare) {}
//
//  @Override
//  public void handleCommitPayload(final Commit commit) {}
//
//  @Override
//  public void handleRoundChangePayload(final RoundChange roundChange) {}

  @Override
  public long getChainHeight() {
    return parentHeader.getBesuBlockHeader().getNumber() + 1;
  }

  @Override
  public BlockHeader getParentBlockHeader() {
    return parentHeader.getBesuBlockHeader();
  }

  @Override
  public void handleProposalMessage(Propose msg) {

  }

  @Override
  public void handleVoteMessage(Vote msg) {

  }

  @Override
  public void handleCommitMessage(Commit msg) {

  }
}
