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
import lombok.experimental.SuperBuilder;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.payload.Payload;
import org.hyperledger.besu.consensus.pos.messagedata.PosMessage;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.rlp.RLPInput;
import org.hyperledger.besu.ethereum.rlp.RLPOutput;

/** The BlockAnnounce payload. */
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class BlockAnnouncePayload extends PosPayload {
  private static final int TYPE = PosMessage.BLOCK_ANNOUNCE.getCode();
  private final Hash digest;


//  private final SECPSignature aggregation;//todo

  public BlockAnnouncePayload(ConsensusRoundIdentifier roundIdentifier, long height , Hash digest) {
    super(roundIdentifier, height);
      this.digest = digest;
  }

  /**
   * Read from rlp input and return commit payload.
   *
   * @param rlpInput the rlp input
   * @return the commit payload
   */
  public static BlockAnnouncePayload readFrom(final RLPInput rlpInput) {
    rlpInput.enterList();
    long height = rlpInput.readLong();
    final ConsensusRoundIdentifier roundIdentifier = ConsensusRoundIdentifier.readFrom(rlpInput);
    final Hash digest = Payload.readDigest(rlpInput);

    rlpInput.leaveList();

    return new BlockAnnouncePayload(roundIdentifier,height,digest);
  }

  @Override
  public void writeTo(final RLPOutput rlpOutput) {
    rlpOutput.startList();
    rlpOutput.writeLong(height);
    getRoundIdentifier().writeTo(rlpOutput);
    rlpOutput.writeBytes(digest);

    rlpOutput.endList();
  }

  @Override
  public int getMessageType() {
    return TYPE;
  }
}
