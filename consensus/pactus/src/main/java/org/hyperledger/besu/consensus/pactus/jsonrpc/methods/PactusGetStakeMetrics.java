// PactusGetStakeMetrics.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.jsonrpc.methods;

import org.hyperledger.besu.consensus.pactus.queries.PactusQueryServiceImpl;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.response.RpcErrorType;

import java.util.HashMap;
import java.util.Map;

/**
 * JSON-RPC method to return metrics about current staking distribution and committee size.
 */
public class PactusGetStakeMetrics implements JsonRpcMethod {

  private final PactusQueryServiceImpl queryService;

  public PactusGetStakeMetrics(final PactusQueryServiceImpl queryService) {
    this.queryService = queryService;
  }

  @Override
  public String getName() {
    return "pactus_getStakeMetrics";
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext request) {
    try {
      int totalValidators = queryService.getTotalValidatorCount();
      int committeeSize = queryService.getCommitteeSize();

      Map<String, Object> result = new HashMap<>();
      result.put("totalValidators", totalValidators);
      result.put("committeeSize", committeeSize);
      result.put("quorum", (2 * totalValidators / 3) + 1);

      return new JsonRpcSuccessResponse(request.getRequest().getId(), result);

    } catch (Exception e) {
      return new JsonRpcErrorResponse(request.getRequest().getId(), RpcErrorType.INTERNAL_ERROR);
    }
  }
}
