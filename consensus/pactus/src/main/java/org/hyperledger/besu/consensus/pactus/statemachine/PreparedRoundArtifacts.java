package org.hyperledger.besu.consensus.pactus.statemachine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.besu.consensus.pactus.core.Block;
import org.hyperledger.besu.consensus.pactus.payload.PreCommitPayload;

import java.util.List;

/**
 * Stores the proposal and pre-commit votes that justify a previously prepared round.
 * Used during round change and recovery phases.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreparedRoundArtifacts {

  /** The block proposed and prepared in the prior round. */
  private Block preparedBlock;

  /** List of pre-commit votes supporting this block from the previous round. */
  private List<PreCommitPayload> preCommits;
}
