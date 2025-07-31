package org.hyperledger.besu.consensus.pos;

import lombok.Getter;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.consensus.common.bft.BftExtraData;
import org.hyperledger.besu.consensus.common.bft.Vote;
import org.hyperledger.besu.crypto.SECPSignature;
import org.hyperledger.besu.datatypes.Address;

import java.util.Collection;
import java.util.Optional;

public class PosExtraData extends BftExtraData {
    @Getter
    private final Address proposer;
    public PosExtraData(Bytes vanityData, Collection<SECPSignature> seals, Optional<Vote> vote, int round,
                        Collection<Address> validators, Address proposer) {
        super(vanityData, seals, vote, round, validators);
        this.proposer = proposer;
    }
}
