package org.hyperledger.besu.consensus.pactus.network;

import org.hyperledger.besu.consensus.pactus.core.Validator;
import org.hyperledger.besu.crypto.SECPPublicKey;
import org.hyperledger.besu.crypto.SECPSignature;
import org.hyperledger.besu.cryptoservices.NodeKey;
import org.hyperledger.besu.crypto.SECP256K1;
import org.hyperledger.besu.crypto.SignatureAlgorithmFactory;
import org.hyperledger.besu.ethereum.p2p.network.ProtocolManager;
import org.hyperledger.besu.ethereum.p2p.rlpx.connections.PeerConnection;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.MessageData;

import java.nio.charset.StandardCharsets;

public class PactusMessageHandler {//implements ProtocolManager.MessageHandler {
//    private final PactusConsensusContext consensusContext;
//
//    public PactusMessageHandler(PactusConsensusContext consensusContext) {
//        this.consensusContext = consensusContext;
//    }
//
//    @Override
//    public void processMessage(PeerConnection peer, MessageData message) {
//        if (message.getCode() == 0x01) { // Registration message code
//            String data = new String(message.getData(), StandardCharsets.UTF_8);
//            String[] parts = data.split(":");
//            if (parts.length == 4 && parts[0].equals("REGISTER")) {
//                String id = parts[1];
//                String address = parts[2];
//                String signature = parts[3];
//
//                if (verifySignature(id, address, signature)) {
//                    Validator newNode = new Validator(id, address);
//                    consensusContext.getPactusNodeSet().addOrUpdatePactusNode(newNode);
//                }
//            }
//        }
//    }
//
//    private boolean verifySignature(String id, String address, String signature) {
//        // Verify the signature using Besu's crypto utilities
//        String message = id + ":" + address;
//        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
//        try {
//            SECPSignature sig = SECPSignature.decodeFromHex(signature);
//            SECPPublicKey recoveredKey = SignatureAlgorithmFactory.getInstance()
//                .recoverPublicKeyFromSignature(messageBytes, sig)
//                .orElseThrow(() -> new IllegalArgumentException("Signature verification failed"));
//            String recoveredAddress = "0x" + org.hyperledger.besu.crypto.Hash.keccak256(recoveredKey.getEncodedBytes())
//                .toHexString()
//                .substring(24);
//            return recoveredAddress.equalsIgnoreCase(address);
//        } catch (Exception e) {
//            return false; // Invalid signature
//        }
//    }
}