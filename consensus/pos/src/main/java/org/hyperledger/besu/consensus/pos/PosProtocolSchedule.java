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

import org.hyperledger.besu.ethereum.ProtocolContext;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.core.BlockImporter;
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule;
import org.hyperledger.besu.ethereum.mainnet.ProtocolSpec;


public class PosProtocolSchedule {

    private final ProtocolSchedule besuProtocolSchedule;
    private final ProtocolContext context;

    /**
     * Constructs a new Pos protocol schedule.
     *
     * @param besuProtocolSchedule The Besu protocol schedule.
     * @param context The protocol context (Blockchain, WorldState) used for state-dependent checks.
     */
    public PosProtocolSchedule(
            final ProtocolSchedule besuProtocolSchedule, final ProtocolContext context) {
        this.besuProtocolSchedule = besuProtocolSchedule;
        this.context = context;
    }

    /**
     * Gets the PoS-specific block importer.
     * This wrapper handles LCR (Largest Chain Rule) logic during block import.
     *
     * @param header the PoS block header
     * @return the PoS block importer
     */
    public BlockImporter getBlockImporter(final BlockHeader header) {
        return getProtocolSpecByBlockHeader(header).getBlockImporter();

    }

    /**
     * Gets the PoS-specific block validator.
     * This validator is responsible for Phase 5 checks:
     * 1. Verify the DSS (ECDSA) signature of the block.
     * 2. Verify the FTS (Follow-the-Satoshi) leader selection for the specific slot.
     *
     * @param header the PoS block header
     * @return the PoS block validator
     */
    public PosBlockValidator getBlockValidator(final BlockHeader header) {
        return new PosBlockValidator(
                getProtocolSpecByBlockHeader(header).getBlockValidator(), context);
    }

    /**
     * Retrieves the underlying Besu ProtocolSpec based on the block number/header.
     *
     * @param header the PoS block header
     * @return the ProtocolSpec
     */
    private ProtocolSpec getProtocolSpecByBlockHeader(final BlockHeader header) {
        // Unwrap the PoS header to get the standard Besu header for schedule lookup
        return besuProtocolSchedule.getByBlockHeader(header);
    }
}