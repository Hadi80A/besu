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

import lombok.Getter;
import org.hyperledger.besu.consensus.common.bft.messagewrappers.BftMessage;
import org.hyperledger.besu.consensus.common.bft.payload.SignedData;
import org.hyperledger.besu.consensus.pos.payload.PayloadDeserializers;
import org.hyperledger.besu.consensus.pos.payload.SelectLeaderPayload;
import org.hyperledger.besu.ethereum.rlp.BytesValueRLPOutput;
import org.hyperledger.besu.ethereum.rlp.RLP;
import org.hyperledger.besu.ethereum.rlp.RLPInput;

import org.apache.tuweni.bytes.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The Proposal. */

@Getter
public class SelectLeader extends BftMessage<SelectLeaderPayload> {

  private static final Logger LOG = LoggerFactory.getLogger(SelectLeader.class);

  /**
   * Instantiates a new Proposal.
   *
   * @param payload the payload
//   * @param certificate the certificate
   */
  public SelectLeader(
          final SignedData<SelectLeaderPayload> payload
  ) {
    super(payload);
//    this.SelectLeaderdBlock = SelectLeaderdBlock;
  }

  @Override
  public Bytes encode() {
    final BytesValueRLPOutput rlpOut = new BytesValueRLPOutput();
    rlpOut.startList();
    getSignedPayload().writeTo(rlpOut);
    rlpOut.endList();
    LOG.debug("Encoding SelectLeader payload");
    return rlpOut.encoded();
  }

  public static SelectLeader decode(final Bytes data) {
    final RLPInput rlpIn = RLP.input(data);
    rlpIn.enterList();
    final SignedData<SelectLeaderPayload> payload =
        PayloadDeserializers.readSignedSelectLeaderPayloadFrom(rlpIn);

    rlpIn.leaveList();
    return new SelectLeader(payload);
  }

}
