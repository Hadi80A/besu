package org.hyperledger.besu.consensus.pos.statemachine;

import lombok.AllArgsConstructor;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.ProtocolContext;
import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.core.Block;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.worldstate.WorldStateArchive;
import org.hyperledger.besu.evm.account.Account;
import org.hyperledger.besu.evm.worldstate.WorldState;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

@AllArgsConstructor
public class ContractCaller {
    private Address contractAddress;
    private ProtocolContext protocolContext;
//    private void printStake(Block block){
//        WorldStateArchive worldStateArchive = protocolContext.getWorldStateArchive();
//        Blockchain blockchain = protocolContext.getBlockchain();
//
//        BlockHeader header =block.getHeader();
//
//        WorldState worldState =
//                worldStateArchive
//                        .get(header.getStateRoot(), header.getHash())
//                        .orElseThrow(() -> new RuntimeException("Genesis state not available"));
//        Address nodeAddress= Util.publicKeyToAddress(nodeKey.getPublicKey());
//        Account account = worldState.get(nodeAddress);
//        BigInteger balanceWei =
//                account != null ? account.getBalance().toBigInteger() : BigInteger.ZERO;
//
//        BigDecimal balanceEth = weiToEth(balanceWei);
//        Address stakeManager = Address.fromHexString("0x1234567890123456789012345678901234567890");
//
//        // Get stake from contract
//        BigInteger stakeWei = getValidatorStake(worldState, stakeManager, nodeAddress);
//        BigDecimal stakeEth = weiToEth(stakeWei);
//        System.out.printf(
//                "%-20s | %-42s | %-15s | %-15s%n",
//                "Validator ID", "Address", "Balance (ETH)", "Stake (ETH)");
//        System.out.println(
//                "---------------------------------------------------------------------------");
//        System.out.printf(
//                "%-20s | %-42s | %-15s | %-15s%n",
//                0, nodeAddress.toHexString(), balanceEth.toString(), stakeEth.toString());
//    }

    private BigDecimal weiToEth(BigInteger wei) {
        return new BigDecimal(wei)
                .divide(new BigDecimal("1000000000000000000"), 6, RoundingMode.HALF_UP.ordinal());
    }

    public BigInteger getValidatorStake(Address validatorAddress, Block block) {
        WorldStateArchive worldStateArchive = protocolContext.getWorldStateArchive();
//        Blockchain blockchain = protocolContext.getBlockchain();

        BlockHeader header =block.getHeader();

        WorldState worldState =
                worldStateArchive
                        .get(header.getStateRoot(), header.getHash())
                        .orElseThrow(() -> new RuntimeException("Genesis state not available"));
        // 1. Get contract account
        Account contractAccount = worldState.get(contractAddress);
        if (contractAccount == null || contractAccount.isEmpty()) {
            System.out.println("contractAccount is null or empty");
            return BigInteger.ZERO;
        }

        // 2. Compute storage slot for validator's stake
        // Slot = keccak256(validatorAddress + slot_index)
        // slot_index = 0 (first slot in the contract storage layout)
        Bytes32 key = Bytes32.leftPad(validatorAddress);
        Bytes32 slotIndex = Bytes32.leftPad(Bytes.of(0)); // Slot 0 for mapping
        Bytes concatenated = Bytes.concatenate(key, slotIndex);
        Bytes32 slotHash = org.hyperledger.besu.crypto.Hash.keccak256(concatenated);

        // 3. Read storage value at computed slot
        UInt256 stakeValue =
                contractAccount.getStorageValue(UInt256.valueOf(slotHash.toUnsignedBigInteger()));
        return stakeValue.toBigInteger();
    }

}
