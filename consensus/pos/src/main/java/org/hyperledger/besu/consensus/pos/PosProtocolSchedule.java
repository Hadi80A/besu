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
import org.hyperledger.besu.consensus.pos.core.PosBlockHeader;
import org.hyperledger.besu.ethereum.ProtocolContext;
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule;
import org.hyperledger.besu.ethereum.mainnet.ProtocolSpec;

/**
 * Adaptor class to allow a {@link ProtocolSchedule} to be used as a {@link PosProtocolSchedule}.
 */
public class PosProtocolSchedule{

  private final ProtocolSchedule besuProtocolSchedule;
  private final ProtocolContext context;

  /**
   * Constructs a new Pos protocol schedule.
   *
   * @param besuProtocolSchedule The Besu protocol schedule.
   * @param context The protocol context.
   */
  public PosProtocolSchedule(
      final ProtocolSchedule besuProtocolSchedule, final ProtocolContext context) {
      this.besuProtocolSchedule = besuProtocolSchedule;
    this.context = context;
  }

  public PosBlockImporter getBlockImporter(final PosBlockHeader header) {
    return new PosBlockImporter(
        getProtocolSpecByBlockHeader(header).getBlockImporter(), context);
  }


  public PosBlockValidator getBlockValidator(final PosBlockHeader header) {
    return new PosBlockValidator(
        getProtocolSpecByBlockHeader(header).getBlockValidator(), context);
  }

  private ProtocolSpec getProtocolSpecByBlockHeader(final PosBlockHeader header) {
    return besuProtocolSchedule.getByBlockHeader(BlockUtil.toBesuBlockHeader(header));
  }
}
