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

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hyperledger.besu.consensus.common.bft.BftBlockHeaderFunctions;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.pos.PosExtraDataCodec;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.core.Block;
import org.hyperledger.besu.ethereum.rlp.RLPInput;
import org.hyperledger.besu.ethereum.rlp.RLPOutput;

import java.io.IOException;
import java.util.Objects;
@Data
@Builder
//@NoArgsConstructor
//@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PosBlock {

  private final Block besuBlock;
  private final PosBlockHeader posBlockHeader;

  /**
   * Constructs a PosBlock from a Besu Block.
   *
   * @param besuBlock the Besu Block
   */
  public PosBlock(final Block besuBlock, ConsensusRoundIdentifier roundIdentifier, Address proposer) {
    this.besuBlock = besuBlock;
    this.posBlockHeader = new PosBlockHeader(besuBlock.getHeader(),roundIdentifier,proposer);
  }

  public PosBlock(final Block besuBlock, PosBlockHeader posBlockHeader) {
    this.besuBlock = besuBlock;
    this.posBlockHeader = posBlockHeader;
  }


  public PosBlockHeader getHeader() {
    return posBlockHeader;
  }


  public boolean isEmpty() {
    return besuBlock.getHeader().getTransactionsRoot().equals(Hash.EMPTY_TRIE_HASH);
  }

  public void writeTo(RLPOutput rlpOutput) throws JsonProcessingException {

    besuBlock.writeTo(rlpOutput);
    posBlockHeader.writeTo(rlpOutput);
  }

  public static PosBlock readFrom(RLPInput rlpInput) throws IOException {
    Block block= Block.readFrom(rlpInput, BftBlockHeaderFunctions.forCommittedSeal(new PosExtraDataCodec()));
    PosBlockHeader header= PosBlockHeader.readFrom(rlpInput,block.getHeader());
    return new PosBlock(block,header);
  }

  public Hash getHash() {
    return besuBlock.getHeader().getHash();
  }
}
