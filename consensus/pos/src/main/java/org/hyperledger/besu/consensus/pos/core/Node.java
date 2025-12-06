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
package org.hyperledger.besu.consensus.pos.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hyperledger.besu.crypto.SECPPublicKey;
import org.hyperledger.besu.datatypes.Address;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Node {

    /** Unique identifier for the validator (e.g., "Validator-1"). */
    private String id;

    /** The Ethereum address of the validator. */
    private Address address;

    /** The ECDSA public key used for DSS authentication. */
    @Setter
    private SECPPublicKey publicKey;

    /** Stake information associated with this validator. */
    private StakeInfo stakeInfo;

    /** Flag indicating whether this validator is currently active/eligible. */
    private boolean inCommittee;

    /** Number of blocks proposed by this validator (optional stat tracking). */
    private int blocksProposed;

    /** Timestamp or round count when the validator last proposed a block (for fair scheduling logic). */
    private long lastProposedAt;

    /**
     * Increases the validator's stake.
     * @param amount amount to add
     */
    public void addStake(long amount) {
        if (stakeInfo != null) {
            stakeInfo.setStakedAmount(stakeInfo.getStakedAmount() + amount);
        }
    }

    /**
     * Decreases the validator's stake.
     * @param amount amount to subtract
     */
    public void subStake(long amount) {
        if (stakeInfo != null) {
            long newStake = Math.max(0, stakeInfo.getStakedAmount() - amount);
            stakeInfo.setStakedAmount(newStake);
        }
    }

    /**
     * Updates the stake (logic depends on specific implementation, usually simplified to set/sub).
     * @param amount amount to adjust
     */
    public void updateStake(long amount) {
        // Typically used for penalties or re-calculations
        subStake(amount);
    }
}