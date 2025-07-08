package org.hyperledger.besu.consensus.pactus.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.besu.consensus.common.bft.payload.Payload;
import org.hyperledger.besu.consensus.pactus.core.PactusBlock;
import org.hyperledger.besu.crypto.SECPSignature;

@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class PactusPayload implements Payload {

    /** The round number in which the proposal is made. */
    protected int round;

    protected int height;


}
