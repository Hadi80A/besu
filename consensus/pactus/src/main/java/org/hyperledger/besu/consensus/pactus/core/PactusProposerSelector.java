package org.hyperledger.besu.consensus.pactus.core;

import lombok.Getter;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.bft.blockcreation.ProposerSelector;
import org.hyperledger.besu.datatypes.Address;

import javax.annotation.processing.Generated;
import java.util.List;

//@AllArgs
@Getter
public class PactusProposerSelector implements ProposerSelector {
    private ValidatorSet validatorSet;
    private Address currentProposer;
    @Override
    public Address selectProposerForRound(ConsensusRoundIdentifier roundIdentifier) {
        int size =validatorSet.committeeSize();
        List<Validator> committers=validatorSet.getCommitteeValidators();
        currentProposer =committers.get(roundIdentifier.getRoundNumber()+1 %size).getAddress();
        return currentProposer;
    }
}
