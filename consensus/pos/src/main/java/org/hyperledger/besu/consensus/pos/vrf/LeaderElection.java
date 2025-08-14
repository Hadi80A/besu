package org.hyperledger.besu.consensus.pos.vrf;

import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.KeyPairGenerator;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LeaderElection {

    private static final double LAMBDA =1.0; // برای دمو می‌توانید 2.0 یا 3.0 بگذارید

    public static class Validator {
        private final String id;
        private final long stake;
        private final byte[] publicKey;  // 32 bytes (RFC8032)
        private final byte[] privateKey; // 32-byte seed

        public Validator(String id, long stake) {
            this.id = id;
            this.stake = stake;
            KeyPairGenerator kpg = new KeyPairGenerator();
            java.security.KeyPair kp = kpg.generateKeyPair();
            this.publicKey  = ((EdDSAPublicKey)  kp.getPublic()).getAbyte(); // ✅ 32 bytes
            this.privateKey = ((EdDSAPrivateKey) kp.getPrivate()).getSeed(); // ✅ 32 bytes
        }

        public String getId() { return id; }
        public long getStake() { return stake; }
        public byte[] getPublicKey() { return publicKey; }
        public byte[] getPrivateKey() { return privateKey; }
    }

    public static class Candidate {
        final Validator validator;
        final byte[] vrfProof;
        final byte[] vrfHash;
        final byte[] seed;
        public Candidate(Validator validator, byte[] vrfProof, byte[] vrfHash, byte[] seed) {
            this.validator = validator; this.vrfProof = vrfProof; this.vrfHash = vrfHash; this.seed = seed;
        }
        public Validator getValidator() { return validator; }
        public byte[] getVrfHash() { return vrfHash; }
    }

    public static Validator electLeader(List<Validator> validators, long round, byte[] prevBlockHash) throws Exception {
        long totalStake = 0;
        for (Validator v : validators) totalStake += v.getStake();
        if (totalStake <= 0) return null;

        byte[] seed = computeSeed(round, prevBlockHash);

        List<Candidate> candidates = new ArrayList<>();
        for (Validator v : validators) {
            VRF.Result res = VRF.Prove(v.getPublicKey(), v.getPrivateKey(), seed);
            byte[] pi = res.pi;
            byte[] beta = res.hash;

            BigDecimal ratio = computeRatio(beta);
            BigDecimal threshold = new BigDecimal(v.getStake())
                    .divide(new BigDecimal(totalStake), MathContext.DECIMAL128)
                    .multiply(BigDecimal.valueOf(LAMBDA));
            if (threshold.compareTo(BigDecimal.ONE) > 0) threshold = BigDecimal.ONE;

            if (ratio.compareTo(threshold) < 0) {
                candidates.add(new Candidate(v, pi, beta, seed));
            }
        }

        return resolveCandidates(candidates);
    }

    private static byte[] computeSeed(long round, byte[] prevBlockHash) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(Long.toString(round).getBytes(StandardCharsets.UTF_8));
        md.update(prevBlockHash);
        return md.digest();
    }

    private static BigDecimal computeRatio(byte[] hash) {
        BigInteger hashInt = new BigInteger(1, hash);
        BigInteger max = BigInteger.ONE.shiftLeft(hash.length * 8);
        return new BigDecimal(hashInt).divide(new BigDecimal(max), MathContext.DECIMAL128);
    }

    private static Validator resolveCandidates(List<Candidate> candidates) throws Exception {
        List<Candidate> valid = new ArrayList<>();
        for (Candidate c : candidates) {
            if (VRF.Verify(c.validator.getPublicKey(), c.vrfProof, c.seed)) {
                valid.add(c);
            }
        }
        if (valid.isEmpty()) return null;
        if (valid.size() == 1) return valid.get(0).getValidator();

        valid.sort(Comparator
                .comparing((Candidate c) -> new BigInteger(1, c.getVrfHash()))
                .thenComparing(c -> c.getValidator().getId()));
        return valid.get(0).getValidator();
    }

    public static void main(String[] args) throws Exception {
        List<Validator> validators = new ArrayList<>();
        validators.add(new Validator("Node1", 100));
        validators.add(new Validator("Node2", 200));
        validators.add(new Validator("Node3", 300));

        byte[] prevBlockHash = "example_block_hash".getBytes(StandardCharsets.UTF_8);

        Validator leader = electLeader(validators, 1, prevBlockHash);
        System.out.println(leader != null ? ("Selected leader: " + leader.getId())
                : "No leader selected.");
    }
}
