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
package org.hyperledger.besu.consensus.pactus.payload;

import lombok.Data;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.payload.Payload;
import org.hyperledger.besu.consensus.pactus.messagedata.PactusMessage;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.rlp.RLPInput;
import org.hyperledger.besu.ethereum.rlp.RLPOutput;

import java.io.IOException;
import java.util.Objects;

import com.google.common.base.MoreObjects;

/** The Round change payload. */
@Data

public class DecidePayload extends PactusPayload{
  private static final int TYPE = PactusMessage.DECIDE.getCode();
//  private final ConsensusRoundIdentifier roundChangeIdentifier;
  private final int round_cp;
  private final int b;

  public DecidePayload(int round,int height , int roundCp, int b){
    super(round, height);
    this.round_cp=roundCp;
    this.b=b;
  }

  @Override
  public ConsensusRoundIdentifier getRoundIdentifier() {
    return null;
  }

  @Override
  public void writeTo(final RLPOutput rlpOutput) {
    rlpOutput.writeInt(round);
    rlpOutput.writeInt(height);
    rlpOutput.writeInt(round_cp);
    rlpOutput.writeInt(b);
  }

  public static DecidePayload readFrom(final RLPInput rlpInput)  {
    int round = rlpInput.readInt();
    int height = rlpInput.readInt();
    int round_cp = rlpInput.readInt();
    int b = rlpInput.readInt();
    return new DecidePayload(round,height,round_cp,b);
  }

  @Override
  public int getMessageType() {
    return TYPE;
  }

  @Override
  public Hash hashForSignature() {
    String data=TYPE + "|"+round+"|"+height+"|" +round_cp+"|" +b ;
    return Hash.hash(Bytes.wrap(data.getBytes()));
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

//  @Override
//  public boolean equals(final Object o) {
//    if (this == o) {
//      return true;
//    }
//    if (o == null || getClass() != o.getClass()) {
//      return false;
//    }
//    ChangeProposerPayload that = (ChangeProposerPayload) o;
//    return Objects.equals(roundChangeIdentifier);
//  }
//
//  @Override
//  public int hashCode() {
//    return Objects.hash(roundChangeIdentifier);
//  }
//
//  @Override
//  public String toString() {
//    return MoreObjects.toStringHelper(this)
//        .add("roundChangeIdentifier", roundChangeIdentifier)
//        .toString();
//  }
}
