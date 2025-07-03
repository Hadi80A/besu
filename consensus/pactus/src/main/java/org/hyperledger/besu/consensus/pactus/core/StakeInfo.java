package org.hyperledger.besu.consensus.pactus.core;

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

  /** The unique ID of the validator. */
  private String validatorId;

  /** The amount of PAC coins staked by the validator (in smallest unit). */
  private long stakedAmount;

  /** Block height when the stake became active. */
  private long activationHeight;

  /** Block height when the stake becomes withdrawable (after unbonding). */
  private long withdrawableHeight;

  /** Flag indicating if stake is currently active in the validator set. */
  private boolean active;

  /** Optional: Flag for whether the validator has initiated unbonding. */
  private boolean unbonding;
}
