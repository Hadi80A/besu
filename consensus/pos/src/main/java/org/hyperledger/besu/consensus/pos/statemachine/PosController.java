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
import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.Message;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.MessageData;
import org.jetbrains.annotations.NotNull;

/** The Pos controller. */
public class PosController extends BaseBftController {

  private BasePosBlockHeightManager currentHeightManager;
  private final PosBlockHeightManagerFactory posBlockHeightManagerFactory;
//  private StakeManagerInteractor stakeManager;

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
  }

  @Override
  protected void handleMessage(final Message message) {
    final MessageData messageData = message.getData();

//    switch (messageData.getCode()) {
//      case Pos.PROPOSAL:
//        consumeMessage(
//            message,
//            ProposalMessageData.fromMessageData(messageData).decode(),
//            currentHeightManager::handleProposalPayload);
//        break;
//
//      case Pos.PREPARE:
//        consumeMessage(
//            message,
//            PrepareMessageData.fromMessageData(messageData).decode(),
//            currentHeightManager::handlePreparePayload);
//        break;
//
//      case Pos.COMMIT:
//        consumeMessage(
//            message,
//            CommitMessageData.fromMessageData(messageData).decode(),
//            currentHeightManager::handleCommitPayload);
//        break;
//
//      case Pos.ROUND_CHANGE:
//        consumeMessage(
//            message,
//            RoundChangeMessageData.fromMessageData(messageData).decode(),
//            currentHeightManager::handleRoundChangePayload);
//        break;
//
//      default:
//        throw new IllegalArgumentException(
//            String.format(
//                "Received message with messageCode=%d does not conform to any recognised IBFT message structure",
//                message.getData().getCode()));
//    }
  }

  @Override
  protected void createNewHeightManager(final BlockHeader parentHeader) {
    PosBlockHeader posBlockHeader = getPosBlockHeader(parentHeader);
    currentHeightManager = posBlockHeightManagerFactory.create(posBlockHeader);
  }

  @NotNull
  private static PosBlockHeader getPosBlockHeader(BlockHeader parentHeader) {
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
