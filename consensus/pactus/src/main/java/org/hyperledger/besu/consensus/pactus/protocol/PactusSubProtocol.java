// PactusSubProtocol.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.protocol;

import org.hyperledger.besu.ethereum.p2p.rlpx.wire.SubProtocol;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.messages.DisconnectMessage;

import java.util.List;

/**
 * Defines the Pactus subprotocol used for consensus-related message exchange
 * over the P2P RLPx network.
 */
public class PactusSubProtocol implements SubProtocol {

  public static final String NAME = "pactus";
  public static final int VERSION = 1;

  private static final int PROTOCOL_LENGTH = 16;


  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public int messageSpace(int protocolVersion) {
    return 0;
  }

  @Override
  public boolean isValidMessageCode(int protocolVersion, int code) {
    return false;
  }

  @Override
  public String messageName(int protocolVersion, int code) {
    return "";
  }
}
