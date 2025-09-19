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
import org.hyperledger.besu.consensus.pos.messagewrappers.BlockAnnounce;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.MessageData;

import org.apache.tuweni.bytes.Bytes;

/** The BlockAnnounce message data. */
public class BlockAnnounceMessageData extends AbstractBftMessageData {

  private static final int MESSAGE_CODE = PosMessage.BLOCK_ANNOUNCE.getCode();


  private BlockAnnounceMessageData(final Bytes data) {
    super(data);
  }

  /**
   * Instantiate BlockAnnounceMessageData from message data.
   *
   * @param messageData the message data
   * @return the blockAnnounce message data
   */
  public static BlockAnnounceMessageData fromMessageData(final MessageData messageData) {
    return fromMessageData(
        messageData, MESSAGE_CODE, BlockAnnounceMessageData.class, BlockAnnounceMessageData::new);
  }

  /**
   * Decode.
   *
   * @return the blockAnnounce
   */
  public BlockAnnounce decode() {
    return BlockAnnounce.decode(data);
  }

  /**
   * Create blockAnnounce message data.
   *
   * @param blockAnnounce the blockAnnounce
   * @return the blockAnnounce message data
   */
  public static BlockAnnounceMessageData create(final BlockAnnounce blockAnnounce) {
    return new BlockAnnounceMessageData(blockAnnounce.encode());
  }

  @Override
  public int getCode() {
    return MESSAGE_CODE;
  }
}
