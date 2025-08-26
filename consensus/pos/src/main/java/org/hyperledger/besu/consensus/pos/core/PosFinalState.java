/*
 * Copyright contributors to Besu.
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
package org.hyperledger.besu.consensus.pos.core;

import lombok.Getter;
import org.hyperledger.besu.consensus.common.bft.BftHelpers;
import org.hyperledger.besu.consensus.common.bft.BlockTimer;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.RoundTimer;
import org.hyperledger.besu.consensus.common.bft.blockcreation.ProposerSelector;
import org.hyperledger.besu.consensus.common.bft.network.ValidatorMulticaster;
import org.hyperledger.besu.consensus.common.bft.statemachine.BftFinalState;
import org.hyperledger.besu.consensus.common.validator.ValidatorProvider;
import org.hyperledger.besu.consensus.pos.PosBlockCreatorFactory;
import org.hyperledger.besu.consensus.pos.messagewrappers.ViewChange;
import org.hyperledger.besu.cryptoservices.NodeKey;
import org.hyperledger.besu.datatypes.Address;

import java.time.Clock;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
@Getter
/** Besu implementation of PosFinalState for maintaining the state of a Pos network. */
public class PosFinalState {
  private final ValidatorProvider validatorProvider;

  final Map<Address, ViewChange> receivedMessages;
    /**
     * -- GETTER --
     *  Gets node key.
     *
     */
    @Getter
    private final NodeKey nodeKey;
    /**
     * -- GETTER --
     *  Gets local address.
     *
     */
    @Getter
    private final Address localAddress;
  private final ProposerSelector proposerSelector;
    /**
     * -- GETTER --
     *  Gets the validator multicaster.
     *
     */
    @Getter
    private final ValidatorMulticaster validatorMulticaster;
    /**
     * -- GETTER --
     *  Gets round timer.
     *
     */
    @Getter
    private final RoundTimer roundTimer;
  @Getter
  private final BlockTimer blockTimer;
    /**
     * -- GETTER --
     *  Gets block creator factory.
     *
     */
    @Getter
    private final PosBlockCreatorFactory blockCreatorFactory;
    /**
     * -- GETTER --
     *  Gets clock.
     *
     */
    @Getter
    private final Clock clock;

    @Getter
    private final BftFinalState bftFinalState;
  /**
   * Constructs a new POS final state.
   *
   * @param validatorProvider the validator provider
   * @param nodeKey the node key
   * @param localAddress the local address
   * @param proposerSelector the proposer selector
   * @param validatorMulticaster the validator multicaster
   * @param roundTimer the round timer
   * @param blockTimer the block timer
   * @param blockCreatorFactory the block creator factory
   * @param clock the clock
   */
  public PosFinalState(
          final ValidatorProvider validatorProvider,
          final NodeKey nodeKey,
          final Address localAddress,
          final ProposerSelector proposerSelector,
          final ValidatorMulticaster validatorMulticaster,
          final RoundTimer roundTimer,
          final BlockTimer blockTimer,
          final PosBlockCreatorFactory blockCreatorFactory,
          final Clock clock, BftFinalState bftFinalState) {
    this.validatorProvider = validatorProvider;
      this.receivedMessages = new HashMap<>();
      this.nodeKey = nodeKey;
    this.localAddress = localAddress;
    this.proposerSelector = proposerSelector;
    this.validatorMulticaster = validatorMulticaster;
    this.roundTimer = roundTimer;
    this.blockTimer = blockTimer;
    this.blockCreatorFactory = blockCreatorFactory;
    this.clock = clock;
    this.bftFinalState = bftFinalState;
  }

  /**
   * Gets validators.
   *
   * @return the validators
   */
  public Collection<Address> getValidators() {
    return validatorProvider.getValidatorsAtHead();
  }

    /**
   * Is local node validator.
   *
   * @return the boolean
   */

  public boolean isLocalNodeValidator() {
    return getValidators().contains(localAddress);
  }


    public int getQuorum() {
    return BftHelpers.calculateRequiredValidatorQuorum(getValidators().size());
  }


    /**
   * Is local node proposer for round.
   *
   * @param roundIdentifier the round identifier
   * @return the boolean
   */

  public boolean isLocalNodeProposerForRound(final ConsensusRoundIdentifier roundIdentifier) {
    return getProposerForRound(roundIdentifier).equals(localAddress);
  }

  /**
   * Gets proposer for round.
   *
   * @param roundIdentifier the round identifier
   * @return the proposer for round
   */
  public Address getProposerForRound(final ConsensusRoundIdentifier roundIdentifier) {
    return proposerSelector.selectProposerForRound(roundIdentifier);
  }

}
