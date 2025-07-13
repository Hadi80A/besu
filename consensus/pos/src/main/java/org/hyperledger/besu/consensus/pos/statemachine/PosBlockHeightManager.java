package org.hyperledger.besu.consensus.pos.statemachine;

import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.events.RoundExpiry;
import org.hyperledger.besu.consensus.common.bft.statemachine.BaseBlockHeightManager;
import org.hyperledger.besu.ethereum.core.BlockHeader;

public class PosBlockHeightManager implements BaseBlockHeightManager {
    private final BlockHeader parentHeader;

    public PosBlockHeightManager(BlockHeader parentHeader) {
        this.parentHeader = parentHeader;
    }


    @Override
    public void handleBlockTimerExpiry(ConsensusRoundIdentifier roundIdentifier) {

    }

    @Override
    public void roundExpired(RoundExpiry expire) {

    }

    @Override
    public long getChainHeight() {
        return 0;
    }

    @Override
    public BlockHeader getParentBlockHeader() {
        return parentHeader;
    }
}
