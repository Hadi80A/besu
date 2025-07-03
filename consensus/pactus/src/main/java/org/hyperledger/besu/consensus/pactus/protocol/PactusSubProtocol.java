// PactusSubProtocol.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.protocol;

import org.hyperledger.besu.ethereum.p2p.network.SubProtocol;
import org.hyperledger.besu.ethereum.p2p.network.SubProtocolIdentifier;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.messages.DisconnectReason;

import java.util.List;

/**
 * Defines the Pactus subprotocol used for consensus-related message exchange
 * over the P2P RLPx network.
 */
public class PactusSubProtocol {

  public static final String NAME = "pactus";
  public static final int VERSION = 1;

  private static final int PROTOCOL_LENGTH = 16;

  public static final SubProtocolIdentifier IDENTIFIER =
      new SubProtocolIdentifier(NAME, VERSION);

  public static final SubProtocol INSTANCE =
      SubProtocol.create(
          NAME,
          VERSION,
          PROTOCOL_LENGTH,
          (capabilities, connection) -> {
            connection.disconnect(DisconnectReason.USELESS_PEER);
          });

  /**
   * Returns the list of supported protocol identifiers for this subprotocol.
   */
  public static List<SubProtocolIdentifier> getSupportedIdentifiers() {
    return List.of(IDENTIFIER);
  }
}
