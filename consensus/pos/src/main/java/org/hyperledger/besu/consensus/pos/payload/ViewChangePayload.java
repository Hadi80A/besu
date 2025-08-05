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

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.payload.Payload;
import org.hyperledger.besu.consensus.pos.core.PosBlock;
import org.hyperledger.besu.consensus.pos.messagedata.PosMessage;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.rlp.RLPInput;
import org.hyperledger.besu.ethereum.rlp.RLPOutput;

import java.io.IOException;
import java.util.Objects;

import com.google.common.base.MoreObjects;

@SuperBuilder
@EqualsAndHashCode(callSuper = false)
/** The Round change payload. */
public class ViewChangePayload extends PosPayload {
  private static final int TYPE = PosMessage.VIEW_CHANGE.getCode();

  protected ViewChangePayload(ConsensusRoundIdentifier roundIdentifier, long height  ) {
    super(roundIdentifier, height);
  }

  @Override
  public void writeTo(final RLPOutput rlpOutput) {
    rlpOutput.startList();
    getRoundIdentifier().writeTo(rlpOutput);
    rlpOutput.writeLong(getHeight());

    rlpOutput.endList();
  }

  /**
   * Read from rlp input and return round change payload.
   *
   * @param rlpInput the rlp input
   * @return the round change payload
   */
  public static ViewChangePayload readFrom(final RLPInput rlpInput) {
    rlpInput.enterList();
    final ConsensusRoundIdentifier roundIdentifier = ConsensusRoundIdentifier.readFrom(rlpInput);
    final long height = rlpInput.readLong();
    rlpInput.leaveList();

    return new ViewChangePayload(roundIdentifier,height);
  }

  @Override
  public int getMessageType() {
    return TYPE;
  }


  protected void writeConsensusRound(final RLPOutput out) {
    out.writeLongScalar(getRoundIdentifier().getSequenceNumber());
    out.writeIntScalar(getRoundIdentifier().getRoundNumber());
  }

  /**
   * Read consensus round.
   *
   * @param in the rlp input
   * @return the consensus round identifier
   */
  protected static ConsensusRoundIdentifier readConsensusRound(final RLPInput in) {
    return new ConsensusRoundIdentifier(in.readLongScalar(), in.readIntScalar());
  }

}