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

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/** The Ibft sub protocol. */
public class PosSubProtocol implements SubProtocol {

  /** The constant NAME. */
  public static String NAME = "POS";

  /** The constant IBFV1. */
  public static final Capability POS = Capability.create(NAME, 1);

  private static final PosSubProtocol INSTANCE = new PosSubProtocol();

  // Precomputed map for fast lookup by code
  private static final Map<Integer, PosMessage> CODE_TO_MESSAGE =
          Arrays.stream(PosMessage.values()).collect(Collectors.toMap(PosMessage::getCode, m -> m));

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
    PosMessage message = CODE_TO_MESSAGE.get(code);
    if (message == null) {
      return false;
    }
    return switch (message) {
      case PROPOSE, VOTE, BLOCK_ANNOUNCE, VIEW_CHANGE , SELECT_LEADER-> true;
      default -> false;
    };
  }

  @Override
  public String messageName(final int protocolVersion, final int code) {
    PosMessage message = CODE_TO_MESSAGE.get(code);
    if (message == null) {
      return INVALID_MESSAGE_NAME;
    }
    return switch (message) {
      case PROPOSE -> "Proposal";
      case VOTE -> "Vote";
      case BLOCK_ANNOUNCE -> "Commit";
      case VIEW_CHANGE -> "ViewChange";
      case SELECT_LEADER -> "SelectLeader";

      default -> INVALID_MESSAGE_NAME;
    };
  }

}
