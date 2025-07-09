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
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.payload.SignedData;
import org.hyperledger.besu.consensus.pactus.PactusProtocolSchedule;
import org.hyperledger.besu.consensus.pactus.core.*;
import org.hyperledger.besu.consensus.pactus.messagewrappers.*;
import org.hyperledger.besu.consensus.pactus.network.PactusMessageTransmitter;
import org.hyperledger.besu.consensus.pactus.payload.*;
import org.hyperledger.besu.consensus.pactus.statemachine.PactusFinalState;
import org.hyperledger.besu.consensus.pactus.statemachine.PactusRound;
import org.hyperledger.besu.consensus.pactus.statemachine.RoundState;
import org.hyperledger.besu.consensus.pactus.validation.MessageValidatorFactory;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.rlp.RLPOutput;

import java.util.List;

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
            finalState.getQuorum()
            );

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

  /**
   * Factory class for creating consensus message payloads and wrappers.
   */
  public static class MessageFactory{

    private final int localValidatorId;

    public MessageFactory(final int localValidatorId) {
      this.localValidatorId = localValidatorId;
    }

    public Proposal createProposal(SignedData<ProposePayload> payload, PactusBlock block) {
      return new Proposal(payload,localValidatorId,block);
    }
    public Prepare createPrepare(SignedData<PreparePayload> payload) {
      return new Prepare(payload);
    }
    public PreCommit createPreCommit(SignedData<PreCommitPayload> payload) {
      return new PreCommit(payload,localValidatorId);
    }

    public Commit createCommit(SignedData<CommitPayload> payload) {
      return new Commit(payload,localValidatorId);
    }

    public Certificate createCertificate(String blockHash, long height, int round,
                                         List<String> signerIds, List<String> signatures) {
      return Certificate.builder()
          .blockHash(blockHash)
          .blockHeight(height)
          .round(round)
          .signerIds(signerIds)
          .signatures(signatures)
          .build();
    }

    public CommitPayload createCommitPayload(PactusBlock block) {
      return CommitPayload.builder()
              .block(block)
              .round(block.getRound())
              .height(block.getHeight())
              .build();
    }

    public ProposePayload createProposePayload(int round,int height) {
      return ProposePayload.builder()
          .round(round)
          .height(height)
          .build();
    }

    public PreparePayload createPreparePayload(PactusBlock block) {
      return PreparePayload.builder()
              .blockHash(block.getHash())
              .round(block.getRound())
              .height(block.getHeight())
              .build();
    }

    public PreCommitPayload createPreCommitPayload(PactusBlock block) {
      return PreCommitPayload.builder()
          .blockHash(block.getHash())
          .round(block.getRound())
          .height(block.getHeight())
          .build();
    }

    @Override
    public void writeTo(RLPOutput rlpOutput) {

    }

    @Override
    public int getMessageType() {
//      PactusMessage.ROUND_CHANGE.getCode()
      return 0;
    }

    @Override
    public Hash hashForSECPSignature() {
      return null;
    }

    @Override
    public ConsensusRoundIdentifier getRoundIdentifier() {
      return null;
    }
  }
}
