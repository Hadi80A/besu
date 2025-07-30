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
package org.hyperledger.besu.consensus.pos.messagedata;

import org.hyperledger.besu.consensus.common.bft.messagedata.AbstractBftMessageData;
import org.hyperledger.besu.consensus.pos.messagewrappers.Vote;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.MessageData;

import org.apache.tuweni.bytes.Bytes;

/** The Vote message data. */
public class VoteMessageData extends AbstractBftMessageData {

  private static final int MESSAGE_CODE = Pos.COMMIT;

  private VoteMessageData(final Bytes data) {
    super(data);
  }

  /**
   * Instantiate VoteMessageData from message data.
   *
   * @param messageData the message data
   * @return the vote message data
   */
  public static VoteMessageData fromMessageData(final MessageData messageData) {
    return fromMessageData(
        messageData, MESSAGE_CODE, VoteMessageData.class, VoteMessageData::new);
  }

  /**
   * Decode.
   *
   * @return the vote
   */
  public Vote decode() {
    return Vote.decode(data);
  }

  /**
   * Create vote message data.
   *
   * @param vote the vote
   * @return the vote message data
   */
  public static VoteMessageData create(final Vote vote) {
    return new VoteMessageData(vote.encode());
  }

  @Override
  public int getCode() {
    return MESSAGE_CODE;
  }
}
