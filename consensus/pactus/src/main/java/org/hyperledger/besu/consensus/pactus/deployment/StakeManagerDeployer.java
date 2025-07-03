
package org.hyperledger.besu.consensus.pactus.deployment;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.ethereum.core.*;
import org.hyperledger.besu.ethereum.rlp.RLP;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.ethereum.core.TransactionBuilder;

import java.math.BigInteger;
import java.util.Optional;

/**
 * Deploys the StakeManager contract at startup during the initialization of the Pactus consensus.
 */
public class StakeManagerDeployer {

  private final MutableBlockchain blockchain;
  private final WorldUpdater worldState;
  private final Address deployerAddress;
  private final long chainId;

  public StakeManagerDeployer(
      final MutableBlockchain blockchain,
      final WorldUpdater worldState,
      final Address deployerAddress,
      final long chainId) {
    this.blockchain = blockchain;
    this.worldState = worldState;
    this.deployerAddress = deployerAddress;
    this.chainId = chainId;
  }

  /**
   * Deploys the contract if not already deployed.
   */
  public void deployIfNotExists() {
    final Address contractAddress = Address.contractAddress(deployerAddress, 0);
    if (worldState.get(contractAddress) != null) {
      System.out.println("✅ StakeManager already deployed at " + contractAddress);
      return;
    }

    final String bytecode = "<STAKE_MANAGER_BYTECODE>"; // Replace with actual bytecode

    final Transaction deploymentTx = Transaction.builder()
        .type(TransactionType.FRONTIER)
        .sender(deployerAddress)
        .nonce(0)
        .gasLimit(8_000_000)
        .gasPrice(Wei.of(0))
        .value(Wei.ZERO)
        .payload(Bytes.fromHexString(bytecode))
        .chainId(chainId)
        .build();

    // Apply transaction directly into genesis block state
    try {
      worldState.getOrCreate(deployerAddress).getMutable().incrementNonce();
      worldState.getOrCreate(contractAddress).getMutable().setCode(Bytes.fromHexString(bytecode));
      System.out.println("🚀 StakeManager deployed to " + contractAddress);
    } catch (Exception e) {
      System.err.println("❌ Failed to deploy StakeManager: " + e.getMessage());
    }
  }
}
