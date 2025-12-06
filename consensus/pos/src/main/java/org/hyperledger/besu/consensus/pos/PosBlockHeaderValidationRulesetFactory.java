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
package org.hyperledger.besu.consensus.pos;

import static org.hyperledger.besu.ethereum.mainnet.AbstractGasLimitSpecification.DEFAULT_MAX_GAS_LIMIT;
import static org.hyperledger.besu.ethereum.mainnet.AbstractGasLimitSpecification.DEFAULT_MIN_GAS_LIMIT;

import org.hyperledger.besu.consensus.common.bft.BftHelpers;
import org.hyperledger.besu.consensus.common.bft.headervalidationrules.BftCoinbaseValidationRule;
import org.hyperledger.besu.consensus.pos.validation.ValidateHeightForBlock;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.mainnet.BlockHeaderValidator;
import org.hyperledger.besu.ethereum.mainnet.feemarket.BaseFeeMarket;
import org.hyperledger.besu.ethereum.mainnet.headervalidationrules.AncestryValidationRule;
import org.hyperledger.besu.ethereum.mainnet.headervalidationrules.ConstantFieldValidationRule;
import org.hyperledger.besu.ethereum.mainnet.headervalidationrules.GasLimitRangeAndDeltaValidationRule;
import org.hyperledger.besu.ethereum.mainnet.headervalidationrules.GasUsageValidationRule;
import org.hyperledger.besu.ethereum.mainnet.headervalidationrules.TimestampBoundedByFutureParameter;
import org.hyperledger.besu.ethereum.mainnet.headervalidationrules.TimestampMoreRecentThanParent;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.tuweni.units.bigints.UInt256;

/**
 * The PoS block header validation ruleset factory.
 * Configures the static checks for Phase 5 (Verification).
 */
public class PosBlockHeaderValidationRulesetFactory {

    /** Default constructor. */
    private PosBlockHeaderValidationRulesetFactory() {}

    public static BlockHeaderValidator.Builder blockHeaderValidator(
            final Duration minimumTimeBetweenBlocks,
            final Optional<BaseFeeMarket> baseFeeMarket,
            final Supplier<Blockchain> blockchainProvider) {

        final BlockHeaderValidator.Builder ruleBuilder =
                new BlockHeaderValidator.Builder()
                        // 1. Basic Ancestry (Parent Hash check)
                        .addRule(new AncestryValidationRule())

                        // 2. Gas Validity
                        .addRule(new GasUsageValidationRule())
                        .addRule(
                                new GasLimitRangeAndDeltaValidationRule(
                                        DEFAULT_MIN_GAS_LIMIT, DEFAULT_MAX_GAS_LIMIT, baseFeeMarket))

                        // 3. Timestamp Validity (Phase 2: Time Slot)
                        .addRule(new TimestampBoundedByFutureParameter(1))

                        // 4. Fixed Fields (Non-PoW compliance)
                        .addRule(
                                new ConstantFieldValidationRule<>(
                                        "MixHash", BlockHeader::getMixHash, BftHelpers.EXPECTED_MIX_HASH))
                        .addRule(
                                new ConstantFieldValidationRule<>(
                                        "OmmersHash", BlockHeader::getOmmersHash, Hash.EMPTY_LIST_HASH))

                        // 5. Difficulty (LCR Weight)
                        // Fixed to 1. This means "Cumulative Difficulty" == "Chain Length".
                        .addRule(
                                new ConstantFieldValidationRule<>(
                                        "Difficulty", BlockHeader::getDifficulty, UInt256.ONE))
                        .addRule(new ConstantFieldValidationRule<>("Nonce", BlockHeader::getNonce, 0L))

                        // 6. DSS Authentication (Phase 5)
                        // Verifies that the 'Coinbase' matches the signer recovered from the ECDSA signature in extraData.
                        .addRule(new BftCoinbaseValidationRule());

        // 7. Time Slot Enforcement
        // Ensures block timestamp strictly progresses relative to parent.
        if (minimumTimeBetweenBlocks.compareTo(Duration.ofSeconds(1)) >= 0) {
            ruleBuilder.addRule(new TimestampMoreRecentThanParent(minimumTimeBetweenBlocks.getSeconds() / 5));
        }

        // 8. Chain Continuity
        ruleBuilder.addRule(new ValidateHeightForBlock(blockchainProvider));

        return ruleBuilder;
    }
}