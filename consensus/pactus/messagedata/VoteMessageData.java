
package org.hyperledger.besu.consensus.pactus.messagedata;

public class VoteMessageData {
    private final String voter;
    private final int round;
    private final boolean approve;

    public VoteMessageData(String voter, int round, boolean approve) {
        this.voter = voter;
        this.round = round;
        this.approve = approve;
    }

    public String getVoter() {
        return voter;
    }

    public int getRound() {
        return round;
    }

    public boolean isApproved() {
        return approve;
    }
}
