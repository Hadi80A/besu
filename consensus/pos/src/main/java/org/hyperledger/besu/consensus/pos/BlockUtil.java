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

import org.hyperledger.besu.consensus.pos.core.PosBlock;
import org.hyperledger.besu.consensus.pos.core.PosBlockHeader;
import org.hyperledger.besu.ethereum.core.Block;
import org.hyperledger.besu.ethereum.core.BlockHeader;

/** Utility class to convert between Besu and QBFT blocks. */
public class BlockUtil {

  /** Private constructor to prevent instantiation. */
  private BlockUtil() {}

  /**
   * Convert a QBFT block to a Besu block.
   *
   * @param block the QBFT block
   * @return the Besu block
   */
  public static Block toBesuBlock(final PosBlock block) {
    if (block instanceof PosBlock) {
      return ((PosBlock) block).getBesuBlock();
    } else {
      throw new IllegalArgumentException("Unsupported block type");
    }
  }

  /**
   * Convert a QBFT block header to a Besu block header.
   *
   * @param header the QBFT block header
   * @return the Besu block header
   */
  public static BlockHeader toBesuBlockHeader(final PosBlockHeader header) {
    if (header instanceof PosBlockHeader) {
      return ((PosBlockHeader) header).getBesuBlockHeader();
    } else {
      throw new IllegalArgumentException("Unsupported block header type");
    }
  }
}
