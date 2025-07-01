package org.hyperledger.besu.consensus.pactus.jsonrpc.methods;

import org.hyperledger.besu.consensus.pactus.queries.PactusQueryServiceImpl;
import org.hyperledger.besu.plugin.services.rpc.RpcMethod;

import java.util.Map;

public class PactusGetStakes implements RpcMethod {
    private final PactusQueryServiceImpl query;

    public PactusGetStakes(PactusQueryServiceImpl query) {
        this.query = query;
    }

    @Override
    public String getName() {
        return "pactus_getStakes";
    }

    @Override
    public Object execute(Object... args) {
        return query.getStakes();
    }
}