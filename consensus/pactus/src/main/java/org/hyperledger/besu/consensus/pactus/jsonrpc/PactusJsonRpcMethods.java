// PactusJsonRpcMethods.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.jsonrpc;

import org.hyperledger.besu.consensus.pactus.queries.PactusQueryServiceImpl;
import org.hyperledger.besu.consensus.pactus.jsonrpc.methods.*;
import org.hyperledger.besu.ethereum.api.jsonrpc.RpcMethod;
import org.hyperledger.besu.ethereum.api.jsonrpc.RpcMethodRegistry;
import org.hyperledger.besu.ethereum.api.jsonrpc.RpcMethodCategory;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;

import java.util.Map;
import java.util.HashMap;

/**
 * Registers Pactus-specific JSON-RPC methods.
 */
public class PactusJsonRpcMethods implements RpcMethodRegistry {

  private final PactusQueryServiceImpl queryService;

  public PactusJsonRpcMethods(final PactusQueryServiceImpl queryService) {
    this.queryService = queryService;
  }

  @Override
  public Map<String, JsonRpcMethod> methods() {
    final Map<String, JsonRpcMethod> methods = new HashMap<>();

    methods.put("pactus_getValidators", new PactusGetValidators(queryService));
    methods.put("pactus_getValidatorById", new PactusGetValidatorById(queryService));
    methods.put("pactus_getCommittee", new PactusGetCommittee(queryService));
    methods.put("pactus_getCommitteeSize", new PactusGetCommitteeSize(queryService));
    methods.put("pactus_stake", new PactusStakeMethod());
    methods.put("pactus_unstake", new PactusUnstakeMethod());

    return methods;
  }

  @Override
  public String getNamespace() {
    return RpcMethodCategory.CONSENSUS.getValue();
  }
}
