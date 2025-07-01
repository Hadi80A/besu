
package org.hyperledger.besu.consensus.pactus;

import org.hyperledger.besu.ethereum.core.ProtocolSchedule;
import org.hyperledger.besu.ethereum.mainnet.ProtocolScheduleBuilder;

public class PactusProtocolScheduleBuilder {
    public static ProtocolSchedule createSchedule() {
        return new ProtocolScheduleBuilder().build();
    }
}
