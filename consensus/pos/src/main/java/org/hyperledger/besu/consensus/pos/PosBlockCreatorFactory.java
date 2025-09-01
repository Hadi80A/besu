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

import org.hyperledger.besu.consensus.common.bft.blockcreation.BftBlockCreatorFactory;

/**
 * Adaptor class to allow a {@link BftBlockCreatorFactory} to be used as a {@link
 * PosBlockCreatorFactory}.
 */
public class PosBlockCreatorFactory {

  private final BftBlockCreatorFactory<?> posBlockCreatorFactory;
  private final PosExtraDataCodec posExtraDataCodec;


  public PosBlockCreatorFactory(
      final BftBlockCreatorFactory<?> bftBlockCreatorFactory,
      final PosExtraDataCodec posExtraDataCodec) {
    this.posBlockCreatorFactory = bftBlockCreatorFactory;
    this.posExtraDataCodec = posExtraDataCodec;
  }

  public PosBlockCreator create(final int roundNumber) {
    return new PosBlockCreator(
        posBlockCreatorFactory.create(roundNumber), posExtraDataCodec);
  }
}
