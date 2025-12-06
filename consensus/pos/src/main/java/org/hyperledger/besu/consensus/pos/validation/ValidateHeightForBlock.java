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
package org.hyperledger.besu.consensus.pos.validation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.mainnet.DetachedBlockHeaderValidationRule;

import java.util.function.Supplier;

/**
 * Validates that the block number is strictly sequential relative to its parent.
 *
 * <p>Phase 5: Verification
 * Ensures chain continuity. Unlike the previous implementation, this allows 
 * sibling blocks (forks at the same height) to pass validation, which is required 
 * for the LCR (Largest Chain Rule) fork choice to function.
 */
public class ValidateHeightForBlock implements DetachedBlockHeaderValidationRule {
    private static final Logger log = LogManager.getLogger(ValidateHeightForBlock.class);

    // Kept for compatibility with the factory constructor signature, though unused for this check.
    private final Supplier<Blockchain> blockchainProvider;

    public ValidateHeightForBlock(Supplier<Blockchain> blockchainProvider) {
        this.blockchainProvider = blockchainProvider;
    }

    /**
     * Validates that the header number is exactly one greater than the parent.
     *
     * @param header the block header to validate
     * @param parent the parent block header
     * @return true if the height is sequential, false otherwise
     */
    @Override
    public boolean validate(BlockHeader header, BlockHeader parent) {
        // Standard Continuity Check: H_child == H_parent + 1
        if (header.getNumber() != parent.getNumber() + 1) {
            log.warn("Invalid block height. Header: {}, Parent: {}. Expected Parent+1.",
                    header.getNumber(), parent.getNumber());
            return false;
        }

        // We explicitly DO NOT check against the local chain head here.
        // In LCR/Nakamoto consensus, valid blocks may arrive for side chains 
        // (height <= head) or as extensions (height > head).
        // The Fork Choice rule (BlockImporter) decides which to follow.

        return true;
    }

    @Override
    public String toString() {
        return "ValidateHeightForBlock";
    }
}