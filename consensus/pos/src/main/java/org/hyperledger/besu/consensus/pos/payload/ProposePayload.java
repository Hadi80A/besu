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
package org.hyperledger.besu.consensus.pos.payload;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.besu.consensus.common.bft.BftBlockHeaderFunctions;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.pos.PosExtraDataCodec;
import org.hyperledger.besu.consensus.pos.messagedata.PosMessage;
import org.hyperledger.besu.ethereum.core.Block;
import org.hyperledger.besu.ethereum.rlp.RLPInput;
import org.hyperledger.besu.ethereum.rlp.RLPOutput;

/**
 * The Proposal payload.
 *
 * <p>Phase 4: Block Propagation
 * Carries the proposed block for the current round.
 * Updated for Pure PoS to remove VRF proofs, as leader selection is deterministic (FTS).
 */
@Slf4j
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ProposePayload extends PosPayload {

    private static final int TYPE = PosMessage.PROPOSE.getCode();

    private final Block proposedBlock;

    private static final PosExtraDataCodec posExtraDataCodec= new PosExtraDataCodec();
    /**
     * Constructor for the Proposal Payload.
     *
     * @param roundIdentifier   The round this proposal belongs to.
     * @param proposedBlock     The actual block being proposed.
c
     */
    public ProposePayload(final ConsensusRoundIdentifier roundIdentifier,
                          final Block proposedBlock) {
        super(roundIdentifier);
        this.proposedBlock = proposedBlock;
    }

    /**
     * Deserializes the payload from RLP input.
     *
     * @param rlpInput RLP source
     * @return The ProposePayload instance
     */
    public static ProposePayload readFrom(final RLPInput rlpInput) {
        rlpInput.enterList();
        final ConsensusRoundIdentifier roundIdentifier = ConsensusRoundIdentifier.readFrom(rlpInput);

        // PosBlock.readFrom throws unchecked RLPException on failure, no explicit catch needed
        final Block proposedBlock = Block.readFrom(rlpInput, BftBlockHeaderFunctions.forCommittedSeal(posExtraDataCodec));

        rlpInput.leaveList();
        log.debug("read proposedBlock");
        return new ProposePayload(roundIdentifier, proposedBlock );
    }

    @Override
    public void writeTo(final RLPOutput rlpOutput) {
        rlpOutput.startList();
        getRoundIdentifier().writeTo(rlpOutput);

        // PosBlock.writeTo throws unchecked RLPException on failure, no explicit catch needed
        proposedBlock.writeTo(rlpOutput);
        log.debug("writeTo");
        rlpOutput.endList();
    }

    @Override
    public int getMessageType() {
        return TYPE;
    }
}