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
import lombok.SneakyThrows;
import org.hyperledger.besu.consensus.common.bft.BftBlockHeaderFunctions;
import org.hyperledger.besu.consensus.common.bft.messagewrappers.BftMessage;
import org.hyperledger.besu.consensus.common.bft.payload.SignedData;
import org.hyperledger.besu.consensus.pos.PosExtraDataCodec;
import org.hyperledger.besu.consensus.pos.core.PosBlock;
import org.hyperledger.besu.consensus.pos.network.PosMessageTransmitter;
import org.hyperledger.besu.consensus.pos.payload.PayloadDeserializers;
import org.hyperledger.besu.consensus.pos.payload.ProposePayload;
import org.hyperledger.besu.ethereum.rlp.BytesValueRLPOutput;
import org.hyperledger.besu.ethereum.rlp.RLP;
import org.hyperledger.besu.ethereum.rlp.RLPInput;

import org.apache.tuweni.bytes.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The Proposal. */

@Getter
public class Propose extends BftMessage<ProposePayload> {

  private static final PosExtraDataCodec BFT_EXTRA_DATA_ENCODER = new PosExtraDataCodec();
  private static final Logger LOG = LoggerFactory.getLogger(Propose.class);

  /**
   * Instantiates a new Proposal.
   *
   * @param payload the payload
//   * @param certificate the certificate
   */
  public Propose(
          final SignedData<ProposePayload> payload
  ) {
    super(payload);
//    this.proposedBlock = proposedBlock;
  }

  /**
   * Gets round change certificate.
   *
   * @return the round change certificate
   */
//  public Optional<RoundChangeCertificate> getRoundChangeCertificate() {
//    return roundChangeCertificate;
//  }

//  @SneakyThrows
  @Override
  public Bytes encode() {
    final BytesValueRLPOutput rlpOut = new BytesValueRLPOutput();
    rlpOut.startList();
    getSignedPayload().writeTo(rlpOut);
    rlpOut.endList();
    LOG.debug("Encoding Propose payload");
    return rlpOut.encoded();
  }

  /**
   * Decode.
   *
   * @param data the data
   * @return the proposal
   */
  @SneakyThrows
  public static Propose decode(final Bytes data) {
    final RLPInput rlpIn = RLP.input(data);
    rlpIn.enterList();
    final SignedData<ProposePayload> payload =
        PayloadDeserializers.readSignedProposalPayloadFrom(rlpIn);

    rlpIn.leaveList();
    return new Propose(payload);
  }

}
