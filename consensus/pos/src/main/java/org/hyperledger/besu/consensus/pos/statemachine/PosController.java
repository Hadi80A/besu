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

import org.hyperledger.besu.consensus.common.bft.Gossiper;
import org.hyperledger.besu.consensus.common.bft.MessageTracker;
import org.hyperledger.besu.consensus.common.bft.SynchronizerUpdater;
import org.hyperledger.besu.consensus.common.bft.statemachine.BaseBftController;
import org.hyperledger.besu.consensus.common.bft.statemachine.BaseBlockHeightManager;
import org.hyperledger.besu.consensus.common.bft.statemachine.FutureMessageBuffer;
import org.hyperledger.besu.consensus.pos.core.PosFinalState;
import org.hyperledger.besu.consensus.pos.messagedata.*;
import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.Message;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.MessageData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The PoS Controller.
 *
 * <p>Phase 5: Propagation (Reception)
 * Acts as the dispatcher for incoming consensus messages. It decodes the RLPX messages
 * and forwards them to the current BlockHeightManager for the active LCR state machine.
 */
public class PosController extends BaseBftController {
    private static final Logger LOG = LoggerFactory.getLogger(PosController.class);

    // Optimization: Static map for O(1) message code lookup
    private static final Map<Integer, PosMessage> CODE_TO_MESSAGE =
            Arrays.stream(PosMessage.values())
                    .collect(Collectors.toMap(PosMessage::getCode, m -> m));

    private BasePosBlockHeightManager currentHeightManager;
    private final PosBlockHeightManagerFactory posBlockHeightManagerFactory;
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
        this.blockchain = blockchain;
    }

    @Override
    protected void handleMessage(final Message message) {
        final MessageData messageData = message.getData();
        final PosMessage posMessage = CODE_TO_MESSAGE.get(messageData.getCode());

        if (posMessage == null) {
            LOG.warn("Received message with unknown code: {}", messageData.getCode());
            return;
        }

        LOG.trace("Received a message: {}", posMessage);

        // State check prevents processing messages inconsistent with current LCR phase
        if (!currentHeightManager.checkValidState(messageData.getCode())){
            LOG.trace("Received message code {} invalid for current state", messageData.getCode());
            return;
        }

        try {
            switch (posMessage) {
                case PROPOSE:
                    consumeMessage(
                            message,
                            ProposalMessageData.fromMessageData(messageData).decode(),
                            currentHeightManager::consumeProposeMessage);
                    break;

                case VOTE:
                    consumeMessage(
                            message,
                            VoteMessageData.fromMessageData(messageData).decode(),
                            currentHeightManager::consumeVoteMessage);
                    break;

                default:
                    throw new IllegalArgumentException(
                            String.format(
                                    "Received message with messageCode=%d does not conform to any recognised POS message structure",
                                    message.getData().getCode()));
            }
        } catch (final Exception e) {
            LOG.warn("Error processing message {}: {} ,cause: {}", posMessage, e.getMessage(), e.getCause());

        }
    }

    @Override
    protected void createNewHeightManager(final BlockHeader parentHeader) {
        currentHeightManager = posBlockHeightManagerFactory.create(parentHeader, blockchain);
    }



    @Override
    protected BaseBlockHeightManager getCurrentHeightManager() {
        return currentHeightManager;
    }

    @Override
    protected void stopCurrentHeightManager(final BlockHeader parentHeader) {
        currentHeightManager = posBlockHeightManagerFactory.createNoOpBlockHeightManager(parentHeader);
    }
}