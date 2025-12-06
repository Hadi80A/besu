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
package org.hyperledger.besu.consensus.pos.validation;

import org.hyperledger.besu.consensus.common.bft.BftContext;
import org.hyperledger.besu.consensus.common.bft.BftExtraDataCodec;
import org.hyperledger.besu.consensus.common.bft.BftProtocolSchedule;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.blockcreation.ProposerSelector;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.ProtocolContext;
import org.hyperledger.besu.ethereum.core.BlockHeader;

import java.util.Collection;


public class MessageValidatorFactory {

    private final ProtocolContext protocolContext;
    private final BftProtocolSchedule protocolSchedule;

    /**
     * Constructor matching the BFT architecture signature.
     * Unused parameters (ProposerSelector, Codec) are retained for compatibility
     * with the controller builder but not stored if irrelevant to LCR.
     */
    public MessageValidatorFactory(
            final ProposerSelector proposerSelector,
            final BftProtocolSchedule protocolSchedule,
            final ProtocolContext protocolContext,
            final BftExtraDataCodec bftExtraDataCodec) {
        this.protocolSchedule = protocolSchedule;
        this.protocolContext = protocolContext;
    }

    /**
     * Get the list of validators that are applicable after the given block.
     * Used to determine the active validator set (Ledger of Stake).
     *
     * @param protocolContext the protocol context
     * @param parentHeader the parent header
     * @return the list of validators
     */
    public static Collection<Address> getValidatorsAfterBlock(
            final ProtocolContext protocolContext, final BlockHeader parentHeader) {
        return protocolContext
                .getConsensusContext(BftContext.class)
                .getValidatorProvider()
                .getValidatorsAfterBlock(parentHeader);
    }

    /**
     * Get the list of validators that are applicable for the given block.
     *
     * @param protocolContext the protocol context
     * @param parentHeader the parent header
     * @return the list of validators
     */
    public static Collection<Address> getValidatorsForBlock(
            final ProtocolContext protocolContext, final BlockHeader parentHeader) {
        return protocolContext
                .getConsensusContext(BftContext.class)
                .getValidatorProvider()
                .getValidatorsForBlock(parentHeader);
    }

    /**
     * Create message validator for a specific round.
     *
     * @param roundIdentifier the round identifier
     * @param parentHeader the parent header
     * @return the message validator
     */
    public MessageValidator createMessageValidator(
            final ConsensusRoundIdentifier roundIdentifier, final BlockHeader parentHeader) {

        // In Pure PoS, we validate proposals against the ProtocolSchedule (Header Rules).
        // Complex BFT SignedDataValidators are not needed for LCR.
        return new MessageValidator(
                protocolContext,
                protocolSchedule
        );
    }
}