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
package org.hyperledger.besu.consensus.nexus.messagedata;

import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.consensus.common.bft.messagedata.AbstractBftMessageData;
import org.hyperledger.besu.consensus.nexus.messagewrappers.RoundChange;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.MessageData;

/** The RoundChange message data. */
public class ViewChangeMessageData extends AbstractBftMessageData {

  private static final int MESSAGE_CODE = NexusMessage.VIEW_CHANGE.getCode();

  private ViewChangeMessageData(final Bytes data) {
    super(data);
  }

  /**
   * Instantiate ViewChangeMessageData from message data.
   *
   * @param messageData the message data
   * @return the RoundChange message data
   */
  public static ViewChangeMessageData fromMessageData(final MessageData messageData) {
    return fromMessageData(
        messageData, MESSAGE_CODE, ViewChangeMessageData.class, ViewChangeMessageData::new);
  }

  /**
   * Decode.
   *
   * @return the RoundChange
   */
  public RoundChange decode() {
    return RoundChange.decode(data);
  }

  /**
   * Create RoundChange message data.
   *
   * @param viewChange the RoundChange
   * @return the vote message data
   */
  public static ViewChangeMessageData create(final RoundChange viewChange) {
    return new ViewChangeMessageData(viewChange.encode());
  }

  @Override
  public int getCode() {
    return MESSAGE_CODE;
  }
}
