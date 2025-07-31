package org.hyperledger.besu.consensus.pos.statemachine;

import lombok.AllArgsConstructor;
import org.hyperledger.besu.consensus.pos.core.NodeSet;
import org.hyperledger.besu.datatypes.Address;
@AllArgsConstructor
public class PosProposerSelector {
    private NodeSet nodeSet;
    private Address currentProposer;
    public Address selectLeader(){

        currentProposer= nodeSet.getAllNodes().stream().findFirst().get().getAddress();
        return currentProposer;
    }

    public Address getCurrentProposer() {
        return currentProposer;
    }
}
