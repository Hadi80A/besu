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
package org.hyperledger.besu.consensus.pos;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hyperledger.besu.consensus.common.bft.BftBlockHeaderFunctions;
import org.hyperledger.besu.consensus.common.bft.BftExtraData;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.pos.core.PosBlock;
import org.hyperledger.besu.consensus.pos.core.PosBlockHeader;
import org.hyperledger.besu.crypto.SECPSignature;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.blockcreation.BlockCreator;
import org.hyperledger.besu.ethereum.core.Block;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.core.BlockHeaderBuilder;

import java.util.Collection;
@Data
@Builder
//@NoArgsConstructor
//@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PosBlockCreator {

  private static final Logger log = LogManager.getLogger(PosBlockCreator.class);
  private final BlockCreator besuBlockCreator;
  private final PosExtraDataCodec posExtraDataCodec;


  public PosBlock createBlock(
          final long headerTimeStampSeconds, final PosBlockHeader parentHeader, Address proposer) {
    var blockResult = besuBlockCreator.createBlock(headerTimeStampSeconds, parentHeader.getBesuBlockHeader());

    log.info("transaction size:{}",blockResult.getTransactionSelectionResults().getSelectedTransactions().size());
    var round=new ConsensusRoundIdentifier(
            parentHeader.getRoundIdentifier().getSequenceNumber()+1,
            parentHeader.getRoundIdentifier().getRoundNumber()+1
    );
//    PosBlockHeader header=PosBlockHeader.builder()
//            .besuHeader(blockResult.getBlock().getHeader())
//            .roundIdentifier(round)
//            .proposer(proposer)
//            .build();
    return new PosBlock(blockResult.getBlock(),round,proposer);
  }

  public PosBlock createSealedBlock(
      final PosBlock block, final int roundNumber, final Collection<SECPSignature> commitSeals,Address propser) {
    final Block besuBlock = BlockUtil.toBesuBlock(block);
    final PosBlockHeader initialHeader = block.getHeader();
    final BftExtraData initialExtraData =
        posExtraDataCodec.decode(BlockUtil.toBesuBlockHeader(initialHeader));

    final PosExtraData sealedExtraData =
        new PosExtraData(
            initialExtraData.getVanityData(),
            commitSeals,
            initialExtraData.getVote(),
            roundNumber,
            initialExtraData.getValidators(),
            propser
        );

    final BlockHeader sealedHeader =
        BlockHeaderBuilder.fromHeader(BlockUtil.toBesuBlockHeader(initialHeader))
            .extraData(posExtraDataCodec.encodePosData(sealedExtraData))
            .blockHeaderFunctions(BftBlockHeaderFunctions.forOnchainBlock(posExtraDataCodec))
            .buildBlockHeader();
    final Block sealedBesuBlock = new Block(sealedHeader, besuBlock.getBody());
    return new PosBlock(sealedBesuBlock,block.getPosBlockHeader());
  }
}
