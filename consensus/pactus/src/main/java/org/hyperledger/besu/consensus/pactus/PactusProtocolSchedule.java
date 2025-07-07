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
package org.hyperledger.besu.consensus.pactus;


import org.hyperledger.besu.consensus.pactus.core.PactusBlockHeader;
import org.hyperledger.besu.consensus.pactus.validation.PactusBlockValidator;
import org.hyperledger.besu.ethereum.ProtocolContext;
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule;
import org.hyperledger.besu.ethereum.mainnet.ProtocolSpec;

/**
 * Adaptor class to allow a {@link ProtocolSchedule} to be used as a {@link PactusProtocolSchedule}.
 */
public class PactusProtocolSchedule {

  private final ProtocolSchedule besuProtocolSchedule;
  private final ProtocolContext context;

  /**
   * Constructs a new Pactus protocol schedule.
   *
   * @param besuProtocolSchedule The Besu protocol schedule.
   * @param context The protocol context.
   */
  public PactusProtocolSchedule(
      final ProtocolSchedule besuProtocolSchedule, final ProtocolContext context) {
    this.besuProtocolSchedule = besuProtocolSchedule;
    this.context = context;
  }

  public PactusBlockImporter getBlockImporter(final PactusBlockHeader header) {
    return new PactusBlockImporter(
        getProtocolSpecByBlockHeader(header).getBlockImporter(), context);
  }

  public PactusBlockValidator getBlockValidator(final PactusBlockHeader header) {
    return new PactusBlockValidator(
        getProtocolSpecByBlockHeader(header).getBlockValidator(), context);
  }

  private ProtocolSpec getProtocolSpecByBlockHeader(final PactusBlockHeader header) {
    return besuProtocolSchedule.getByBlockHeader(header.getBesuHeader());
  }
}
