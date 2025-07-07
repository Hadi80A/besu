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
import org.hyperledger.besu.consensus.pactus.PactusBlockCodec;
import org.hyperledger.besu.consensus.pactus.PactusExtraDataCodec;
import org.hyperledger.besu.consensus.pactus.core.PactusBlock;
import org.hyperledger.besu.consensus.pactus.payload.ProposePayload;
import org.hyperledger.besu.ethereum.core.Block;
import org.hyperledger.besu.ethereum.rlp.BytesValueRLPOutput;
import org.hyperledger.besu.ethereum.rlp.RLP;
import org.hyperledger.besu.ethereum.rlp.RLPInput;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Represents the wrapper for a proposal in Pactus consensus.
 * This is the first message broadcast in a new round by the selected proposer.
 */
public class Proposal extends BftMessage<ProposePayload> {

    /**
     * The ID (or public key) of the proposer.
     */
    private int proposerId = -1;
    private static final PactusBlockCodec pactusBlockCodec = new PactusBlockCodec(new PactusExtraDataCodec());

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

    public static Proposal decode(final Bytes data) {
        final RLPInput rlpIn = RLP.input(data);
        int proposerId = rlpIn.readInt();
        rlpIn.enterList();

        final SignedData<ProposePayload> payload = readPayload(rlpIn, rlpInput -> {
            try {
                return ProposePayload.readFrom(rlpInput, pactusBlockCodec);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return new Proposal(payload, proposerId);

    }

}
