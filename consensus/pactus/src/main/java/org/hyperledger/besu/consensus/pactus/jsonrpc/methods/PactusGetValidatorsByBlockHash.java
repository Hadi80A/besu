// PactusGetValidatorsByBlockHash.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.jsonrpc.methods;

import org.hyperledger.besu.consensus.pactus.core.Validator;
import org.hyperledger.besu.consensus.pactus.queries.PactusQueryServiceImpl;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.response.RpcErrorType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JSON-RPC method to retrieve validators by block hash.
 * (In this simplified version, returns current validators regardless of hash.)
 */
public class PactusGetValidatorsByBlockHash implements JsonRpcMethod {

  private final PactusQueryServiceImpl queryService;

  public PactusGetValidatorsByBlockHash(final PactusQueryServiceImpl queryService) {
    this.queryService = queryService;
  }

  @Override
  public String getName() {
    return "pactus_getValidatorsByBlockHash";
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext request) {
    try {
      // NOTE: In real implementation, you'd lookup by blockHash.
      List<Map<String, Object>> validatorList = queryService.getAllValidators().stream()
          .map(this::toMap)
          .collect(Collectors.toList());

      return new JsonRpcSuccessResponse(request.getRequest().getId(), validatorList);

    } catch (Exception e) {
      return new JsonRpcErrorResponse(request.getRequest().getId(), RpcErrorType.INTERNAL_ERROR);
    }
  }

  private Map<String, Object> toMap(Validator validator) {
    return Map.of(
        "id", validator.getId(),
        "stake", validator.getStake(),
        "isInCommittee", validator.isInCommittee()
    );
  }
}
