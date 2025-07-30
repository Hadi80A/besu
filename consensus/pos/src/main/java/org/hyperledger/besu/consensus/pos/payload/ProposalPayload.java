/*
 * Copyright ConsenSys AG.
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
import org.hyperledger.besu.consensus.pos.core.PosBlock;
import org.hyperledger.besu.consensus.pos.messagedata.Pos;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.rlp.RLPInput;
import org.hyperledger.besu.ethereum.rlp.RLPOutput;

import java.util.Objects;
import java.util.StringJoiner;

/** The Proposal payload. */
@AllArgsConstructor
@Getter
public class ProposalPayload extends PosPayload {
  private static final int TYPE = Pos.PROPOSAL;
  private final ConsensusRoundIdentifier roundIdentifier;
  private long height = -1;
//  private vrf leader

  /**
   * Read from rlp input and return proposal payload.
   *
   * @param rlpInput the rlp input
   * @return the proposal payload
   */
  public static ProposalPayload readFrom(final RLPInput rlpInput) {
    rlpInput.enterList();
    final ConsensusRoundIdentifier roundIdentifier = ConsensusRoundIdentifier.readFrom(rlpInput);
    final long height = rlpInput.readLong();
    rlpInput.leaveList();

    return new ProposalPayload(roundIdentifier,height);
  }

  @Override
  public void writeTo(final RLPOutput rlpOutput) {
    rlpOutput.startList();
    roundIdentifier.writeTo(rlpOutput);
    rlpOutput.writeLong(height);
    rlpOutput.endList();
  }

  /**
   * Gets digest.
   *
   * @return the digest
   */

  @Override
  public int getMessageType() {
    return TYPE;
  }


  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final ProposalPayload that = (ProposalPayload) o;
    return Objects.equals(roundIdentifier, that.roundIdentifier)
        && Objects.equals(height, that.height);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roundIdentifier, height);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", ProposalPayload.class.getSimpleName() + "[", "]")
        .add("roundIdentifier=" + roundIdentifier)
        .add("digest=" + height)
        .toString();
  }
}
