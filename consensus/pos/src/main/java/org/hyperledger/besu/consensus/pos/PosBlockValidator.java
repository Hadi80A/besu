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

import org.hyperledger.besu.ethereum.BlockProcessingResult;
import org.hyperledger.besu.ethereum.BlockValidator;
import org.hyperledger.besu.ethereum.ProtocolContext;
import org.hyperledger.besu.ethereum.core.Block;
import org.hyperledger.besu.ethereum.mainnet.HeaderValidationMode;

import java.util.Optional;

/** Adaptor class to allow a {@link BlockValidator} to be used as a {@link PosBlockValidator}. */
public class PosBlockValidator {

    private final BlockValidator blockValidator;
    private final ProtocolContext protocolContext;

    /**
     * Constructs a new Pos block validator
     *
     * @param blockValidator The Besu block validator
     * @param protocolContext The protocol context
     */
    public PosBlockValidator(
            final BlockValidator blockValidator, final ProtocolContext protocolContext) {
        this.blockValidator = blockValidator;
        this.protocolContext = protocolContext;
    }


    public ValidationResult validateBlock(final Block block) {
        final BlockProcessingResult blockProcessingResult =
                blockValidator.validateAndProcessBlock(
                        protocolContext,
                        block,
                        HeaderValidationMode.LIGHT,
                        HeaderValidationMode.FULL,
                        false);
        return new ValidationResult(
                blockProcessingResult.isSuccessful(), blockProcessingResult.errorMessage);
    }

    /**
     * The result of a block validation.
     *
     * @param success whether the validation was successful
     * @param errorMessage the error message if the validation was not successful
     */
    record ValidationResult(boolean success, Optional<String> errorMessage) {}
}
