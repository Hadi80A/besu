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

/**
 * The Vote payload.
 *
 * <p>Updated for Pure PoS (LCR):
 * Removed BLS Signature field. LCR does not utilize threshold signature aggregation.
 * This payload acts as a simple attestation to a block hash, authenticated by the 
 * outer SignedData (ECDSA) wrapper.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class VotePayload extends PosPayload {
    private static final int TYPE = PosMessage.VOTE.getCode();

    private final Hash digest;

    protected VotePayload(
            final ConsensusRoundIdentifier roundIdentifier,
            final Hash digest) {
        super(roundIdentifier);
        this.digest = digest;
    }

    public static VotePayload readFrom(final RLPInput rlpInput) {
        rlpInput.enterList();
        final ConsensusRoundIdentifier roundIdentifier = ConsensusRoundIdentifier.readFrom(rlpInput);
        final Hash digest = Payload.readDigest(rlpInput);
        rlpInput.leaveList();

        return new VotePayload(roundIdentifier, digest);
    }

    @Override
    public void writeTo(final RLPOutput rlpOutput) {
        rlpOutput.startList();
        getRoundIdentifier().writeTo(rlpOutput);
        rlpOutput.writeBytes(digest);
        rlpOutput.endList();
    }

    @Override
    public int getMessageType() {
        return TYPE;
    }
}