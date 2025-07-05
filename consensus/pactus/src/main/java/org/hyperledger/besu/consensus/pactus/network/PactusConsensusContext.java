package org.hyperledger.besu.consensus.pactus.network;

import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.consensus.pactus.core.ValidatorSet;
import org.hyperledger.besu.crypto.SECP256K1;
import org.hyperledger.besu.crypto.SECPSignature;
import org.hyperledger.besu.crypto.SignatureAlgorithmFactory;
import org.hyperledger.besu.cryptoservices.NodeKey;
import org.hyperledger.besu.ethereum.ConsensusContext;
import org.hyperledger.besu.ethereum.core.Difficulty;
import org.hyperledger.besu.crypto.Hash;

import java.nio.charset.StandardCharsets;

public class PactusConsensusContext implements ConsensusContext {
    private final ValidatorSet nodeSet;
    private final NodeKey nodeKey;

    public PactusConsensusContext(ValidatorSet nodeSet, NodeKey nodeKey) {
        this.nodeSet = nodeSet;
        this.nodeKey = nodeKey;
    }

    public ValidatorSet getValidatorSet() {
        return nodeSet;
    }

    public String getWalletAddress() {
        Bytes publicKeyBytes = nodeKey.getPublicKey().getEncodedBytes();
        return "0x" + Hash.keccak256(publicKeyBytes).toHexString().substring(24);
    }

    public String signMessage(String id, String address) {
        String message = id + ":" + address;
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        SECPSignature signature = SignatureAlgorithmFactory.getInstance()
            .sign(messageBytes, nodeKey.getKeyPair());
        return signature.toString(); // Hex-encoded signature
    }

    @Override
    public <C extends ConsensusContext> C as(Class<C> klass) {
        return null;
    }

    @Override
    public void setIsPostMerge(Difficulty totalDifficulty) {
        ConsensusContext.super.setIsPostMerge(totalDifficulty);
    }
}