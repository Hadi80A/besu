/*
 * Copyright 2020 ConsenSys AG.
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
package org.hyperledger.besu.consensus.pos.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.payload.Payload;
import org.hyperledger.besu.datatypes.Hash;

import org.apache.tuweni.bytes.Bytes;

/** The Pos payload. */

@AllArgsConstructor
@Getter
public abstract class PosPayload implements Payload {
  private final ConsensusRoundIdentifier roundIdentifier;
  private final long height;
  /** Default constructor. */
//  protected PosPayload(ConsensusRoundIdentifier roundIdentifier, long height) {
//      this.roundIdentifier = roundIdentifier;
//      this.height = height;
//  }


  @Override
  public Hash hashForSignature() {
    return Hash.hash(Bytes.concatenate(Bytes.of(getMessageType()), encoded()));
  }
}
