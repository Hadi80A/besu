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
package org.hyperledger.besu.consensus.pactus.statemachine;

import org.hyperledger.besu.consensus.common.bft.Gossiper;
import org.hyperledger.besu.consensus.common.bft.MessageTracker;
import org.hyperledger.besu.consensus.common.bft.SynchronizerUpdater;
import org.hyperledger.besu.consensus.common.bft.statemachine.BaseBftController;
import org.hyperledger.besu.consensus.common.bft.statemachine.BaseBlockHeightManager;
import org.hyperledger.besu.consensus.common.bft.statemachine.BftFinalState;
import org.hyperledger.besu.consensus.common.bft.statemachine.FutureMessageBuffer;
import org.hyperledger.besu.consensus.pactus.messagedata.*;
import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.Message;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.MessageData;

/** The Pactus controller. */
public class PactusController extends BaseBftController {

  private PactusBlockManager currentHeightManager;
  private final PactusBlockHeightManagerFactory PactusBlockHeightManagerFactory;

  /**
   * Instantiates a new Pactus controller.
   *
   * @param blockchain the blockchain
   * @param bftFinalState the bft final state
   * @param PactusBlockHeightManagerFactory the Pactus block height manager factory
   * @param gossiper the gossiper
   * @param duplicateMessageTracker the duplicate message tracker
   * @param futureMessageBuffer the future message buffer
   * @param synchronizerUpdater the synchronizer updater
   */
  public PactusController(
          final Blockchain blockchain,
          final BftFinalState bftFinalState,
          final PactusBlockHeightManagerFactory PactusBlockHeightManagerFactory,
          final Gossiper gossiper,
          final MessageTracker duplicateMessageTracker,
          final FutureMessageBuffer futureMessageBuffer,
          final SynchronizerUpdater synchronizerUpdater) {

    super(
            blockchain,
            bftFinalState,
            gossiper,
            duplicateMessageTracker,
            futureMessageBuffer,
            synchronizerUpdater);
    this.PactusBlockHeightManagerFactory = PactusBlockHeightManagerFactory;
  }

  @Override
  protected void handleMessage(final Message message) {
    final MessageData messageData = message.getData();
      if (messageData.getCode() == PactusMessage.PROPOSAL.getCode()) {
        consumeMessage(
                message,
                ProposalMessageData.fromMessageData(messageData).decode(),
                currentHeightManager::handleProposalPayload);
      }
    else if (messageData.getCode() == PactusMessage.PREPARE.getCode()) {
      consumeMessage(
              message,
              PrepareMessageData.fromMessageData(messageData).decode(),
              currentHeightManager::handlePreparePayload);
    }
    if (messageData.getCode() == PactusMessage.PRE_COMMIT.getCode()) {
      consumeMessage(
              message,
              PreCommitMessageData.fromMessageData(messageData).decode(),
              currentHeightManager::handlePreCommitPayload);
    }
    else if (messageData.getCode() == PactusMessage.COMMIT.getCode()) {
      consumeMessage(
              message,
              CommitMessageData.fromMessageData(messageData).decode(),
              currentHeightManager::handleCommitPayload);
    }else{
      throw new IllegalArgumentException(
        String.format(
                "Received message with messageCode=%d does not conform to any recognised Pactus message structure",
                message.getData().getCode()));
    }
  }

  protected void createNewHeightManager(final BlockHeader parentHeader) {
    currentHeightManager = PactusBlockHeightManagerFactory.create(parentHeader);
  }

  protected BaseBlockHeightManager getCurrentHeightManager() {
    return currentHeightManager;
  }

  protected void stopCurrentHeightManager(final BlockHeader parentHeader) {
    currentHeightManager = PactusBlockHeightManagerFactory.createNoOpBlockHeightManager(parentHeader);
  }
}
