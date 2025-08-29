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

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.pos.core.PosBlock;
import org.hyperledger.besu.consensus.pos.messagedata.PosMessage;
import org.hyperledger.besu.consensus.pos.vrf.VRF;
import org.hyperledger.besu.ethereum.rlp.RLPInput;
import org.hyperledger.besu.ethereum.rlp.RLPOutput;

import java.io.IOException;

/** The Proposal payload. */
//@AllArgsConstructor
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class SelectLeaderPayload extends PosPayload {
  private static final int TYPE = PosMessage.SELECT_LEADER.getCode();
  private VRF.Proof proof;

  protected SelectLeaderPayload(ConsensusRoundIdentifier roundIdentifier, long height,VRF.Proof proof) {
    super(roundIdentifier, height);
    this.proof = proof;
  }

  public static SelectLeaderPayload readFrom(final RLPInput rlpInput) {
    rlpInput.enterList();
    final ConsensusRoundIdentifier roundIdentifier = ConsensusRoundIdentifier.readFrom(rlpInput);
    final long height = rlpInput.readLong();
      Bytes proofBytes = rlpInput.readBytes();

      final VRF.Proof proof=new VRF.Proof(proofBytes.toArray());
      rlpInput.leaveList();

      return new SelectLeaderPayload(roundIdentifier,height,proof);
  }


  @Override
  public void writeTo(final RLPOutput rlpOutput) {
    rlpOutput.startList();
    getRoundIdentifier().writeTo(rlpOutput);
    rlpOutput.writeLong(getHeight());
      rlpOutput.writeBytes(Bytes.wrap(proof.bytes()));
      rlpOutput.endList();
  }

  @Override
  public int getMessageType() {
    return TYPE;
  }

}
