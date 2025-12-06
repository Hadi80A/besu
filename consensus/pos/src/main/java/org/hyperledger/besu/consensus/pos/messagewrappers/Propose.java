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
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.consensus.common.bft.messagewrappers.BftMessage;
import org.hyperledger.besu.consensus.common.bft.payload.SignedData;
import org.hyperledger.besu.consensus.pos.payload.PayloadDeserializers;
import org.hyperledger.besu.consensus.pos.payload.ProposePayload;
import org.hyperledger.besu.ethereum.rlp.BytesValueRLPOutput;
import org.hyperledger.besu.ethereum.rlp.RLP;
import org.hyperledger.besu.ethereum.rlp.RLPInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Propose message wrapper.
 *
 * <p>Phase 4: Block Propagation
 * Wraps the signed ProposePayload (containing the new Block) for wire transmission.
 * This is the critical message for LCR consensus, carrying the candidate block
 * for the current slot.
 */
@Getter
public class Propose extends BftMessage<ProposePayload> {

    private static final Logger LOG = LoggerFactory.getLogger(Propose.class);

    /**
     * Instantiates a new Proposal message.
     *
     * @param payload the signed payload containing the block
     */
    public Propose(final SignedData<ProposePayload> payload) {
        super(payload);
    }

    @Override
    public Bytes encode() {
        final BytesValueRLPOutput rlpOut = new BytesValueRLPOutput();
        rlpOut.startList();
        getSignedPayload().writeTo(rlpOut);
        rlpOut.endList();
        LOG.trace("Encoding Propose payload for round {}", getRoundIdentifier());
        return rlpOut.encoded();
    }

    /**
     * Decodes a Propose message from raw RLP bytes.
     *
     * @param data the RLP encoded data
     * @return the decoded Propose message
     */
    public static Propose decode(final Bytes data) {
        final RLPInput rlpIn = RLP.input(data);
        rlpIn.enterList();
        final SignedData<ProposePayload> payload = PayloadDeserializers.readSignedProposalPayloadFrom(rlpIn);
        rlpIn.leaveList();
        return new Propose(payload);
    }
}