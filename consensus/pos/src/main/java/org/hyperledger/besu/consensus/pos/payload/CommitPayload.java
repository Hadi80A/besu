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
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.payload.Payload;
import org.hyperledger.besu.consensus.pos.messagedata.PosMessage;
import org.hyperledger.besu.consensus.pos.statemachine.QuorumCertificate;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.rlp.RLPInput;
import org.hyperledger.besu.ethereum.rlp.RLPOutput;

/** The Commit payload. */
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Getter
public class CommitPayload extends PosPayload {
  private static final int TYPE = PosMessage.COMMIT.getCode();

  private final Hash digest;
  private final QuorumCertificate quorumCertificate;
//  private final byte[] blsAggregate;

  public CommitPayload(ConsensusRoundIdentifier roundIdentifier, long height , Hash digest, QuorumCertificate quorumCertificate) {
    super(roundIdentifier, height);
      this.digest = digest;
      this.quorumCertificate = quorumCertificate;
  }

  /**
   * Read from rlp input and return commit payload.
   *
   * @param rlpInput the rlp input
   * @return the commit payload
   */
  public static CommitPayload readFrom(final RLPInput rlpInput) {
    rlpInput.enterList();
    long height = rlpInput.readLong();
    final ConsensusRoundIdentifier roundIdentifier = ConsensusRoundIdentifier.readFrom(rlpInput);
    final Hash digest = Payload.readDigest(rlpInput);

    QuorumCertificate quorumCertificate = QuorumCertificate.readFrom(rlpInput);
    rlpInput.leaveList();

    return new CommitPayload(roundIdentifier,height,digest,quorumCertificate);
  }

  @Override
  public void writeTo(final RLPOutput rlpOutput) {
    rlpOutput.startList();
    rlpOutput.writeLong(height);
    getRoundIdentifier().writeTo(rlpOutput);
    rlpOutput.writeBytes(digest);
    quorumCertificate.writeTo(rlpOutput);
    rlpOutput.endList();
  }

  @Override
  public int getMessageType() {
    return TYPE;
  }
}
