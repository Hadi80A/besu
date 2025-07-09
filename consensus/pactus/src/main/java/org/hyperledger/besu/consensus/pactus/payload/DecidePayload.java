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

import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.payload.Payload;
import org.hyperledger.besu.consensus.pactus.messagedata.PactusMessage;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.rlp.RLPInput;
import org.hyperledger.besu.ethereum.rlp.RLPOutput;

import java.util.Objects;

import com.google.common.base.MoreObjects;

/** The Round change payload. */
public class DecidePayload implements Payload {
  private static final int TYPE = PactusMessage.DECIDE.getCode();
  private final ConsensusRoundIdentifier roundChangeIdentifier;
  private final int round_cp;
  private final int b;
  /**
   * Instantiates a new Round change payload.
   *
   * @param roundChangeIdentifier the round change identifier
   */
  public DecidePayload(
          final ConsensusRoundIdentifier roundChangeIdentifier, int roundCp, int b){
    this.roundChangeIdentifier = roundChangeIdentifier;
      round_cp = roundCp;
      this.b = b;
  }

  @Override
  public ConsensusRoundIdentifier getRoundIdentifier() {
    return roundChangeIdentifier;
  }

  @Override
  public void writeTo(final RLPOutput rlpOutput) {
    // RLP encode of the message data content (round identifier and prepared certificate)
    rlpOutput.startList();
    writeConsensusRound(rlpOutput);

    rlpOutput.startList();
    rlpOutput.endList();

    rlpOutput.endList();
  }

  /**
   * Read from rlp input and return round change payload.
   *
   * @param rlpInput the rlp input
   * @return the round change payload
   */
  public static ChangeProposerPayload readFrom(final RLPInput rlpInput) {
    rlpInput.enterList();
    final ConsensusRoundIdentifier roundIdentifier = readConsensusRound(rlpInput);

    rlpInput.leaveList();
    return new ChangeProposerPayload(roundIdentifier);
  }

  @Override
  public int getMessageType() {
    return TYPE;
  }

  @Override
  public Hash hashForSignature() {
    String data= getMessageType() + "|"+round+"|"+height+"|"+blockHash.toHexString();
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

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ChangeProposerPayload that = (ChangeProposerPayload) o;
    return Objects.equals(roundChangeIdentifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roundChangeIdentifier);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("roundChangeIdentifier", roundChangeIdentifier)
        .toString();
  }
}
