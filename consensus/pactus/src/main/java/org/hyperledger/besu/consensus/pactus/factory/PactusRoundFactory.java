/*
 * Copyright ConsenSys AG.
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
package org.hyperledger.besu.consensus.pactus.factory;
import jakarta.validation.Valid;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.pactus.PactusProtocolSchedule;
import org.hyperledger.besu.consensus.pactus.core.*;
import org.hyperledger.besu.consensus.pactus.network.PactusMessageTransmitter;
import org.hyperledger.besu.consensus.pactus.payload.MessageFactory;
import org.hyperledger.besu.consensus.pactus.statemachine.PactusFinalState;
import org.hyperledger.besu.consensus.pactus.statemachine.PactusRound;
import org.hyperledger.besu.consensus.pactus.statemachine.RoundState;
import org.hyperledger.besu.consensus.pactus.validation.MessageValidatorFactory;

/** The Pactus round factory. */
public class PactusRoundFactory {

  private final PactusFinalState finalState;
  private final PactusBlockCreatorFactory blockCreatorFactory;
  private final PactusBlockInterface blockInterface;
  private final PactusProtocolSchedule protocolSchedule;
  private final MessageValidatorFactory messageValidatorFactory;
  private final MessageFactory messageFactory;
  private final ValidatorSet validators;
  private final PactusProposerSelector proposerSelector;

  /**
   * Instantiates a new Pactus round factory.
   *
   * @param finalState the final state
   * @param blockInterface the block interface
   * @param protocolSchedule the protocol schedule
   * @param messageValidatorFactory the message validator factory
   * @param messageFactory the message factory
   */
  public PactusRoundFactory(
          final PactusFinalState finalState,
          final PactusBlockInterface blockInterface,
          final PactusProtocolSchedule protocolSchedule,
          final MessageValidatorFactory messageValidatorFactory,
          final MessageFactory messageFactory, ValidatorSet validators, PactusProposerSelector proposerSelector) {
    this.finalState = finalState;
    this.blockCreatorFactory = finalState.getBlockCreatorFactory();
    this.blockInterface = blockInterface;
    this.protocolSchedule = protocolSchedule;
    this.messageValidatorFactory = messageValidatorFactory;
    this.messageFactory = messageFactory;
      this.validators = validators;
      this.proposerSelector = proposerSelector;
  }

  /**
   * Create new round Pactus round.
   *
   * @param parentHeader the parent header
   * @param round the round
   * @return the Pactus round
   */
  public PactusRound createNewRound(final PactusBlockHeader parentHeader, final int round) {
    long nextBlockHeight = parentHeader.getBesuHeader().getNumber() + 1;
    final ConsensusRoundIdentifier roundIdentifier =
        new ConsensusRoundIdentifier(nextBlockHeight, round);

    final RoundState roundState =
        new RoundState(
            roundIdentifier,
            finalState.getQuorum(),
            messageValidatorFactory.createMessageValidator(roundIdentifier, parentHeader));

    return createNewRoundWithState(parentHeader, roundState);
  }

  /**
   * Create new Pactus round with state.
   *
   * @param parentHeader the parent header
   * @param roundState the round state
   * @return the Pactus round
   */
  public PactusRound createNewRoundWithState(
      final PactusBlockHeader parentHeader, final RoundState roundState) {
    final PactusBlockCreator blockCreator =
        blockCreatorFactory.create(roundState.getRoundIdentifier().getRoundNumber());

    // TODO(tmm): Why is this created everytime?!
    final PactusMessageTransmitter messageTransmitter =
        new PactusMessageTransmitter(messageFactory, finalState.getValidatorMulticaster());

    return new PactusRound(
        roundState,
        blockCreator,
        blockInterface,
        protocolSchedule,
        finalState.getNodeKey(),
        messageFactory,
        messageTransmitter,
        finalState.getRoundTimer(),
        parentHeader,
        validators,
        proposerSelector

    );
  }
}
