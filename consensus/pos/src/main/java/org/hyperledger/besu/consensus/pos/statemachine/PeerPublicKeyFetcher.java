package org.hyperledger.besu.consensus.pos.statemachine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.tuweni.bytes.Bytes;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.core.Util;
import org.hyperledger.besu.ethereum.eth.manager.EthPeers;
import org.hyperledger.besu.ethereum.eth.manager.EthPeer;

/**
 * Utility to fetch connected peer node public keys (hex) from the in-process EthPeers.
 *
 * Notes:
 * - EthPeers is typically available from EthContext: EthContext.getEthPeers()
 * - EthPeer.getId() returns a Bytes instance representing the peer node id (the same as admin_peers "id").
 */
public class PeerPublicKeyFetcher {

  private final EthPeers ethPeers;

  public PeerPublicKeyFetcher(final EthPeers ethPeers) {
    this.ethPeers = ethPeers;
  }

  /**
   * Returns connected peers' node public keys as hex (0x-prefixed).
   */
  public List<String> getConnectedPeerNodeIdsHex() {
    // ethPeers.streamPeers() -> Stream<EthPeer>
    return ethPeers.streamAllPeers()
        .map(EthPeer::getId)             // returns org.apache.tuweni.bytes.Bytes
        .map(Bytes::toHexString)         // ensure hex string format (may already include 0x)
        .map(s -> s.startsWith("0x") ? s : "0x" + s)
        .collect(Collectors.toList());
  }

  /**
   * Returns connected peers' node public keys as hex (0x-prefixed).
   */
  public List<Bytes> getConnectedPeerNodeIds() {
    // ethPeers.streamPeers() -> Stream<EthPeer>
    return ethPeers.streamAllPeers()
            .map(EthPeer::getId)
            .collect(Collectors.toList());
  }

  public Map<Address, Bytes> getConnectedPeerNodeIdsMap() {
    List<Bytes> connectedPeerNodeIds = getConnectedPeerNodeIds();
    Map<Address, Bytes> connectedPeerNodeIdsMap = new HashMap<>();
    for (Bytes nodePubKey : connectedPeerNodeIds) {
      Address address = Util.publicKeyToAddress(nodePubKey);
      connectedPeerNodeIdsMap.put(address, nodePubKey);
    }
    return connectedPeerNodeIdsMap;
  }
}
