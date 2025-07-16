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

import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.core.Block;

import java.util.Objects;

public class PosBlock {

  private final Block besuBlock;
  private final PosBlockHeader qbftBlockHeader;

  /**
   * Constructs a PosBlock from a Besu Block.
   *
   * @param besuBlock the Besu Block
   */
  public PosBlock(final Block besuBlock) {
    this.besuBlock = besuBlock;
    this.qbftBlockHeader = new PosBlockHeader(besuBlock.getHeader());
  }


  public PosBlockHeader getHeader() {
    return qbftBlockHeader;
  }


  public boolean isEmpty() {
    return besuBlock.getHeader().getTransactionsRoot().equals(Hash.EMPTY_TRIE_HASH);
  }

  /**
   * Returns the Besu Block associated with this PosBlock. Used to convert a PosBlock back to a
   * Besu block.
   *
   * @return the Besu Block
   */
  public Block getBesuBlock() {
    return besuBlock;
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof PosBlock qbftBlock)) return false;
    return Objects.equals(besuBlock, qbftBlock.besuBlock)
        && Objects.equals(qbftBlockHeader, qbftBlock.qbftBlockHeader);
  }

  @Override
  public int hashCode() {
    return Objects.hash(besuBlock, qbftBlockHeader);
  }

  public Hash getHash() {
    return getHeader().getHash();
  }
}
