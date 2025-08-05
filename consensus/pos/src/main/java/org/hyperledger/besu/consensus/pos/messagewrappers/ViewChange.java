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
package org.hyperledger.besu.consensus.pos.messagewrappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.SneakyThrows;
import org.hyperledger.besu.consensus.common.bft.messagewrappers.BftMessage;
import org.hyperledger.besu.consensus.common.bft.payload.SignedData;
import org.hyperledger.besu.consensus.pos.core.PosBlock;
import org.hyperledger.besu.consensus.pos.payload.ViewChangePayload;
import org.hyperledger.besu.ethereum.rlp.BytesValueRLPOutput;
import org.hyperledger.besu.ethereum.rlp.RLP;
import org.hyperledger.besu.ethereum.rlp.RLPInput;

import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;

/** The Round change payload message. */
@Getter
public class ViewChange extends BftMessage<ViewChangePayload> {


  /**
   * Instantiates a new Round change.
   *
   * @param payload the payload
   */
  public ViewChange(
      final SignedData<ViewChangePayload> payload) {
    super(payload);
  }

    @Override
  public Bytes encode() {
    final BytesValueRLPOutput rlpOut = new BytesValueRLPOutput();
    rlpOut.startList();
    getSignedPayload().writeTo(rlpOut);
    rlpOut.endList();
    return rlpOut.encoded();
  }

  /**
   * Decode.
   *
   * @param data the data
   * @return the round change
   */
  @SneakyThrows
  public static ViewChange decode(final Bytes data) {

    final RLPInput rlpIn = RLP.input(data);
    rlpIn.enterList();
    final SignedData<ViewChangePayload> payload = readPayload(rlpIn, ViewChangePayload::readFrom);

    rlpIn.leaveList();

    return new ViewChange(payload);
  }
}