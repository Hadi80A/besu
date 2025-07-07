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
package org.hyperledger.besu.consensus.pactus.messagewrappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.hyperledger.besu.consensus.common.bft.messagewrappers.BftMessage;
import org.hyperledger.besu.consensus.common.bft.payload.SignedData;
import org.hyperledger.besu.consensus.pactus.PactusBlockCodec;
import org.hyperledger.besu.consensus.pactus.core.PactusBlock;
import org.hyperledger.besu.consensus.pactus.payload.PreparePayload;
import org.hyperledger.besu.consensus.pactus.payload.RoundChangePayload;
import org.hyperledger.besu.ethereum.rlp.BytesValueRLPOutput;
import org.hyperledger.besu.ethereum.rlp.RLP;
import org.hyperledger.besu.ethereum.rlp.RLPInput;

import java.util.List;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;

/** The Round change payload message. */
public class RoundChange extends BftMessage<RoundChangePayload> {

  private final Optional<PactusBlock> proposedBlock;
  private final PactusBlockCodec blockEncoder;

  /**
   * Instantiates a new Round change.
   *
   * @param payload the payload
   * @param proposedBlock the proposed block
   * @param blockEncoder the pactus block encoder
   */
  public RoundChange(
      final SignedData<RoundChangePayload> payload,
      final Optional<PactusBlock> proposedBlock,
      final PactusBlockCodec blockEncoder) {
    super(payload);
    this.proposedBlock = proposedBlock;
    this.blockEncoder = blockEncoder;
  }

  /**
   * Gets proposed block.
   *
   * @return the proposed block
   */
  public Optional<PactusBlock> getProposedBlock() {
    return proposedBlock;
  }

  @Override
  public Bytes encode() {
    final BytesValueRLPOutput rlpOut = new BytesValueRLPOutput();
    rlpOut.startList();
    getSignedPayload().writeTo(rlpOut);
    proposedBlock.ifPresentOrElse(pb -> {
        try {
            blockEncoder.writeTo(pb, rlpOut);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }, rlpOut::writeEmptyList);
    rlpOut.endList();
    return rlpOut.encoded();
  }

  /**
   * Decode.
   *
   * @param data the data
   * @param blockEncoder the pactus block encoder
   * @return the round change
   */
  public static RoundChange decode(final Bytes data, final PactusBlockCodec blockEncoder) {

    final RLPInput rlpIn = RLP.input(data);
    rlpIn.enterList();
    final SignedData<RoundChangePayload> payload = readPayload(rlpIn, RoundChangePayload::readFrom);

    final Optional<PactusBlock> block;
    if (rlpIn.nextIsList() && rlpIn.nextSize() == 0) {
      rlpIn.skipNext();
      block = Optional.empty();
    } else {
      block = Optional.of(blockEncoder.readFrom(rlpIn));
    }

    rlpIn.leaveList();

    return new RoundChange(payload, block, blockEncoder);
  }
}
