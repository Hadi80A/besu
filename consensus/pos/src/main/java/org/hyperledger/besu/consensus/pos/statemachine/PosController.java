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
import org.hyperledger.besu.consensus.common.bft.Gossiper;
import org.hyperledger.besu.consensus.common.bft.MessageTracker;
import org.hyperledger.besu.consensus.common.bft.SynchronizerUpdater;
import org.hyperledger.besu.consensus.common.bft.statemachine.BaseBftController;
import org.hyperledger.besu.consensus.common.bft.statemachine.BaseBlockHeightManager;
import org.hyperledger.besu.consensus.common.bft.statemachine.BftFinalState;
import org.hyperledger.besu.consensus.common.bft.statemachine.FutureMessageBuffer;
import org.hyperledger.besu.consensus.pos.PosExtraData;
import org.hyperledger.besu.consensus.pos.PosExtraDataCodec;
import org.hyperledger.besu.consensus.pos.core.PosBlockHeader;
import org.hyperledger.besu.consensus.pos.core.PosFinalState;
import org.hyperledger.besu.consensus.pos.messagedata.*;
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

/** The Pos controller. */
public class PosController extends BaseBftController {
  private static final Logger LOG = LoggerFactory.getLogger(PosController.class);

  private BasePosBlockHeightManager currentHeightManager;
  private final PosBlockHeightManagerFactory posBlockHeightManagerFactory;
//  private StakeManagerInteractor stakeManager;
  private final Blockchain blockchain;

  public PosController(
          final Blockchain blockchain,
          final PosFinalState posFinalState,
          final PosBlockHeightManagerFactory posBlockHeightManagerFactory,
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
    Map<Integer, PosMessage> CODE_TO_MESSAGE =
            Arrays.stream(PosMessage.values())
                    .collect(Collectors.toMap(PosMessage::getCode, m -> m));

    LOG.debug("received a message: {}", messageData);
    switch (CODE_TO_MESSAGE.get(messageData.getCode())) {
      case PosMessage.PROPOSE:
        consumeMessage(
            message,
            ProposalMessageData.fromMessageData(messageData).decode(),
            currentHeightManager::handleProposalMessage);
        break;

      case PosMessage.VOTE:
        consumeMessage(
            message,
            VoteMessageData.fromMessageData(messageData).decode(),
            currentHeightManager::handleVoteMessage);
        break;

      case PosMessage.BLOCK_ANNOUNCE:
        consumeMessage(
            message,
            CommitMessageData.fromMessageData(messageData).decode(),
            currentHeightManager::handleCommitMessage);
        break;

      case PosMessage.VIEW_CHANGE:
        consumeMessage(
            message,
            ViewChangeMessageData.fromMessageData(messageData).decode(),
            currentHeightManager::handleViewChangePayload);
        break;

      default:
        throw new IllegalArgumentException(
            String.format(
                "Received message with messageCode=%d does not conform to any recognised POS message structure",
                message.getData().getCode()));
    }
  }

  @Override
  protected void createNewHeightManager(final BlockHeader parentHeader) {
    PosBlockHeader posBlockHeader = getPosBlockHeader(parentHeader);
    currentHeightManager = posBlockHeightManagerFactory.create(posBlockHeader,blockchain);

  }

  @NotNull
  private PosBlockHeader getPosBlockHeader(BlockHeader parentHeader) {
    if(parentHeader.getNumber()<=0) {
      return new PosBlockHeader(parentHeader,new ConsensusRoundIdentifier(0,0),null);
    }
    PosExtraData posExtraData = new PosExtraDataCodec().decodePosData(parentHeader.getExtraData());
    ConsensusRoundIdentifier roundIdentifier=new ConsensusRoundIdentifier(0,posExtraData.getRound()); //TODO: sequense
    PosBlockHeader posBlockHeader=new PosBlockHeader(parentHeader,roundIdentifier,posExtraData.getProposer());
    return posBlockHeader;
  }

  @Override
  protected BaseBlockHeightManager getCurrentHeightManager() {
    return currentHeightManager;
  }


  @Override
  protected void stopCurrentHeightManager(final BlockHeader parentHeader) {
    PosBlockHeader posBlockHeader = getPosBlockHeader(parentHeader);
    currentHeightManager = posBlockHeightManagerFactory.createNoOpBlockHeightManager(posBlockHeader);
  }
}
