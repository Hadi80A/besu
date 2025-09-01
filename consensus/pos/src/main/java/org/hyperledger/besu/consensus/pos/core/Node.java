package org.hyperledger.besu.consensus.pos.core;

import lombok.*;
import org.hyperledger.besu.crypto.SECPPublicKey;
import org.hyperledger.besu.datatypes.Address;

/**
 * Represents a validator in the Pactus consensus mechanism.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Node {

  /** Unique identifier for the validator (e.g., public key or address). */
  private String id;
  private Address address;
  @Setter
  private SECPPublicKey  publicKey;
  /** Stake information associated with this validator. */
  private StakeInfo stakeInfo;

    //  private long lastJoinedHeight;

  /** Flag indicating whether this validator is currently in the committee. */
  private boolean inCommittee;

  /** Number of blocks proposed by this validator. */
  private int blocksProposed;

  /** Optional: Timestamp or round count when the validator last proposed a block. */
  private long lastProposedAt;
  public void addStake(int amount){
    long newStake=stakeInfo.getStakedAmount() + amount;
    stakeInfo.setStakedAmount(newStake);
  }

  public void subStake(int amount){
    long newStake=stakeInfo.getStakedAmount() - amount;
    stakeInfo.setStakedAmount(newStake);
  }

  public void updateStake(int amount){
    long newStake=stakeInfo.getStakedAmount() - amount;
    stakeInfo.setStakedAmount(newStake);
  }

    //  public boolean canRunSortition(long currentHeight) {
//    // A validator can run sortition if they are not already in the committee
//    // or if they've been in the committee for 51+ blocks
//    return !inCommittee || (currentHeight - lastJoinedHeight >= 51);
//  }

    //  public boolean shouldBeRemoved(long currentHeight) {
//    return inCommittee && (currentHeight - lastJoinedHeight >= 51);
//  }
}
