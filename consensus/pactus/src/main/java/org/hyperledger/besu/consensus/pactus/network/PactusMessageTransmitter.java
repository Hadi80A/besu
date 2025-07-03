// PactusMessageTransmitter.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.network;

import org.hyperledger.besu.consensus.pactus.messagewrappers.Commit;
import org.hyperledger.besu.consensus.pactus.messagewrappers.PreCommit;
import org.hyperledger.besu.consensus.pactus.messagewrappers.Proposal;
import org.hyperledger.besu.consensus.pactus.messagewrappers.Certificate;
import org.hyperledger.besu.ethereum.p2p.api.Peer;
import org.hyperledger.besu.ethereum.p2p.network.Network;

import java.util.Collection;

/**
 * Responsible for transmitting consensus messages to peers over the P2P network.
 */
public class PactusMessageTransmitter {

  private final Network network;

  public PactusMessageTransmitter(final Network network) {
    this.network = network;
  }

  /**
   * Broadcast a proposal to all peers.
   */
  public void multicastProposal(final Proposal proposal) {
    network.broadcast("pactus/proposal", proposal);
  }

  /**
   * Broadcast a pre-commit message to all peers.
   */
  public void multicastPreCommit(final PreCommit preCommit) {
    network.broadcast("pactus/precommit", preCommit);
  }

  /**
   * Broadcast a commit message to all peers.
   */
  public void multicastCommit(final Commit commit) {
    network.broadcast("pactus/commit", commit);
  }

  /**
   * Broadcast a final certificate to all peers.
   */
  public void multicastCertificate(final Certificate certificate) {
    network.broadcast("pactus/certificate", certificate);
  }

  /**
   * Send a proposal message to a specific peer.
   */
  public void unicastProposal(final Peer peer, final Proposal proposal) {
    network.send(peer, "pactus/proposal", proposal);
  }

  /**
   * Send a commit message to a specific peer.
   */
  public void unicastCommit(final Peer peer, final Commit commit) {
    network.send(peer, "pactus/commit", commit);
  }

  /**
   * Send a pre-commit message to a specific peer.
   */
  public void unicastPreCommit(final Peer peer, final PreCommit preCommit) {
    network.send(peer, "pactus/precommit", preCommit);
  }

  /**
   * Send a certificate message to a specific peer.
   */
  public void unicastCertificate(final Peer peer, final Certificate certificate) {
    network.send(peer, "pactus/certificate", certificate);
  }

  /**
   * Send a batch of proposals or consensus messages to a group of peers.
   */
  public void multicastToGroup(Collection<Peer> peers, String messageType, Object message) {
    for (Peer peer : peers) {
      network.send(peer, messageType, message);
    }
  }
}
