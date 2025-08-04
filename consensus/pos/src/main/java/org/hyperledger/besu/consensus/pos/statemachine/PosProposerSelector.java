package org.hyperledger.besu.consensus.pos.statemachine;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hyperledger.besu.consensus.pos.core.NodeSet;
import org.hyperledger.besu.datatypes.Address;
@RequiredArgsConstructor
public class PosProposerSelector {
    private final NodeSet nodeSet;

    @Getter
    private Address currentProposer;

    public Address selectLeader(){
        currentProposer= nodeSet.getAllNodes().stream().findFirst().get().getAddress();
        System.out.println("Current Proposer: " + currentProposer);
        return currentProposer;
    }

}
