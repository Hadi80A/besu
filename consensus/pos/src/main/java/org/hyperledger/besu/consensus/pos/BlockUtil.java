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

public class BlockUtil {

  /** Private constructor to prevent instantiation. */
  private BlockUtil() {}


  public static Block toBesuBlock(final PosBlock block) {
    if (block != null) {
      return block.getBesuBlock();
    } else {
      throw new IllegalArgumentException("Unsupported block type");
    }
  }


  public static BlockHeader toBesuBlockHeader(final PosBlockHeader header) {
    if (header != null) {
      return header.getBesuBlockHeader();
    } else {
      throw new IllegalArgumentException("Unsupported block header type");
    }
  }
}
