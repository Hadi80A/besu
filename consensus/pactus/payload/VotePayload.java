
package org.hyperledger.besu.consensus.pactus.payload;

public class VotePayload implements PactusPayload {
    private final String voter;
    private final int round;
    private final boolean approve;

    public VotePayload(String voter, int round, boolean approve) {
        this.voter = voter;
        this.round = round;
        this.approve = approve;
    }

    @Override
    public String getType() {
        return "Vote";
    }

    @Override
    public byte[] encode() {
        return (voter + ":" + round + ":" + approve).getBytes();
    }

    public String getVoter() {
        return voter;
    }

    public int getRound() {
        return round;
    }

    public boolean isApprove() {
        return approve;
    }
}
