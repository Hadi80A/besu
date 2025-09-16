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

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.pos.messagedata.PosMessage;
import org.hyperledger.besu.consensus.pos.vrf.VRF;
import org.hyperledger.besu.crypto.SECPPublicKey;
import org.hyperledger.besu.ethereum.rlp.RLPInput;
import org.hyperledger.besu.ethereum.rlp.RLPOutput;

/** The Proposal payload. */
//@AllArgsConstructor
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class SelectLeaderPayload extends PosPayload {
  private static final int TYPE = PosMessage.SELECT_LEADER.getCode();
  private static String ALGORITHM = "ECDSA";

  private VRF.Proof proof;
  private boolean isCandidate;
  private SECPPublicKey publicKey;

  protected SelectLeaderPayload(ConsensusRoundIdentifier roundIdentifier, long height,VRF.Proof proof,
                                boolean isCandidate, SECPPublicKey publicKey) {
    super(roundIdentifier, height);
    this.proof = proof;
    this.isCandidate = isCandidate;
    this.publicKey = publicKey;
  }

  public static SelectLeaderPayload readFrom(final RLPInput rlpInput) {
    rlpInput.enterList();
    final ConsensusRoundIdentifier roundIdentifier = ConsensusRoundIdentifier.readFrom(rlpInput);
    final long height = rlpInput.readLong();
      Bytes proofBytes = rlpInput.readBytes();

      final VRF.Proof proof=new VRF.Proof(proofBytes.toArray());
    final int candidateInt = rlpInput.readInt();
    final boolean isCandidate = candidateInt != 0;
    Bytes publicKeyByte= rlpInput.readBytes();
    final SECPPublicKey secpPublicKey=SECPPublicKey.create(publicKeyByte, ALGORITHM);
      rlpInput.leaveList();

      return new SelectLeaderPayload(roundIdentifier,height,proof,isCandidate,secpPublicKey);
  }


  @Override
  public void writeTo(final RLPOutput rlpOutput) {
    rlpOutput.startList();
    getRoundIdentifier().writeTo(rlpOutput);
    rlpOutput.writeLong(getHeight());
      rlpOutput.writeBytes(Bytes.wrap(proof.bytes()));
    // Write boolean as 0x01 (true) or 0x00 (false)
    rlpOutput.writeInt(isCandidate ? 1 : 0);
    rlpOutput.writeBytes(publicKey.getEncodedBytes());
      rlpOutput.endList();
  }

  @Override
  public int getMessageType() {
    return TYPE;
  }

}
