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
package org.hyperledger.besu.consensus.pos.protocol;

import org.hyperledger.besu.consensus.pos.messagedata.PosMessage;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.Capability;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.SubProtocol;

/** The Ibft sub protocol. */
public class PosSubProtocol implements SubProtocol {

  /** The constant NAME. */
  public static String NAME = "POS";

  /** The constant IBFV1. */
  public static final Capability POS = Capability.create(NAME, 1);

  private static final PosSubProtocol INSTANCE = new PosSubProtocol();

  /** Default constructor. */
  public PosSubProtocol() {}

  /**
   * Get ibft sub protocol.
   *
   * @return the ibft sub protocol
   */
  public static PosSubProtocol get() {
    return INSTANCE;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public int messageSpace(final int protocolVersion) {
    return PosMessage.MESSAGE_SPACE.getCode();
  }

  @Override
  public boolean isValidMessageCode(final int protocolVersion, final int code) {
    PosMessage[] values = PosMessage.values();
      return switch (values[code]) {
          case PosMessage.PROPOSE, PosMessage.VOTE, PosMessage.BLOCK_ANNOUNCE ->
//      case PosMessage.ROUND_CHANGE:
                  true;
          default -> false;
      };
  }

  @Override
  public String messageName(final int protocolVersion, final int code) {
    PosMessage[] values = PosMessage.values();
      return switch (values[code]) {
          case PosMessage.PROPOSE -> "Proposal";
          case PosMessage.VOTE -> "Prepare";
          case PosMessage.BLOCK_ANNOUNCE -> "Commit";
//      case PosMessage.ROUND_CHANGE:
//        return "RoundChange";
          default -> INVALID_MESSAGE_NAME;
      };
  }
}
