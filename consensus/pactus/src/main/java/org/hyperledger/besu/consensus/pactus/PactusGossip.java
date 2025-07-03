// PactusGossip.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus;

import org.hyperledger.besu.consensus.pactus.network.PactusMessageTransmitter;

/**
 * Handles network-level gossiping of Pactus consensus messages.
 * Delegates to the message transmitter component.
 */
public class PactusGossip {

  private final PactusMessageTransmitter messageTransmitter;

  public PactusGossip(final PactusMessageTransmitter messageTransmitter) {
    this.messageTransmitter = messageTransmitter;
  }

  public void broadcastProposal(String proposalPayload) {
    messageTransmitter.multicastProposal(proposalPayload);
  }

  public void broadcastPreCommit(String preCommitPayload) {
    messageTransmitter.multicastPreCommit(preCommitPayload);
  }

  public void broadcastCommit(String commitPayload) {
    messageTransmitter.multicastCommit(commitPayload);
  }

  public void broadcastCertificate(String certificatePayload) {
    messageTransmitter.multicastCertificate(certificatePayload);
  }
}
