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
package org.hyperledger.besu.consensus.nexus.statemachine;

import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.events.RoundExpiry;
import org.hyperledger.besu.consensus.nexus.core.NexusBlockHeader;
import org.hyperledger.besu.consensus.nexus.messagewrappers.*;
import org.hyperledger.besu.ethereum.core.BlockHeader;

/** The NoOp block height manager. */
public class NoOpBlockHeightManager implements BaseNexusBlockHeightManager {

  private final NexusBlockHeader parentHeader;

  /**
   * Instantiates a new NoOp block height manager.
   *
   * @param parentHeader the parent header
   */
  public NoOpBlockHeightManager(final NexusBlockHeader parentHeader) {
    this.parentHeader = parentHeader;
  }

  @Override
  public void handleBlockTimerExpiry(final ConsensusRoundIdentifier roundIdentifier) {}

  @Override
  public void roundExpired(final RoundExpiry expire) {}


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

  @Override
  public void handleRoundChangeMessage(RoundChange message) {

  }

  @Override
  public void handleSelectLeaderMessage(SelectLeader message) {

  }

  @Override
  public void consumeProposeMessage(Propose msg) {

  }

  @Override
  public void consumeVoteMessage(Vote msg) {

  }

  @Override
  public void consumeCommitMessage(Commit msg) {

  }

  @Override
  public void consumeBlockAnnounceMessage(BlockAnnounce msg) {

  }

  @Override
  public void consumeRoundChangeMessage(RoundChange message) {

  }

  @Override
  public void consumeSelectLeaderMessage(SelectLeader message) {

  }

  @Override
  public boolean checkValidState(int msgCode) {
    return false;
  }
}
