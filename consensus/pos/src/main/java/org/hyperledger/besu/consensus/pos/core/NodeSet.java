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

import org.hyperledger.besu.datatypes.Address;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class NodeSet {

    // Map of validator Address to Node object
    private final Map<Address, Node> validatorMap = new ConcurrentHashMap<>();

    /**
     * Adds or updates a node in the set.
     * @param node the node information
     */
    public void addOrUpdateNode(final Node node) {
        validatorMap.put(node.getAddress(), node);
    }

    public Optional<Node> getNode(final Address address) {
        return Optional.ofNullable(validatorMap.get(address));
    }

    public Collection<Node> getAllNodes() {
        return validatorMap.values();
    }

    /**
     * Returns all nodes sorted by Address.
     * Essential for deterministic consensus operations (e.g., FTS Leader Selection).
     *
     * @return List of nodes sorted by Address.
     */
    public List<Node> getSortedNodes() {
        return validatorMap.values().stream()
                .sorted(Comparator.comparing(Node::getAddress))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a node by its index in the sorted list of validators.
     * Used for mapping bitfields in messages to specific validators.
     *
     * @param index the 0-based index
     * @return the Node at that index
     */
    public Node getNodeByIndex(final int index) {
        List<Node> sorted = getSortedNodes();
        if (index < 0 || index >= sorted.size()) {
            throw new IndexOutOfBoundsException("Validator index " + index + " out of bounds (size: " + sorted.size() + ")");
        }
        return sorted.get(index);
    }

    public List<Node> getCommitteeNodes() {
        // In simple LCR/FTS, effectively all staked nodes are eligible,
        // but we retain the 'inCommittee' flag for potential optimization or rotation logic.
        return validatorMap.values().stream()
                .filter(Node::isInCommittee)
                .collect(Collectors.toList());
    }

    public List<Node> getNonCommitteeNodes() {
        return validatorMap.values().stream()
                .filter(v -> !v.isInCommittee())
                .collect(Collectors.toList());
    }

    public int committeeSize() {
        return (int) validatorMap.values().stream().filter(Node::isInCommittee).count();
    }

    public int totalSize() {
        return validatorMap.size();
    }
}