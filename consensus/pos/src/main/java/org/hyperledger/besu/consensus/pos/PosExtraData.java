package org.hyperledger.besu.consensus.pos;

import lombok.Getter;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.consensus.common.bft.BftExtraData;
import org.hyperledger.besu.consensus.common.bft.Vote;
import org.hyperledger.besu.consensus.pos.bls.Bls;
import org.hyperledger.besu.consensus.pos.statemachine.QuorumCertificate;
import org.hyperledger.besu.crypto.SECPPublicKey;
import org.hyperledger.besu.crypto.SECPSignature;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.chain.Blockchain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
@Getter
public class PosExtraData extends BftExtraData {
    private final Address proposer;
    private final Collection<SECPPublicKey> publicKeys;
    private final Collection<Bls.PublicKey> blsPublicKeys;
    private final Collection<Bls.Signature> pops;
    private final QuorumCertificate quorumCertificate;
    private final Bytes32 seed;
    public PosExtraData(Bytes vanityData, Collection<SECPSignature> seals, Optional<Vote> vote, int round,
                        Collection<Address> validators, Address proposer, Collection<SECPPublicKey> publicKeys,
                        Collection<Bls.PublicKey> blsPublicKeys, Collection<Bls.Signature> pops, QuorumCertificate quorumCertificate,
                        Bytes32 seed) {
        super(vanityData, seals, vote, round, validators);
        this.proposer = proposer;
        this.publicKeys = publicKeys;
        this.blsPublicKeys = blsPublicKeys;
        this.pops=pops;
        this.quorumCertificate = quorumCertificate;
        this.seed=seed;
    }
}
