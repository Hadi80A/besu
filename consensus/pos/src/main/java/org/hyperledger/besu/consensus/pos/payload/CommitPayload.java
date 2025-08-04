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
import lombok.SneakyThrows;
import lombok.experimental.SuperBuilder;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.payload.Payload;
import org.hyperledger.besu.consensus.pos.core.PosBlock;
import org.hyperledger.besu.consensus.pos.messagedata.PosMessage;
import org.hyperledger.besu.crypto.SECPSignature;
import org.hyperledger.besu.crypto.SignatureAlgorithmFactory;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.core.Block;
import org.hyperledger.besu.ethereum.rlp.RLPInput;
import org.hyperledger.besu.ethereum.rlp.RLPOutput;

import java.util.Objects;
import java.util.StringJoiner;

/** The Commit payload. */
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class CommitPayload extends PosPayload {
  private static final int TYPE = PosMessage.BLOCK_ANNOUNCE.getCode();
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
  @SneakyThrows
  public static CommitPayload readFrom(final RLPInput rlpInput) {
    rlpInput.enterList();
    long height = rlpInput.readLong();
    final ConsensusRoundIdentifier roundIdentifier = ConsensusRoundIdentifier.readFrom(rlpInput);
    final PosBlock proposedBlock =
            PosBlock.readFrom(rlpInput);
    rlpInput.leaveList();

    return new CommitPayload(roundIdentifier,height,proposedBlock);
  }

  @SneakyThrows
  @Override
  public void writeTo(final RLPOutput rlpOutput) {
    rlpOutput.startList();
    rlpOutput.writeLong(height);
    getRoundIdentifier().writeTo(rlpOutput);
    block.writeTo(rlpOutput);
    rlpOutput.endList();
  }

  @Override
  public int getMessageType() {
    return TYPE;
  }
}
