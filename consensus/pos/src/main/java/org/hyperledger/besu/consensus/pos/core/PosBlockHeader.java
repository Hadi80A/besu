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

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.*;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.pos.util.SerializeUtil;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.rlp.RLPInput;
import org.hyperledger.besu.ethereum.rlp.RLPOutput;

import java.io.IOException;
import java.util.Objects;
@Data
@Builder
//@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PosBlockHeader{
  private BlockHeader besuHeader;
  private final ConsensusRoundIdentifier roundIdentifier;
  private Address proposer;



  public long getHeight() {
    return besuHeader.getNumber();

  }

  public long getTimestamp() {
    return besuHeader.getTimestamp();
  }

  public Address getCoinbase() {
    return besuHeader.getCoinbase();
  }

  public Hash getHash() {
    return besuHeader.getHash();
  }

  /**
   * Returns the Besu block header.
   *
   * @return the Besu block header.
   */
  public BlockHeader getBesuBlockHeader() {
    return besuHeader;
  }

  public void writeTo(RLPOutput rlpOutput) throws JsonProcessingException {
    besuHeader.writeTo(rlpOutput);
    rlpOutput.writeInt(roundIdentifier.getRoundNumber());
    rlpOutput.writeLong(roundIdentifier.getSequenceNumber());
    rlpOutput.writeBytes(SerializeUtil.toBytes(proposer));
  }

  public static PosBlockHeader readFrom(RLPInput rlpInput, BlockHeader besuHeader) throws IOException {
    ConsensusRoundIdentifier round = new ConsensusRoundIdentifier(rlpInput.readLong(),rlpInput.readInt());
    Address proposer=  SerializeUtil.toObject(rlpInput.readBytes(),Address.class);
    return new PosBlockHeader(besuHeader,round,proposer);
  }

}
