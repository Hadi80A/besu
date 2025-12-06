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
package org.hyperledger.besu.consensus.pos.messagedata;

import lombok.Getter;

/**
 * Defines the P2P wire protocol message codes for the PoS SubProtocol.
 */
@Getter
public enum PosMessage {



    // Phase 4: Block Proposal (Core LCR)
    PROPOSE(0x9),

    // Legacy / Finality Gadget Placeholders (Unused in Core LCR)
    VOTE(0x10),
    COMMIT(0x11),

    // Protocol Definition
    MESSAGE_SPACE(0x16);

    private final int code;

    PosMessage(int code) {
        this.code = code;
    }
}