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
package org.hyperledger.besu.consensus.pos.statemachine;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.config.PosConfigOptions;
import org.hyperledger.besu.consensus.pos.core.Node;
import org.hyperledger.besu.consensus.pos.core.NodeSet;
import org.hyperledger.besu.crypto.Hash;
import org.hyperledger.besu.cryptoservices.NodeKey;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.core.Util;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Log4j2
@Setter
@Getter
public class PosProposerSelector {

    private final NodeSet nodeSet;     // all validators
    private final NodeKey nodeKey;     // local validator’s key
    private final PosConfigOptions posConfigOptions;

    // In Pure PoS / LCR, the "previousSeed" acts as the entropy source (Parent Block Hash)
    private Bytes32 previousSeed;

    private Optional<Address> currentLeader = Optional.empty();

    public PosProposerSelector(final NodeSet nodeSet,
                               final NodeKey nodeKey,
                               final long selfStake,
                               PosConfigOptions posConfigOptions) {
        this.nodeSet = Objects.requireNonNull(nodeSet);
        this.nodeKey = Objects.requireNonNull(nodeKey);
        this.posConfigOptions = posConfigOptions;
    }

    /**
     * Follow-the-Satoshi (FTS) Leader Selection.
     * Selects a leader deterministically based on the parent block hash (previousSeed),
     * the round number, and the stake distribution.
     *
     * @param round the current round/slot number relative to the parent block.
     * @return The Address of the selected leader.
     */
    public Address getLeader(final long round) {
        // 1. Calculate Total Stake
        BigInteger totalStake = BigInteger.ZERO;
        List<Node> sortedValidators = nodeSet.getAllNodes().stream()
                .sorted(Comparator.comparing(Node::getAddress))
                .toList();

        for (Node node : sortedValidators) {
            totalStake = totalStake.add(BigInteger.valueOf(node.getStakeInfo().getStakedAmount()));
        }

        if (totalStake.equals(BigInteger.ZERO)) {
            log.warn("Total stake is zero. Cannot select leader.");
            return null;
        }

        // 2. Generate Randomness using the stored previousSeed (Parent Hash)
        Bytes32 entropy = computeEntropy(previousSeed, round);
        BigInteger randomValue = entropy.toUnsignedBigInteger();

        // 3. Determine the winning "Satoshi"
        BigInteger winningTicket = randomValue.mod(totalStake);

        // 4. Find the validator holding that ticket
        BigInteger accumulator = BigInteger.ZERO;
        for (Node node : sortedValidators) {
            BigInteger nodeStake = BigInteger.valueOf(node.getStakeInfo().getStakedAmount());
            accumulator = accumulator.add(nodeStake);

            if (accumulator.compareTo(winningTicket) > 0) {
                log.debug("FTS Selection: Round {}, Ticket {}, Leader {}", round, winningTicket, node.getAddress());
                return node.getAddress();
            }
        }

        log.error("FTS Selection failed to find a leader (Logic Error).");
        return null;
    }


    public Bytes32 getSeedAtRound(final long round, final Bytes32 prevBlockHash) {
        return computeEntropy(prevBlockHash, round);
    }

    /**
     * Computes the deterministic entropy for a specific round based on the parent block hash.
     */
    private Bytes32 computeEntropy(Bytes32 parentHash, long round) {
        final ByteBuffer roundBuffer = ByteBuffer.allocate(Long.BYTES).putLong(round);
        roundBuffer.flip();

        Bytes input = Bytes.concatenate(
                parentHash != null ? parentHash : Bytes32.ZERO,
                Bytes.wrap(roundBuffer.array())
        );
        return Hash.keccak256(input);
    }

    public Optional<Address> getCurrentProposer() {
        return currentLeader;
    }

    public boolean isLocalProposer() {
        Address localAddress = Util.publicKeyToAddress(nodeKey.getPublicKey());
        return currentLeader.isPresent() && localAddress.equals(currentLeader.get());
    }

}