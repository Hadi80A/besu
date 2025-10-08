package org.hyperledger.besu.consensus.pos.validation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.mainnet.DetachedBlockHeaderValidationRule;

import java.util.function.Supplier;

public class ValidateHeightForBlock implements DetachedBlockHeaderValidationRule {
    private static final Logger log = LogManager.getLogger(ValidateHeightForBlock.class);
    private final Supplier<Blockchain> blockchainProvider;
    public ValidateHeightForBlock(Supplier<Blockchain> blockchainProvider) {
        this.blockchainProvider = blockchainProvider;
    }

    @Override
    public boolean validate(BlockHeader header, BlockHeader parent) {
        log.debug("header.getnumber = {} ,parent.getnumber= {}",header.getNumber(),parent.getNumber());

        return header.getNumber() != blockchainProvider.get().getChainHeadBlock().getHeader().getNumber();
    }
}
