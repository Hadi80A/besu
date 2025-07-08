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
package org.hyperledger.besu.consensus.pactus.core;

import org.hyperledger.besu.consensus.common.bft.BftBlockHeaderFunctions;
import org.hyperledger.besu.consensus.common.bft.BftExtraData;
import org.hyperledger.besu.consensus.common.bft.BftExtraDataCodec;
import org.hyperledger.besu.crypto.SECPSignature;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.blockcreation.BlockCreator;
import org.hyperledger.besu.ethereum.core.Block;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.core.BlockHeaderBuilder;

import java.util.Collection;

/** Adaptor class to allow a {@link BlockCreator} to be used as a {@link PactusBlockCreator}. */
public class PactusBlockCreator{

  private final BlockCreator besuBlockCreator;
  private final BftExtraDataCodec bftExtraDataCodec;

  /**
   * Constructs a new PactusBlockCreator
   *
   * @param besuBftBlockCreator the Besu BFT block creator
   * @param bftExtraDataCodec the bftExtraDataCodec used to encode extra data for the new header
   */
  public PactusBlockCreator(
      final BlockCreator besuBftBlockCreator, final BftExtraDataCodec bftExtraDataCodec) {
    this.besuBlockCreator = besuBftBlockCreator;
    this.bftExtraDataCodec = bftExtraDataCodec;
  }

  public PactusBlock createBlock(
          final long headerTimeStampSeconds, final PactusBlockHeader parentHeader, Address proposer) {
    var blockResult =
        besuBlockCreator.createBlock(
            headerTimeStampSeconds,parentHeader.getBesuHeader());
    PactusBlockHeader newHeader= createHeader(parentHeader, blockResult,proposer);
    return new PactusBlock(blockResult.getBlock(),newHeader,null);
  }

  private static PactusBlockHeader createHeader(PactusBlockHeader parentHeader, BlockCreator.BlockCreationResult blockResult , Address proposer) {
    long newSeed= 0;//TODO : generate new Seed
    return PactusBlockHeader.builder()
            .version(parentHeader.getVersion())
            .sortitionSeed(newSeed)
            .proposer(proposer)
            .besuHeader(blockResult.getBlock().getHeader())
            .build();
  }

  public PactusBlock createSealedBlock(
      final PactusBlock block, final int roundNumber, final Collection<SECPSignature> commitSeals) {
    final Block besuBlock = block.getBesuBlock();
    final PactusBlockHeader initialHeader = block.getPactusHeader();
    final BftExtraData initialExtraData =
        bftExtraDataCodec.decode(initialHeader.getBesuHeader());

    final BftExtraData sealedExtraData =
        new BftExtraData(
            initialExtraData.getVanityData(),
            commitSeals,
            initialExtraData.getVote(),
            roundNumber,
            initialExtraData.getValidators());

    final BlockHeader sealedHeader =
        BlockHeaderBuilder.fromHeader(initialHeader.getBesuHeader())
            .extraData(bftExtraDataCodec.encode(sealedExtraData))
            .blockHeaderFunctions(BftBlockHeaderFunctions.forOnchainBlock(bftExtraDataCodec))
            .buildBlockHeader();
    final Block sealedBesuBlock = new Block(sealedHeader, besuBlock.getBody());
    return new PactusBlock(sealedBesuBlock,initialHeader,null);
  }
}
