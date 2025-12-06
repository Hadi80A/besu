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

import org.hyperledger.besu.consensus.common.bft.payload.Payload;
import org.hyperledger.besu.consensus.common.bft.payload.SignedData;
import org.hyperledger.besu.crypto.SECPSignature;
import org.hyperledger.besu.crypto.SignatureAlgorithmFactory;
import org.hyperledger.besu.ethereum.rlp.RLPInput;

/**
 * Utility class to deserialize RLP inputs into SignedData wrappers.
 *
 * <p>Phase 5: Propagation
 * This component is the entry point for converting raw network bytes into
 * authenticated internal objects (Payload + ECDSA Signature).
 */
public class PayloadDeserializers {

    /** Default constructor. */
    protected PayloadDeserializers() {}


    public static SignedData<ProposePayload> readSignedProposalPayloadFrom(final RLPInput rlpInput) {
        rlpInput.enterList();
        final ProposePayload unsignedMessageData = ProposePayload.readFrom(rlpInput);
        final SECPSignature signature = readSignature(rlpInput);
        rlpInput.leaveList();

        return from(unsignedMessageData, signature);
    }

    /**
     * Read signed vote payload from rlp input.
     *
     * @param rlpInput the rlp input
     * @return the signed data
     */
    public static SignedData<VotePayload> readSignedVotePayloadFrom(final RLPInput rlpInput) {
        rlpInput.enterList();
        final VotePayload unsignedMessageData = VotePayload.readFrom(rlpInput);
        final SECPSignature signature = readSignature(rlpInput);
        rlpInput.leaveList();

        return from(unsignedMessageData, signature);
    }



    /**
     * Create signed payload data from unsigned message data.
     *
     * @param <M> the type parameter
     * @param unsignedMessageData the unsigned message data
     * @param signature the signature
     * @return the signed data
     */
    protected static <M extends Payload> SignedData<M> from(
            final M unsignedMessageData, final SECPSignature signature) {
        return SignedData.create(unsignedMessageData, signature);
    }

    /**
     * Read signature from signed message.
     *
     * @param signedMessage the signed message
     * @return the secp signature
     */
    protected static SECPSignature readSignature(final RLPInput signedMessage) {
        return signedMessage.readBytes(SignatureAlgorithmFactory.getInstance()::decodeSignature);
    }
}