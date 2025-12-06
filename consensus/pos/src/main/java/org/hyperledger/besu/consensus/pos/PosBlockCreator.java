/*
 * Copyright contributors to Besu.
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
package org.hyperledger.besu.consensus.pos;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.consensus.common.bft.BftBlockHeaderFunctions;
import org.hyperledger.besu.consensus.common.bft.BftExtraData;
import org.hyperledger.besu.crypto.SECPSignature;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.blockcreation.BlockCreator;
import org.hyperledger.besu.ethereum.core.Block;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.core.BlockHeaderBuilder;

import java.util.Collection;


@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class PosBlockCreator {

    private static final Logger log = LogManager.getLogger(PosBlockCreator.class);
    private final BlockCreator besuBlockCreator;
    private final PosExtraDataCodec posExtraDataCodec;

    // Phase 3 Artifact: The seed used for FTS in this round.
    private final Bytes32 seed;

    /**
     * Explicit constructor to match the factory usage.
     */
    public PosBlockCreator(final BlockCreator besuBlockCreator,
                           final PosExtraDataCodec posExtraDataCodec,
                           final Bytes32 seed) {
        this.besuBlockCreator = besuBlockCreator;
        this.posExtraDataCodec = posExtraDataCodec;
        this.seed = seed;
    }

    /**
     * Creates a candidate block.
     * The underlying creator typically produces a block with basic/template extra data.
     *
     * @param headerTimeStampSeconds the timestamp for the new block
     * @param parentHeader the parent block header
     * @return the candidate Block (unsigned/template)
     */
    public Block createBlock(final long headerTimeStampSeconds, final BlockHeader parentHeader) {
        var blockResult = besuBlockCreator.createBlock(headerTimeStampSeconds, parentHeader);
        return blockResult.getBlock();
    }

    /**
     * Finalizes the block by injecting the Proposer's signature and PoS metadata.
     *
     * @param block The template block.
     * @param roundNumber The current round.
     * @param commitSeals The Proposer's ECDSA signature (DSS).
     * @param proposer The Proposer's address.
     * @return The fully formed and signed Block.
     */
    public Block createSealedBlock(
            final Block block,
            final int roundNumber,
            final Collection<SECPSignature> commitSeals,
            final Address proposer) {

        log.debug("Creating sealed block for round {}", roundNumber);
        final BlockHeader initialHeader = block.getHeader();

        // Decode the existing extra data from the template block to preserve fields
        // (e.g. validators, vote, vanity) that were set by the upstream creator.
        final BftExtraData initialExtraData = posExtraDataCodec.decode(initialHeader);

        // Rebuild the header with the new ExtraData
        final BlockHeader sealedHeader =
                BlockHeaderBuilder.fromHeader(initialHeader)
                        .extraData(posExtraDataCodec.encode(initialExtraData))
                        .blockHeaderFunctions(BftBlockHeaderFunctions.forOnchainBlock(posExtraDataCodec))
                        .buildBlockHeader();

        return new Block(sealedHeader, block.getBody());
    }
}