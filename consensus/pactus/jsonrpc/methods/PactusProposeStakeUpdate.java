package org.hyperledger.besu.consensus.pactus.jsonrpc.methods;

import org.hyperledger.besu.consensus.pactus.ValidatorMetadata;
import org.hyperledger.besu.consensus.pactus.ValidatorSet;
import org.hyperledger.besu.consensus.pactus.statemachine.CommitteeTracker;
import org.hyperledger.besu.plugin.services.rpc.RpcMethod;

public class PactusProposeStakeUpdate implements RpcMethod {
    private final ValidatorSet validatorSet;

    public PactusProposeStakeUpdate(ValidatorSet validatorSet) {
        this.validatorSet = validatorSet;
    }

    @Override
    public String getName() {
        return "pactus_proposeStakeUpdate";
    }

    @Override
    public Object execute(Object... args) {
        if (args.length != 2) return "Invalid parameters. Usage: (address, newStake)";
        String address = (String) args[0];
        double newStake = Double.parseDouble(args[1].toString());

        if (CommitteeTracker.getCurrentCommittee().contains(address)) {
            return "Rejected: Cannot change stake while in committee";
        }

        ValidatorMetadata existing = validatorSet.getValidator(address).orElse(null);
        if (existing == null) {
            validatorSet.addValidator(new ValidatorMetadata(address, newStake));
            return "Stake created for new validator";
        } else {
            validatorSet.addValidator(new ValidatorMetadata(address, newStake));
            return "Stake updated";
        }
    }
}