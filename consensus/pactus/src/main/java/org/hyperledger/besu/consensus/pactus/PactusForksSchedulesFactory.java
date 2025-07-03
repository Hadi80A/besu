// PactusForksSchedulesFactory.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus;

import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule;
import org.hyperledger.besu.ethereum.mainnet.ProtocolSpec;
import org.hyperledger.besu.ethereum.mainnet.fork.Fork;
import org.hyperledger.besu.ethereum.mainnet.fork.ForkSpec;
import org.hyperledger.besu.ethereum.mainnet.fork.ForksSchedule;

import java.util.Collections;

/**
 * Builds the ForksSchedule for Pactus consensus, defining protocol specs across forks.
 */
public class PactusForksSchedulesFactory {

  private final ProtocolSchedule protocolSchedule;

  public PactusForksSchedulesFactory(final ProtocolSchedule protocolSchedule) {
    this.protocolSchedule = protocolSchedule;
  }

  /**
   * Returns a static schedule with a single fork using the base protocol schedule.
   */
  public ForksSchedule<ProtocolSpec> createForksSchedule() {
    final Fork fork = new Fork(0L); // Genesis fork
    final ProtocolSpec protocolSpec = protocolSchedule.getByBlockNumber(0L);
    final ForkSpec<ProtocolSpec> forkSpec = new ForkSpec<>(fork, protocolSpec);

    return new ForksSchedule<>(Collections.singletonList(forkSpec));
  }

  /**
   * Returns the active ProtocolSpec for a given block header.
   */
  public ProtocolSpec getSpecForHeader(final BlockHeader header) {
    return protocolSchedule.getByBlockNumber(header.getNumber());
  }
}
