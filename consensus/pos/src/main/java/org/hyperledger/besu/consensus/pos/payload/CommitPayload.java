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

import lombok.EqualsAndHashCode;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.payload.Payload;
import org.hyperledger.besu.consensus.pos.core.PosBlock;
import org.hyperledger.besu.consensus.pos.messagedata.PosMessage;
import org.hyperledger.besu.crypto.SECPSignature;
import org.hyperledger.besu.crypto.SignatureAlgorithmFactory;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.rlp.RLPInput;
import org.hyperledger.besu.ethereum.rlp.RLPOutput;

import java.util.Objects;
import java.util.StringJoiner;

/** The Commit payload. */
@EqualsAndHashCode(callSuper = true)
public class CommitPayload extends PosPayload {
  private static final int TYPE = PosMessage.BLOCK_ANNOUNCE.getCode();
//  private final ConsensusRoundIdentifier roundIdentifier;
  private final PosBlock block;
//  private final SECPSignature commitSeal;//todo

  public CommitPayload(ConsensusRoundIdentifier roundIdentifier, long height,PosBlock block) {
    super(roundIdentifier, height);
    this.block = block;
  }

  /**
   * Read from rlp input and return commit payload.
   *
   * @param rlpInput the rlp input
   * @return the commit payload
   */
  public static CommitPayload readFrom(final RLPInput rlpInput) {
    rlpInput.enterList();
    final ConsensusRoundIdentifier roundIdentifier = ConsensusRoundIdentifier.readFrom(rlpInput);
    final Hash digest = Payload.readDigest(rlpInput);
    final SECPSignature commitSeal =
        rlpInput.readBytes(SignatureAlgorithmFactory.getInstance()::decodeSignature);
    rlpInput.leaveList();

    return new CommitPayload(roundIdentifier,height,block);
  }

  @Override
  public void writeTo(final RLPOutput rlpOutput) {
    rlpOutput.startList();
    roundIdentifier.writeTo(rlpOutput);
    rlpOutput.writeBytes(digest);
    rlpOutput.writeBytes(commitSeal.encodedBytes());
    rlpOutput.endList();
  }

  @Override
  public int getMessageType() {
    return TYPE;
  }

  /**
   * Gets digest.
   *
   * @return the digest
   */
  public Hash getDigest() {
    return digest;
  }

  /**
   * Gets commit seal.
   *
   * @return the commit seal
   */
  public SECPSignature getCommitSeal() {
    return commitSeal;
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
//    final CommitPayload that = (CommitPayload) o;
//    return Objects.equals(
////            roundIdentifier, that.roundIdentifier)
//            Objects.equals(digest, that.digest)
//        && Objects.equals(commitSeal, that.commitSeal);
//  }

//  @Override
//  public int hashCode() {
//    return Objects.hash( digest, commitSeal);
//  }
//
//  @Override
//  public String toString() {
//    return new StringJoiner(", ", CommitPayload.class.getSimpleName() + "[", "]")
////        .add("roundIdentifier=" + roundIdentifier)
//        .add("digest=" + digest)
//        .add("commitSeal=" + commitSeal)
//        .toString();
//  }
}
