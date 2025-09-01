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

import org.hyperledger.besu.consensus.common.bft.messagewrappers.BftMessage;
import org.hyperledger.besu.consensus.common.bft.payload.SignedData;
import org.hyperledger.besu.consensus.pos.payload.PayloadDeserializers;
import org.hyperledger.besu.consensus.pos.payload.VotePayload;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.rlp.BytesValueRLPOutput;
import org.hyperledger.besu.ethereum.rlp.RLP;

import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.ethereum.rlp.RLPInput;

/** The Vote. */
public class Vote extends BftMessage<VotePayload> {

  /**
   * Instantiates a new Vote.
   *
   * @param payload the payload
   */
  public Vote(final SignedData<VotePayload> payload) {
    super(payload);
  }


  /**
   * Gets digest.
   *
   * @return the digest
   */
  public Hash getDigest() {
    return getPayload().getDigest();
  }

  @Override
  public Bytes encode() {
    final BytesValueRLPOutput rlpOut = new BytesValueRLPOutput();
    rlpOut.startList();
    getSignedPayload().writeTo(rlpOut);
    rlpOut.endList();
    return rlpOut.encoded();
  }


  public static Vote decode(final Bytes data) {
    final RLPInput rlpIn = RLP.input(data);
    rlpIn.enterList();
    final SignedData<VotePayload> payload =
            PayloadDeserializers.readSignedVotePayloadFrom(rlpIn);

    rlpIn.leaveList();
    return new Vote(payload);
  }

}
