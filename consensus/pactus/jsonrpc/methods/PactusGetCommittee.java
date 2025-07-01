package org.hyperledger.besu.consensus.pactus.jsonrpc.methods;

import org.hyperledger.besu.consensus.pactus.queries.PactusQueryServiceImpl;
import org.hyperledger.besu.plugin.services.rpc.RpcMethod;
import java.util.List;

public class PactusGetCommittee implements RpcMethod {
    private final PactusQueryServiceImpl query;

    public PactusGetCommittee(PactusQueryServiceImpl query) {
        this.query = query;
    }

    @Override
    public String getName() {
        return "pactus_getCommittee";
    }

    @Override
    public Object execute(Object... args) {
        List<String> committee = query.getCommittee();
        return committee.toArray(new String[0]);
    }
}