package org.hyperledger.besu.consensus.pactus.jsonrpc;

import java.util.*;
import org.hyperledger.besu.plugin.services.rpc.RpcMethod;
import org.hyperledger.besu.plugin.services.rpc.RpcMethodCategory;
import org.hyperledger.besu.plugin.services.rpc.RpcMethodMap;
import org.hyperledger.besu.consensus.pactus.ValidatorSet;
import org.hyperledger.besu.consensus.pactus.queries.PactusQueryServiceImpl;
import org.hyperledger.besu.consensus.pactus.jsonrpc.methods.*;

public class PactusJsonRpcMethods extends RpcMethodMap {
    public PactusJsonRpcMethods(ValidatorSet validatorSet) {
        super(RpcMethodCategory.CONSENSUS);
        PactusQueryServiceImpl queryService = new PactusQueryServiceImpl(validatorSet);

        add(new PactusGetCommittee(queryService));
        add(new PactusGetStakes(queryService));
        add(new PactusProposeStakeUpdate(validatorSet));
    }
}