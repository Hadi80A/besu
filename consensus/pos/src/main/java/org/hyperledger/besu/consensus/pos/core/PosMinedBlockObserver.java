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
package org.hyperledger.besu.consensus.pos.core;

import org.hyperledger.besu.ethereum.core.Block;

/**
 * Observer for mined (created) blocks in the PoS consensus.
 *
 * <p>Implemented by components that need to react to new local blocks,
 * such as the BlockPropagator (to gossip to peers) or the TransactionPool (to remove included txs).
 */
public interface PosMinedBlockObserver {

    /**
     * Called when a block is mined/created by the local node.
     *
     * @param block the mined block
     */
    void blockMined(Block block);
}