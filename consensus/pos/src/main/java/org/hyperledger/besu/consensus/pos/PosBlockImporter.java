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
import org.hyperledger.besu.ethereum.ProtocolContext;
import org.hyperledger.besu.ethereum.core.BlockImporter;
import org.hyperledger.besu.ethereum.mainnet.BlockImportResult;
import org.hyperledger.besu.ethereum.mainnet.HeaderValidationMode;

/** Adaptor class to allow a {@link BlockImporter} to be used as a {@link PosBlockImporter}. */
public class PosBlockImporter {

  private final BlockImporter blockImporter;
  private final ProtocolContext context;


  public PosBlockImporter(
      final BlockImporter blockImporter, final ProtocolContext context) {
    this.blockImporter = blockImporter;
    this.context = context;
  }

  public boolean importBlock(final PosBlock block) {
    final BlockImportResult blockImportResult =
        blockImporter.importBlock(context, BlockUtil.toBesuBlock(block), HeaderValidationMode.FULL);
    return blockImportResult.isImported();
  }
}
