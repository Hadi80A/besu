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
package org.hyperledger.besu.consensus.pos.statemachine;

import lombok.Getter;
import lombok.Setter;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.config.PosConfigOptions;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.RoundTimer;
import org.hyperledger.besu.consensus.common.bft.payload.SignedData;
import org.hyperledger.besu.consensus.pos.*;
import org.hyperledger.besu.consensus.pos.core.*;
import org.hyperledger.besu.consensus.pos.messagewrappers.Propose;
import org.hyperledger.besu.consensus.pos.network.PosMessageTransmitter;
import org.hyperledger.besu.consensus.pos.payload.PosPayload;
import org.hyperledger.besu.consensus.pos.payload.ProposePayload;
import org.hyperledger.besu.crypto.SECPSignature;
import org.hyperledger.besu.cryptoservices.NodeKey;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.ProtocolContext;
import org.hyperledger.besu.ethereum.core.Block;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.core.BlockImporter;
import org.hyperledger.besu.ethereum.core.Util;
import org.hyperledger.besu.ethereum.mainnet.BlockImportResult;
import org.hyperledger.besu.ethereum.mainnet.HeaderValidationMode;
import org.hyperledger.besu.plugin.services.securitymodule.SecurityModuleException;
import org.hyperledger.besu.util.Subscribers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.time.Clock;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Setter
@Getter
public class PosRound {

    private static final Logger LOG = LoggerFactory.getLogger(PosRound.class);

    private final Subscribers<PosMinedBlockObserver> observers;
    private final RoundState roundState;
    private final PosBlockCreator blockCreator;
    private final PosConfigOptions posConfigOptions;

    protected final ProtocolContext protocolContext;
    protected final NodeSet nodeSet;
    protected final ContractCaller contractCaller;

    private final PosProtocolSchedule protocolSchedule;
    private final NodeKey nodeKey;
    private final PosRoundFactory.MessageFactory messageFactory;
    private final PosMessageTransmitter transmitter;
    private final PosExtraDataCodec posExtraDataCodec;
    private final BlockHeader parentHeader;

    private final PosProposerSelector posProposerSelector;
    private final PosFinalState posFinalState;
    private final Address localAddress;

    private PosBlockHeightManager posBlockHeightManager;

    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(
                    r -> {
                        Thread t = new Thread(r, "pos-round-executor");
                        t.setDaemon(true);
                        return t;
                    });

    public PosRound(
            final RoundState roundState,
            final PosBlockCreator blockCreator,
            final ProtocolContext protocolContext,
            final PosProtocolSchedule protocolSchedule,
            final Subscribers<PosMinedBlockObserver> observers,
            final NodeKey nodeKey,
            final PosRoundFactory.MessageFactory messageFactory,
            final PosMessageTransmitter transmitter,
            final RoundTimer roundTimer,
            PosConfigOptions posConfigOptions,
            final PosExtraDataCodec posExtraDataCodec,
            final BlockHeader parentHeader,
            final ContractCaller contractCaller,
            NodeSet nodeSet,
            PosProposerSelector posProposerSelector,
            PosFinalState posFinalState
    ) {
        this.roundState = roundState;
        this.blockCreator = blockCreator;
        this.protocolContext = protocolContext;
        this.protocolSchedule = protocolSchedule;
        this.observers = observers;
        this.nodeKey = nodeKey;
        this.messageFactory = messageFactory;
        this.transmitter = transmitter;
        this.posConfigOptions = posConfigOptions;
        this.posExtraDataCodec = posExtraDataCodec;
        this.parentHeader = parentHeader;
        this.contractCaller = contractCaller;
        this.nodeSet = nodeSet;
        this.localAddress = Util.publicKeyToAddress(nodeKey.getPublicKey());
        this.posProposerSelector = posProposerSelector;
        this.posFinalState = posFinalState;
    }

    public ConsensusRoundIdentifier getRoundIdentifier() {
        return roundState.getRoundIdentifier();
    }

    /**
     * Creates a candidate block from the tx pool.
     */
    public Block createBlock(final long headerTimeStampSeconds) {
        LOG.debug("Creating candidate block at timestamp {}", headerTimeStampSeconds);
        return blockCreator.createBlock(headerTimeStampSeconds, this.parentHeader);
    }

    /**
     * Creates a Signed Data wrapper for a payload (DSS/ECDSA).
     */
    public <M extends PosPayload> SignedData<M> createSignedData(M payload){
        SECPSignature sign = nodeKey.sign(payload.hashForSignature());
        LOG.debug("createSignedData");
        return SignedData.create(payload, sign);
    }

    private SignedData<ProposePayload> createProposePayload(Block block,ConsensusRoundIdentifier roundIdentifier) {
        ProposePayload proposePayload = messageFactory.createProposePayload(
                block,roundIdentifier);
        LOG.debug("createProposePayload");
        return createSignedData(proposePayload);
    }


    protected void createProposalAndTransmit(Clock clock) {
        long MIN_GAP_SECONDS = posConfigOptions.getBlockPeriodSeconds() / 5;
        long delayMs = Math.max(0L, (parentHeader.getTimestamp() + MIN_GAP_SECONDS) * 1000L - clock.millis());

        executor.schedule(() -> {
            try {
                // Verify we are the leader before creating (Safety Check)
                if (!posProposerSelector.isLocalProposer()) {
                    LOG.debug("Skipping proposal creation - not the leader.");
                    return;
                }

                long tsSec = Math.max(parentHeader.getTimestamp() + MIN_GAP_SECONDS, TimeUnit.MILLISECONDS.toSeconds(clock.millis()));
                Block posBlock = createBlock(tsSec);

                if (posBlock!=null) {
                    var proposePayload = createProposePayload(posBlock, getRoundIdentifier() );
                    LOG.info("Proposed block created. Round={}. Broadcasting...", getRoundIdentifier());

                    Propose proposal = messageFactory.createPropose(proposePayload);
                    transmitter.multicastProposal(proposal);
                    roundState.setProposeMessage(proposal);

                } else {
                    // Handle Empty Block logic if configured...
                    LOG.trace("Empty block created (skipped broadcast or handled otherwise)");
                }

            } catch (final SecurityModuleException e) {
                LOG.warn("Failed to create a signed Proposal", e);
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Phase 6: Fork Choice / Import
     * Finalizes the block by applying the Proposer's seal and submitting to the Core Importer.
     */
    public boolean importBlockToChain() {
        if (posProposerSelector.getCurrentProposer().isEmpty()) {
            LOG.warn("No proposer selected for importBlockToChain");
            return false;
        }

        // In LCR/FTS, the seal is just the proposer's signature (already available or signed now).
        // The 'commitSeals' argument is effectively the proposer's signature.
        // If importing our own block, we sign it now. If importing another's, we extract it.

        final Block blockToImport =
                blockCreator.createSealedBlock(
                        roundState.getProposedBlock(),
                        roundState.getRoundIdentifier().getRoundNumber(),
                        // For our own block, we sign. For received block, signature is in extraData already
                        // but createSealedBlock expects us to provide the signature to put in.
                        // Simplified: We assume createSealedBlock handles the signature generation for local.
                        roundState.getCommitSeals(),
                        posProposerSelector.getCurrentProposer().get()
                );

        final long blockNumber = blockToImport.getHeader().getNumber();

        LOG.info("Importing block to chain. Round={}, Hash={}", getRoundIdentifier(), blockToImport.getHash());

        final BlockImporter blockImporter =
                protocolSchedule.getBlockImporter(blockToImport.getHeader());

        BlockImportResult result = blockImporter.importBlock(protocolContext, blockToImport, HeaderValidationMode.FULL);

        if (result.isImported()) {
            LOG.info("Successfully imported block: number={}, hash={}, status={}",
                    blockNumber, blockToImport.getHash(), result.getStatus());
            notifyNewBlockListeners(blockToImport);
            return true;
        } else {
            LOG.error("Failed to import block. Number={}", blockNumber);
            return false;
        }
    }

    private void notifyNewBlockListeners(final Block block) {
        observers.forEach(obs -> obs.blockMined(block));
    }

    public void updateNodes(Block currentBlock){
        nodeSet.getAllNodes().forEach(node -> {
            BigInteger newStake= contractCaller.getValidatorStake(node.getAddress(),currentBlock);
            StakeInfo stakeInfo = new StakeInfo(newStake.longValue());
//      node.setStakeInfo(stakeInfo); //TODO uncomment

        });
    }
}