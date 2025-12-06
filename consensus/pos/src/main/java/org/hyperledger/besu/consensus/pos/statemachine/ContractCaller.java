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
package org.hyperledger.besu.consensus.pos.statemachine;

import lombok.AllArgsConstructor;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.crypto.Hash;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.ProtocolContext;
import org.hyperledger.besu.ethereum.core.Block;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.worldstate.WorldStateArchive;
import org.hyperledger.besu.evm.account.Account;
import org.hyperledger.besu.evm.worldstate.WorldState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

/**
 * Helper class to read Validator Stake from the World State.
 *
 * <p>Phase 1 (Initialization/Staking):
 * This class interfaces with the on-chain "Ledger of Stake". It assumes a specific 
 * Solidity storage layout: `mapping(address => uint256)` located at Storage Slot 0.
 */
@AllArgsConstructor
public class ContractCaller {

    private static final Logger LOG = LoggerFactory.getLogger(ContractCaller.class);

    private final Address contractAddress;
    private final ProtocolContext protocolContext;

    /**
     * Retrieves the stake amount for a validator at the state of a specific block.
     *
     * @param validatorAddress The address of the validator.
     * @param block The block whose post-state we are querying.
     * @return The stake amount in Wei (or basic units), or 0 if not found.
     */
    public BigInteger getValidatorStake(final Address validatorAddress, final Block block) {
        WorldStateArchive worldStateArchive = protocolContext.getWorldStateArchive();
        BlockHeader header = block.getHeader();

        // 1. Retrieve the World State for the given block
        WorldState worldState =
                worldStateArchive
                        .get(header.getStateRoot(), header.getHash())
                        .orElseThrow(() -> new RuntimeException(
                                "World State not available for block " + header.getNumber() + " (" + header.getHash() + ")"));

        // 2. Get the Stake Contract Account
        Account contractAccount = worldState.get(contractAddress);
        if (contractAccount == null || contractAccount.isEmpty()) {
            LOG.trace("Stake Contract {} is null or empty at block {}", contractAddress, header.getNumber());
            return BigInteger.ZERO;
        }

        // 3. Compute storage slot for validator's stake
        // Assumption: Solidity mapping(address => uint256) at Slot 0
        // Formula: keccak256(leftPad(address) . leftPad(slot_index))
        Bytes32 key = Bytes32.leftPad(validatorAddress);
        Bytes32 slotIndex = Bytes32.leftPad(Bytes.of(0)); // Slot 0
        Bytes concatenated = Bytes.concatenate(key, slotIndex);

        Bytes32 slotHash = Hash.keccak256(concatenated);

        // 4. Read storage value at computed slot
        UInt256 stakeValue =
                contractAccount.getStorageValue(UInt256.valueOf(slotHash.toUnsignedBigInteger()));

        // Handle null result (key not present in map)
        if (stakeValue == null) {
            return BigInteger.ZERO;
        }

        return stakeValue.toBigInteger();
    }
}