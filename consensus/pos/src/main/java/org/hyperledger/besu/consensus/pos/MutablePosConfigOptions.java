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

import lombok.Setter;
import org.hyperledger.besu.config.PosConfigOptions;
import org.hyperledger.besu.consensus.common.ForksSchedule;
import org.hyperledger.besu.consensus.common.bft.MutableBftConfigOptions;
import org.hyperledger.besu.datatypes.Address;

import java.util.Optional;
import java.util.OptionalLong;

/**
 * A mutable {@link PosConfigOptions} that is used for building config for transitions in the
 * {@link ForksSchedule}.
 */
public class MutablePosConfigOptions extends MutableBftConfigOptions implements PosConfigOptions {
    /**
     * -- SETTER --
     *  Sets validator contract address.
     *
     * @param validatorContractAddress the validator contract address
     */
    @Setter
    private Optional<String> validatorContractAddress;
    @Setter
    private Address contractAddress;
  /**
   * Instantiates a new Mutable pos config options.
   *
   * @param posConfigOptions the pos config options
   */
  public MutablePosConfigOptions(final PosConfigOptions posConfigOptions) {
    super(posConfigOptions);
    this.validatorContractAddress =
        posConfigOptions.getValidatorContractAddress().map(String::toLowerCase);
    this.contractAddress=posConfigOptions.getContractAddress();
  }

  @Override
  public Optional<String> getValidatorContractAddress() {
    return validatorContractAddress;
  }

    @Override
  public OptionalLong getStartBlock() {
    return OptionalLong.empty();
  }

  @Override
  public Address getContractAddress() {
    return contractAddress;
  }
}
