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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.payload.Payload;
import org.hyperledger.besu.consensus.pos.messagedata.PosMessage;
import org.hyperledger.besu.crypto.SECPSignature;
import org.hyperledger.besu.crypto.SignatureAlgorithmFactory;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.rlp.RLPInput;
import org.hyperledger.besu.ethereum.rlp.RLPOutput;

import java.util.Objects;
import java.util.StringJoiner;
//@AllArgsConstructor
@Getter
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
/** The Vote payload. */
public class VotePayload extends PosPayload {
  private static final int TYPE = PosMessage.VOTE.getCode();
  private final Hash digest;

  /**
   * Default constructor.
   *
   * @param roundIdentifier
   * @param height
   */
  protected VotePayload(ConsensusRoundIdentifier roundIdentifier, long height, Hash digest) {
    super(roundIdentifier, height);
    this.digest = digest;
  }

  /**
   * Read from rlp input and return vote payload.
   *
   * @param rlpInput the rlp input
   * @return the vote payload
   */
  public static VotePayload readFrom(final RLPInput rlpInput) {
    rlpInput.enterList();
    final ConsensusRoundIdentifier roundIdentifier = ConsensusRoundIdentifier.readFrom(rlpInput);
    final Hash digest = Payload.readDigest(rlpInput);
    final long height = rlpInput.readLong();

    rlpInput.leaveList();

    return new VotePayload(roundIdentifier,height,digest);
  }

  @Override
  public void writeTo(final RLPOutput rlpOutput) {
    rlpOutput.startList();
    getRoundIdentifier().writeTo(rlpOutput);
    rlpOutput.writeBytes(digest);
    rlpOutput.writeLong(getHeight());
    rlpOutput.endList();
  }

  @Override
  public int getMessageType() {
    return TYPE;
  }

//  @Override
//  public ConsensusRoundIdentifier getRoundIdentifier() {
//    return roundIdentifier;
//  }

//  @Override
//  public boolean equals(final Object o) {
//    if (this == o) {
//      return true;
//    }
//    if (o == null || getClass() != o.getClass()) {
//      return false;
//    }
//    final VotePayload that = (VotePayload) o;
//    return Objects.equals(roundIdentifier, that.roundIdentifier)
//        && Objects.equals(digest, that.digest);
//  }

//  @Override
//  public int hashCode() {
//    return Objects.hash(roundIdentifier, digest);
//  }
//
//  @Override
//  public String toString() {
//    return new StringJoiner(", ", VotePayload.class.getSimpleName() + "[", "]")
//        .add("roundIdentifier=" + roundIdentifier)
//        .add("digest=" + digest)
//        .toString();
//  }
}
