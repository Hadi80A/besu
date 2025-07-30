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

import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.payload.Payload;
import org.hyperledger.besu.consensus.pos.messagedata.Pos;
import org.hyperledger.besu.crypto.SECPSignature;
import org.hyperledger.besu.crypto.SignatureAlgorithmFactory;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.rlp.RLPInput;
import org.hyperledger.besu.ethereum.rlp.RLPOutput;

import java.util.Objects;
import java.util.StringJoiner;

/** The Vote payload. */
public class VotePayload extends PosPayload {
  private static final int TYPE = Pos.COMMIT;
  private final ConsensusRoundIdentifier roundIdentifier;
  private final Hash digest;
  private final SECPSignature voteSeal;

  /**
   * Instantiates a new Vote payload.
   *
   * @param roundIdentifier the round identifier
   * @param digest the digest
   * @param voteSeal the vote seal
   */
  public VotePayload(
      final ConsensusRoundIdentifier roundIdentifier,
      final Hash digest,
      final SECPSignature voteSeal) {
    this.roundIdentifier = roundIdentifier;
    this.digest = digest;
    this.voteSeal = voteSeal;
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
    final SECPSignature voteSeal =
        rlpInput.readBytes(SignatureAlgorithmFactory.getInstance()::decodeSignature);
    rlpInput.leaveList();

    return new VotePayload(roundIdentifier, digest, voteSeal);
  }

  @Override
  public void writeTo(final RLPOutput rlpOutput) {
    rlpOutput.startList();
    roundIdentifier.writeTo(rlpOutput);
    rlpOutput.writeBytes(digest);
    rlpOutput.writeBytes(voteSeal.encodedBytes());
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
   * Gets vote seal.
   *
   * @return the vote seal
   */
  public SECPSignature getVoteSeal() {
    return voteSeal;
  }

  @Override
  public ConsensusRoundIdentifier getRoundIdentifier() {
    return roundIdentifier;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final VotePayload that = (VotePayload) o;
    return Objects.equals(roundIdentifier, that.roundIdentifier)
        && Objects.equals(digest, that.digest)
        && Objects.equals(voteSeal, that.voteSeal);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roundIdentifier, digest, voteSeal);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", VotePayload.class.getSimpleName() + "[", "]")
        .add("roundIdentifier=" + roundIdentifier)
        .add("digest=" + digest)
        .add("voteSeal=" + voteSeal)
        .toString();
  }
}
