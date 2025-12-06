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

import org.hyperledger.besu.consensus.pos.messagewrappers.Propose;
import org.hyperledger.besu.ethereum.BlockValidator;
import org.hyperledger.besu.ethereum.ProtocolContext;
import org.hyperledger.besu.ethereum.core.Block;
import org.hyperledger.besu.ethereum.mainnet.HeaderValidationMode;
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Message validator.
 *
 * <p>Phase 5: Verification
 * Responsible for validating the content of consensus messages.
 * For LCR, this primarily involves ensuring the Block within a Proposal is valid
 * (EVM execution, Gas, Roots, Header Signatures) before it is considered for import.
 */
public class MessageValidator {

    private static final Logger LOG = LoggerFactory.getLogger(MessageValidator.class);

    private final ProtocolSchedule protocolSchedule;
    private final ProtocolContext protocolContext;

    public MessageValidator(
            final ProtocolContext protocolContext,
            final ProtocolSchedule protocolSchedule) {
        this.protocolSchedule = protocolSchedule;
        this.protocolContext = protocolContext;
    }

    /**
     * Validates a Proposal message.
     * Checks if the embedded block is semantically valid according to the protocol rules.
     *
     * @param msg the Propose message containing the block
     * @return true if the block is valid, false otherwise
     */
    public boolean validateProposal(final Propose msg) {
        // Extract the Besu Block from the PoS Message Wrapper
        final Block block = msg.getSignedPayload().getPayload().getProposedBlock();

        // Validate the block content (Transactions, State Root, Receipts)
        // without persisting it to the blockchain yet.
        return validateBlockWithoutPersisting(block);
    }

    private boolean validateBlockWithoutPersisting(final Block block) {
        // Retrieve the validator for this specific block height (handles Hardforks)
        final BlockValidator blockValidator =
                protocolSchedule.getByBlockHeader(block.getHeader()).getBlockValidator();

        // HeaderValidationMode.FULL ensures that Phase 5 checks (DSS/FTS) in the header are verified.
        // persist = false ensures we don't change state yet.
        final var validationResult =
                blockValidator.validateAndProcessBlock(
                        protocolContext, block, HeaderValidationMode.LIGHT, HeaderValidationMode.FULL, false);

        if (validationResult.isFailed()) {
            LOG.info(
                    "Invalid Proposal message, block did not pass validation. Reason: {}",
                    validationResult.errorMessage);
            return false;
        }

        return true;
    }
}