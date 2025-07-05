// Proposal.java - placeholder for Pactus consensus implementation
package org.hyperledger.besu.consensus.pactus.messagewrappers;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.consensus.common.bft.BftBlockHeaderFunctions;
import org.hyperledger.besu.consensus.common.bft.messagewrappers.BftMessage;
import org.hyperledger.besu.consensus.common.bft.payload.SignedData;
import org.hyperledger.besu.consensus.pactus.core.PactusBlock;
import org.hyperledger.besu.consensus.pactus.payload.ProposePayload;
import org.hyperledger.besu.ethereum.core.Block;
import org.hyperledger.besu.ethereum.rlp.BytesValueRLPOutput;
import org.hyperledger.besu.ethereum.rlp.RLP;
import org.hyperledger.besu.ethereum.rlp.RLPInput;

import java.util.List;
import java.util.Optional;

/**
 * Represents the wrapper for a proposal in Pactus consensus.
 * This is the first message broadcast in a new round by the selected proposer.
 */
@Data
@Builder

public class Proposal extends BftMessage<ProposePayload> {

  /** The ID (or public key) of the proposer. */
  private int proposerId =-1;



  public Proposal(SignedData<ProposePayload> payload, int proposerId) {
    super(payload);
    this.proposerId = proposerId;
  }

  /**
   * Validates that the proposal message is complete and structurally correct.
   */
  public boolean isValid() {
    return proposerId != -1 &&
           getPayload() != null;
  }


  @Override
  public Bytes encode() {
    final BytesValueRLPOutput rlpOut = new BytesValueRLPOutput();
    rlpOut.writeInt(proposerId);
    getSignedPayload().writeTo(rlpOut);
    return rlpOut.encoded();


  }

  /**
   * Decode.
   *
   * @param data the data
   * @return the proposal
   */

  public static Proposal decode(final Bytes data) {
    final RLPInput rlpIn = RLP.input(data);
    int proposerId= rlpIn.readInt();
    rlpIn.enterList();
    final SignedData<ProposePayload> payload = readPayload(rlpIn, rlpInput -> ProposePayload.readFrom(rlpInput, blockEncoder));
    final Block proposedBlock =
            Block.readFrom(rlpIn, BftBlockHeaderFunctions.forCommittedSeal(BFT_EXTRA_DATA_ENCODER));

    final Optional<RoundChangeCertificate> roundChangeCertificate =
            readRoundChangeCertificate(rlpIn);

    rlpIn.leaveList();
    return new Proposal(payload, proposedBlock, roundChangeCertificate);

//    final RLPInput rlpIn = RLP.input(data);
//    rlpIn.enterList();
//    final SignedData<ProposalPayload> payload =
//            readPayload(rlpIn, rlpInput -> ProposalPayload.readFrom(rlpInput, blockEncoder));
//
//    rlpIn.enterList();
//    final List<SignedData<RoundChangePayload>> roundChanges =
//            rlpIn.readList(r -> readPayload(r, RoundChangePayload::readFrom));
//    final List<SignedData<PreparePayload>> prepares =
//            rlpIn.readList(r -> readPayload(r, PreparePayload::readFrom));
//    rlpIn.leaveList();
//
//    rlpIn.leaveList();
//    return new Proposal(payload, roundChanges, prepares);
  }

}
