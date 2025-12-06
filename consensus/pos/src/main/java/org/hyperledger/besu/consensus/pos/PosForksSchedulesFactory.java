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

import org.hyperledger.besu.config.GenesisConfigOptions;
import org.hyperledger.besu.config.PosConfigOptions;
import org.hyperledger.besu.config.PosFork;
import org.hyperledger.besu.consensus.common.ForkSpec;
import org.hyperledger.besu.consensus.common.ForksSchedule;
import org.hyperledger.besu.consensus.common.ForksScheduleFactory;

/**
 * The Pos forks schedules factory.
 *
 * <p>Phase 2: Time Slot Management
 * Responsible for creating the schedule of protocol parameters.
 * Enables dynamic updates to Slot Duration (BlockPeriodSeconds) and Rewards via fork configuration.
 */
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

    /**
     * Creates a new config option set based on the previous spec and the incoming fork definition.
     *
     * @param lastSpec the configuration active before this fork
     * @param fork the new fork definition containing overrides
     * @return the updated configuration options
     */
    private static PosConfigOptions createPosConfigOptions(
            final ForkSpec<PosConfigOptions> lastSpec, final PosFork fork) {
        final MutablePosConfigOptions posConfigOptions =
                new MutablePosConfigOptions(lastSpec.getValue());

        // Update Slot Duration if defined in the fork
        fork.getBlockPeriodSeconds().ifPresent(posConfigOptions::setBlockPeriodSeconds);

        // Update Block Reward if defined
        fork.getBlockRewardWei().ifPresent(posConfigOptions::setBlockRewardWei);

        // Update Fee Recipient/Beneficiary if explicitly configured
        if (fork.isMiningBeneficiaryConfigured()) {
            posConfigOptions.setMiningBeneficiary(fork.getMiningBeneficiary());
        }

        return posConfigOptions;
    }
}