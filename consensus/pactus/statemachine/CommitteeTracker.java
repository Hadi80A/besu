package org.hyperledger.besu.consensus.pactus.statemachine;

import java.util.HashSet;
import java.util.Set;

public class CommitteeTracker {
    private static final Set<String> currentCommittee = new HashSet<>();

    public static void setCurrentCommittee(Set<String> validators) {
        currentCommittee.clear();
        currentCommittee.addAll(validators);
    }

    public static Set<String> getCurrentCommittee() {
        return currentCommittee;
    }
}