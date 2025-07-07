
package org.hyperledger.besu.consensus.pactus.validation;

import org.hyperledger.besu.consensus.pactus.core.PactusBlock;
import org.hyperledger.besu.ethereum.BlockProcessingResult;
import org.hyperledger.besu.ethereum.BlockValidator;
import org.hyperledger.besu.ethereum.ProtocolContext;
import org.hyperledger.besu.ethereum.mainnet.HeaderValidationMode;

import java.util.Optional;

/** Adaptor class to allow a {@link BlockValidator} to be used as a {@link PactusBlockValidator}. */
public class PactusBlockValidator {

  private final BlockValidator blockValidator;
  private final ProtocolContext protocolContext;

  /**
   * Constructs a new Pactus block validator
   *
   * @param blockValidator The Besu block validator
   * @param protocolContext The protocol context
   */
  public PactusBlockValidator(
      final BlockValidator blockValidator, final ProtocolContext protocolContext) {
    this.blockValidator = blockValidator;
    this.protocolContext = protocolContext;
  }

  public ValidationResult validateBlock(final PactusBlock block) {
    final BlockProcessingResult blockProcessingResult =
        blockValidator.validateAndProcessBlock(
            protocolContext,
            block.getBesuBlock(),
            HeaderValidationMode.LIGHT,
            HeaderValidationMode.FULL,
            false);
    return new ValidationResult(
        blockProcessingResult.isSuccessful(), blockProcessingResult.errorMessage);
  }


  /**
   * The result of a block validation.
   *
   * @param success whether the validation was successful
   * @param errorMessage the error message if the validation was not successful
   */
  record ValidationResult(boolean success, Optional<String> errorMessage) {}
}
