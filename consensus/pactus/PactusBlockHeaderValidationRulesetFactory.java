
package org.hyperledger.besu.consensus.pactus;

import org.hyperledger.besu.consensus.common.BlockHeaderValidator;
import org.hyperledger.besu.datatypes.BlockHeader;

public class PactusBlockHeaderValidationRulesetFactory {
    public static BlockHeaderValidator create() {
        return new BlockHeaderValidator() {
            @Override
            public boolean validate(BlockHeader header) {
                // Simulate signature, proposer check, timestamp rule etc.
                return header.getTimestamp() > 0 && header.getProposer().isPresent();
            }
        };
    }
}
