package org.hyperledger.besu.consensus.pactus.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTransaction {
    private int transactionId;
    private int sender;
    private int receiver;
    private double amount;
}
