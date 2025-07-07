package org.hyperledger.besu.consensus.pactus.core;

import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.blockcreation.ProposerSelector;
import org.hyperledger.besu.datatypes.Address;

import java.util.List;

//@AllArgs
public class PactusProposerSelector implements ProposerSelector {
    private ValidatorSet validatorSet;
    @Override
    public Address selectProposerForRound(ConsensusRoundIdentifier roundIdentifier) {
        int size =validatorSet.committeeSize();
        List<Validator> committers=validatorSet.getCommitteeValidators();
        return committers.get(roundIdentifier.getRoundNumber()+1 %size).getAddress();
    }
}
