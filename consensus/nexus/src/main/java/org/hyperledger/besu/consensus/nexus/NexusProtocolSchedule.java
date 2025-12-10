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
package org.hyperledger.besu.consensus.nexus;
import org.hyperledger.besu.consensus.nexus.core.NexusBlockHeader;
import org.hyperledger.besu.ethereum.ProtocolContext;
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule;
import org.hyperledger.besu.ethereum.mainnet.ProtocolSpec;

/**
 * Adaptor class to allow a {@link ProtocolSchedule} to be used as a {@link NexusProtocolSchedule}.
 */
public class NexusProtocolSchedule{

  private final ProtocolSchedule besuProtocolSchedule;
  private final ProtocolContext context;

  /**
   * Constructs a new Nexus protocol schedule.
   *
   * @param besuProtocolSchedule The Besu protocol schedule.
   * @param context The protocol context.
   */
  public NexusProtocolSchedule(
      final ProtocolSchedule besuProtocolSchedule, final ProtocolContext context) {
      this.besuProtocolSchedule = besuProtocolSchedule;
    this.context = context;
  }

  public NexusBlockImporter getBlockImporter(final NexusBlockHeader header) {
    return new NexusBlockImporter(
        getProtocolSpecByBlockHeader(header).getBlockImporter(), context);
  }


  public NexusBlockValidator getBlockValidator(final NexusBlockHeader header) {
    return new NexusBlockValidator(
        getProtocolSpecByBlockHeader(header).getBlockValidator(), context);
  }

  private ProtocolSpec getProtocolSpecByBlockHeader(final NexusBlockHeader header) {
    return besuProtocolSchedule.getByBlockHeader(BlockUtil.toBesuBlockHeader(header));
  }
}
