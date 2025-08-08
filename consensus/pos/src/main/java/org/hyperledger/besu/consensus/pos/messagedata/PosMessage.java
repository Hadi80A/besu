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

@Getter
public enum PosMessage {
  PROPOSE(0x9),
  VOTE(0x10),
  BLOCK_ANNOUNCE(0x11),
  VIEW_CHANGE(0x12),
  PRE_VOTE(0x13),
  MAIN_VOTE(0x14),
  DECIDE(0x15),
  MESSAGE_SPACE(0x16);

  private final int code;

  PosMessage(int code) {
    this.code = code;
  }
}