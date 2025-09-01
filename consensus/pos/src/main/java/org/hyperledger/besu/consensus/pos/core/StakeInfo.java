package org.hyperledger.besu.consensus.pos.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents stake-related data for a validator.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StakeInfo {


  /** The amount of PAC coins staked by the validator (in smallest unit). */
  private long stakedAmount;

    //  private long activationHeight;

    //  private long withdrawableHeight;

    //  private boolean active;

    //  private boolean unbonding;
}