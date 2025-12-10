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
package org.hyperledger.besu.consensus.nexus.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hyperledger.besu.consensus.common.bft.BftBlockHeaderFunctions;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.nexus.NexusExtraDataCodec;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.core.Block;
import org.hyperledger.besu.ethereum.rlp.RLPInput;
import org.hyperledger.besu.ethereum.rlp.RLPOutput;

import java.io.IOException;

@Data
@Builder
//@NoArgsConstructor
//@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class NexusBlock {

  private final Block besuBlock;
  private final NexusBlockHeader nexusBlockHeader;

  /**
   * Constructs a NexusBlock from a Besu Block.
   *
   * @param besuBlock the Besu Block
   */
  public NexusBlock(final Block besuBlock, ConsensusRoundIdentifier roundIdentifier, Address proposer) {
    this.besuBlock = besuBlock;
    this.nexusBlockHeader = new NexusBlockHeader(besuBlock.getHeader(),roundIdentifier,proposer);
  }

  public NexusBlock(final Block besuBlock, NexusBlockHeader nexusBlockHeader) {
    this.besuBlock = besuBlock;
    this.nexusBlockHeader = nexusBlockHeader;
  }


  public NexusBlockHeader getHeader() {
    return nexusBlockHeader;
  }


  public boolean isEmpty() {
    return besuBlock.getHeader().getTransactionsRoot().equals(Hash.EMPTY_TRIE_HASH);
  }

  public void writeTo(RLPOutput rlpOutput) throws JsonProcessingException {

    besuBlock.writeTo(rlpOutput);
    nexusBlockHeader.writeTo(rlpOutput);
  }

  public static NexusBlock readFrom(RLPInput rlpInput) throws IOException {
//    Block block= Block.readFrom(rlpInput, BftBlockHeaderFunctions.forCommittedSeal(new NexusExtraDataCodec()));
    Block block= Block.readFrom(rlpInput, BftBlockHeaderFunctions.forCommittedSeal(new NexusExtraDataCodec()));
    NexusBlockHeader header= NexusBlockHeader.readFrom(rlpInput,block.getHeader());
    return new NexusBlock(block,header);
  }

  public Hash getHash() {
    return besuBlock.getHeader().getHash();
  }
}
