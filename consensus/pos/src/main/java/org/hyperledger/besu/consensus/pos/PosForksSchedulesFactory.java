/*
 * Copyright contributors to Hyperledger Besu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.consensus.pos;

import org.hyperledger.besu.config.*;
import org.hyperledger.besu.consensus.common.ForkSpec;
import org.hyperledger.besu.consensus.common.ForksSchedule;
import org.hyperledger.besu.consensus.common.ForksScheduleFactory;

/** The Pos forks schedules factory. */
public class PosForksSchedulesFactory {
  /** Default constructor. */
  private PosForksSchedulesFactory() {}

  /**
   * Create forks schedule.
   *
   * @param genesisConfig the genesis config
   * @return the forks schedule
   */
  public static ForksSchedule<PosConfigOptions> create(final GenesisConfigOptions genesisConfig) {
    return ForksScheduleFactory.create(
        genesisConfig.getPosConfigOptions(),
        genesisConfig.getTransitions().getPosForks(),
        PosForksSchedulesFactory::createPosConfigOptions);
  }

  private static PosConfigOptions createPosConfigOptions(
      final ForkSpec<PosConfigOptions> lastSpec, final PosFork fork) {
    final MutablePosConfigOptions posConfigOptions =
        new MutablePosConfigOptions(lastSpec.getValue());

    fork.getBlockPeriodSeconds().ifPresent(posConfigOptions::setBlockPeriodSeconds);
    fork.getBlockRewardWei().ifPresent(posConfigOptions::setBlockRewardWei);

    if (fork.isMiningBeneficiaryConfigured()) {
      // Only override if mining beneficiary is explicitly configured
      posConfigOptions.setMiningBeneficiary(fork.getMiningBeneficiary());
    }

    return posConfigOptions;
  }
}
