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
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.rlp.RLPInput;
import org.hyperledger.besu.ethereum.rlp.RLPOutput;

import java.io.IOException;

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
    rlpOutput.startList();
    // consensus round + sequence
    rlpOutput.writeInt(roundIdentifier.getRoundNumber());
    rlpOutput.writeLong(roundIdentifier.getSequenceNumber());

    // write raw address bytes (20 bytes)
    // use Bytes.fromHexString to convert "0x..." -> raw bytes
    rlpOutput.writeBytes(Bytes.fromHexString(proposer.toHexString()));

    rlpOutput.endList();
  }


  public static PosBlockHeader readFrom(RLPInput rlpInput, BlockHeader besuHeader) throws IOException {
    rlpInput.enterList();

    int roundNumber = rlpInput.readInt();
    long sequenceNumber = rlpInput.readLong();
    ConsensusRoundIdentifier round = new ConsensusRoundIdentifier(sequenceNumber, roundNumber);

    // read raw 20 bytes
    Bytes addressBytes = rlpInput.readBytes();
    // Prefer Address.wrap if available (no extra hex conversions)
    // If Address.wrap(Bytes) exists:
    // Address proposer = Address.wrap(addressBytes);
    //
    // Otherwise convert bytes -> hex string and use Address.fromHexString(...)
    Address proposer = Address.fromHexString(addressBytes.toHexString());

    rlpInput.leaveList();
    return new PosBlockHeader(besuHeader, round, proposer);
  }


}
