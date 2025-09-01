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
package org.hyperledger.besu.consensus.pos;

import org.hyperledger.besu.consensus.common.bft.Gossiper;
import org.hyperledger.besu.consensus.common.bft.network.ValidatorMulticaster;
import org.hyperledger.besu.consensus.common.bft.payload.Authored;

import org.hyperledger.besu.consensus.pos.messagedata.*;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.Message;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.MessageData;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

/** Class responsible for rebroadcasting IBFT messages to known validators */
public class PosGossip implements Gossiper {

  private final ValidatorMulticaster multicaster;

  /**
   * Constructor that attaches gossip logic to a set of multicaster
   *
   * @param multicaster Network connections to the remote validators
   */
  public PosGossip(final ValidatorMulticaster multicaster) {
    this.multicaster = multicaster;
  }

  /**
   * Retransmit a given IBFT message to other known validators nodes
   *
   * @param message The raw message to be gossiped
   */
  @Override
  public void send(final Message message) {
    final MessageData messageData = message.getData();
    final Authored decodedMessage;
      Map<Integer, PosMessage> CODE_TO_MESSAGE =
              Arrays.stream(PosMessage.values())
                      .collect(Collectors.toMap(PosMessage::getCode, m -> m));
      decodedMessage = switch (CODE_TO_MESSAGE.get(messageData.getCode())) {
          case PosMessage.SELECT_LEADER -> SelectLeaderMessageData.fromMessageData(messageData).decode();
          case PosMessage.PROPOSE -> ProposalMessageData.fromMessageData(messageData).decode();
          case PosMessage.BLOCK_ANNOUNCE -> CommitMessageData.fromMessageData(messageData).decode();
          case PosMessage.VOTE -> null;
//          VoteMessageData.fromMessageData(messageData).decode();
          case PosMessage.VIEW_CHANGE -> ViewChangeMessageData.fromMessageData(messageData).decode();

          default -> throw new IllegalArgumentException(
                  "Received message does not conform to any recognised pos message structure.");
      };
      if (decodedMessage == null) {
        return;
      }
    final List<Address> excludeAddressesList =
        Lists.newArrayList(
            message.getConnection().getPeerInfo().getAddress(), decodedMessage.getAuthor());

    multicaster.send(messageData, excludeAddressesList);
  }
}
