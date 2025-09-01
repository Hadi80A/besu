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
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.payload.Payload;
import org.hyperledger.besu.consensus.pos.messagedata.PosMessage;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.rlp.RLPInput;
import org.hyperledger.besu.ethereum.rlp.RLPOutput;

//@AllArgsConstructor
@Getter
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
public class VotePayload extends PosPayload {
  private static final int TYPE = PosMessage.VOTE.getCode();
  private final Hash digest;


  protected VotePayload(ConsensusRoundIdentifier roundIdentifier, long height, Hash digest) { //, SECPSignature signature
    super(roundIdentifier, height);
    this.digest = digest;
//      this.signature = signature;
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

}
