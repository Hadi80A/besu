/*
 * Copyright 2020 ConsenSys AG.
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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.payload.Payload;
import org.hyperledger.besu.datatypes.Hash;


@AllArgsConstructor
@Getter
@SuperBuilder
public abstract class PosPayload implements Payload {

    private static final Logger log = LogManager.getLogger(PosPayload.class);
    protected final ConsensusRoundIdentifier roundIdentifier;

    /**
     * Generates the hash that will be signed by the ECDSA key.
     * Structure: Keccak256(MessageType || RLP(Payload))
     *
     * @return The 32-byte hash to sign.
     */
    @Override
    public Hash hashForSignature() {
        log.debug("hashForSignature");
        // DSS: The signature covers the type + content to prevent replay across message types.
        Bytes concatenate = Bytes.concatenate(Bytes.of(getMessageType()), encoded());
        log.debug("hashForSignature concatenate");
        return Hash.hash(concatenate);

    }
}