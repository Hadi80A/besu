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
import org.hyperledger.besu.consensus.common.bft.Gossiper;
import org.hyperledger.besu.consensus.common.bft.MessageTracker;
import org.hyperledger.besu.consensus.common.bft.SynchronizerUpdater;
import org.hyperledger.besu.consensus.common.bft.statemachine.BaseBftController;
import org.hyperledger.besu.consensus.common.bft.statemachine.BaseBlockHeightManager;
import org.hyperledger.besu.consensus.common.bft.statemachine.FutureMessageBuffer;
import org.hyperledger.besu.consensus.nexus.NexusExtraData;
import org.hyperledger.besu.consensus.nexus.NexusExtraDataCodec;
import org.hyperledger.besu.consensus.nexus.core.NexusBlockHeader;
import org.hyperledger.besu.consensus.nexus.core.NexusFinalState;
import org.hyperledger.besu.consensus.nexus.messagedata.*;
import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.Message;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.MessageData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/** The Nexus controller. */
public class NexusController extends BaseBftController {
  private static final Logger LOG = LoggerFactory.getLogger(NexusController.class);

  private BaseNexusBlockHeightManager currentHeightManager;
  private final NexusBlockHeightManagerFactory posBlockHeightManagerFactory;
//  private StakeManagerInteractor stakeManager;
  private final Blockchain blockchain;

  public NexusController(
          final Blockchain blockchain,
          final NexusFinalState posFinalState,
          final NexusBlockHeightManagerFactory posBlockHeightManagerFactory,
          final Gossiper gossiper,
          final MessageTracker duplicateMessageTracker,
          final FutureMessageBuffer futureMessageBuffer,
          final SynchronizerUpdater synchronizerUpdater) {

    super(
        blockchain,
        posFinalState.getBftFinalState(),
        gossiper,
        duplicateMessageTracker,
        futureMessageBuffer,
        synchronizerUpdater);

    this.posBlockHeightManagerFactory = posBlockHeightManagerFactory;
//    this.stakeManager=stakeManager;
      this.blockchain = blockchain;
  }

  @Override
  protected void handleMessage(final Message message) {
    final MessageData messageData = message.getData();
    Map<Integer, NexusMessage> CODE_TO_MESSAGE =
            Arrays.stream(NexusMessage.values())
                    .collect(Collectors.toMap(NexusMessage::getCode, m -> m));

    LOG.debug("received a message: {}", messageData);
    if (!currentHeightManager.checkValidState(messageData.getCode())){
      LOG.warn("received a message with invalid state code: {}", messageData.getCode());
      return;
    }
    switch (CODE_TO_MESSAGE.get(messageData.getCode())) {
      case NexusMessage.SELECT_LEADER:
        consumeMessage(
                message,
                SelectLeaderMessageData.fromMessageData(messageData).decode(),
                currentHeightManager::consumeSelectLeaderMessage);
        break;
      case NexusMessage.PROPOSE:
        consumeMessage(
            message,
            ProposalMessageData.fromMessageData(messageData).decode(),
            currentHeightManager::consumeProposeMessage);
        break;

      case NexusMessage.VOTE:
        consumeMessage(
            message,
            VoteMessageData.fromMessageData(messageData).decode(),
            currentHeightManager::consumeVoteMessage);
        break;

      case NexusMessage.COMMIT:
        consumeMessage(
            message,
            CommitMessageData.fromMessageData(messageData).decode(),
            currentHeightManager::consumeCommitMessage);
        break;

      case NexusMessage.BLOCK_ANNOUNCE:
        consumeMessage(
                message,
                BlockAnnounceMessageData.fromMessageData(messageData).decode(),
                currentHeightManager::consumeBlockAnnounceMessage);
        break;

      case NexusMessage.VIEW_CHANGE:
        consumeMessage(
            message,
            ViewChangeMessageData.fromMessageData(messageData).decode(),
            currentHeightManager::handleViewChangePayload);
        break;

      default:
        throw new IllegalArgumentException(
            String.format(
                "Received message with messageCode=%d does not conform to any recognised NEXUS message structure",
                message.getData().getCode()));
    }
  }

  @Override
  protected void createNewHeightManager(final BlockHeader parentHeader) {
    NexusBlockHeader posBlockHeader = getNexusBlockHeader(parentHeader);
    currentHeightManager = posBlockHeightManagerFactory.create(posBlockHeader,blockchain);

  }

  @NotNull
  private NexusBlockHeader getNexusBlockHeader(BlockHeader parentHeader) {
    if(parentHeader.getNumber()<=0) {
      return new NexusBlockHeader(parentHeader,new ConsensusRoundIdentifier(0,0),null);
    }
    NexusExtraData posExtraData = new NexusExtraDataCodec().decodeNexusData(parentHeader.getExtraData());

    ConsensusRoundIdentifier roundIdentifier=new ConsensusRoundIdentifier(0,posExtraData.getRound()); //TODO: sequense
      return new NexusBlockHeader(parentHeader,roundIdentifier,posExtraData.getProposer());
  }

  @Override
  protected BaseBlockHeightManager getCurrentHeightManager() {
    return currentHeightManager;
  }


  @Override
  protected void stopCurrentHeightManager(final BlockHeader parentHeader) {
    NexusBlockHeader posBlockHeader = getNexusBlockHeader(parentHeader);
    currentHeightManager = posBlockHeightManagerFactory.createNoOpBlockHeightManager(posBlockHeader);
  }
}
