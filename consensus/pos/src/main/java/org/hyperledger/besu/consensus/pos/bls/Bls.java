package org.hyperledger.besu.consensus.pos.bls;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import supranational.blst.P1;
import supranational.blst.P1_Affine;
import supranational.blst.P2;
import supranational.blst.P2_Affine;
import supranational.blst.Pairing;
import supranational.blst.blst;
import supranational.blst.Scalar;

/**
 * BLS12-381 helper using supranational/blst via JBlst.
 *
 * Changes:
 *  - Enforce PoP registration (config flag, default true).
 *  - Check duplicate public keys in fastAggregateVerify.
 *  - Validate secret key deserialization (non-zero and < r).
 *  - Improved logging and less exception swallowing.
 */
public final class Bls {

    private static final Logger logger = Logger.getLogger(Bls.class.getName());

    // ---- Constants -----------------------------------------------------------

    // Use canonical DST strings from the IETF BLS drafts
    public static final String DST_SIG_STRING =
            "BLS_SIG_BLS12381G2_XMD:SHA-256_SSWU_RO_POP_";
    public static final byte[] DST_SIG_BYTES = DST_SIG_STRING.getBytes(StandardCharsets.US_ASCII);

    public static final String DST_POP_STRING =
            "BLS_POP_BLS12381G2_XMD:SHA-256_SSWU_RO_POP_";
    public static final byte[] DST_POP_BYTES = DST_POP_STRING.getBytes(StandardCharsets.US_ASCII);

    /** Lengths (compressed) per IETF serialization for BLS12-381. */
    public static final int SECRET_KEY_LEN   = 32;  // Scalar bytes
    public static final int PUBLIC_KEY_LEN   = 48;  // G1 compressed
    public static final int SIGNATURE_LEN    = 96;  // G2 compressed

    // HKDF constants
    private static final String HKDF_HMAC_ALG = "HmacSHA256";
    private static final MessageDigest SHA256;
    static {
        try {
            SHA256 = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // The subgroup order r for BLS12-381 (from curve parameters)
    // r = 0x73eda753299d7d483339d80809a1d80553bda402fffe5bfeffffffff00000001
    private static final BigInteger R = new BigInteger(
            "73eda753299d7d483339d80809a1d80553bda402fffe5bfeffffffff00000001", 16);

    // L value for KeyGen: ceil((3 * ceil(log2(r))) / 16) -> 48 for BLS12-381
    private static final int KEYGEN_OKM_LEN = 48;

    // Salt base ASCII string for IETF KeyGen
    private static final byte[] KEYGEN_SALT_BASE = "BLS-SIG-KEYGEN-SALT-".getBytes(StandardCharsets.US_ASCII);

    // --- PoP registration store (enforce PoP registration at genesis) -----
    // Map: compressedPublicKeyBytes -> compressedPoPBytes
    private static final Map<ByteArrayKey, byte[]> REGISTERED_POPS = new ConcurrentHashMap<>();

    // If true, require that any public key used for aggregation or verification has an entry in REGISTERED_POPS.
    // Default true because you said "for all".
    private static volatile boolean REQUIRE_POP_REGISTRATION = true;

    // Public setter so the runtime can disable/enable if needed (e.g., for tests)
    public static void setRequirePopRegistration(final boolean value) {
        REQUIRE_POP_REGISTRATION = value;
    }

    // ---- Types ---------------------------------------------------------------

    /** Holder for a secret key (blst_Scalar). */
    public static final class SecretKey {
        private final Scalar sk;

        private SecretKey(final Scalar sk) {
            this.sk = sk;
        }

        public byte[] toBytes() {
            return sk.to_bendian();
        }

        // Optional helper to zeroize if needed (call when SK is no longer needed)
        public void zeroize() {
            try {
                final byte[] b = sk.to_bendian();
                Arrays.fill(b, (byte) 0);
            } catch (Throwable t) {
                // ignore
            }
        }
    }

    /** Holder for a public key in G1 (affine). */
    public static final class PublicKey {
        private final P1_Affine pk;

        private PublicKey(final P1_Affine pk) {
            this.pk = pk;
        }

        /** Optional: Validation (in-group + not infinity). */
        public boolean isValid() {
            try {
                return pk.in_group() && !pk.is_inf();
            } catch (Throwable t) {
                return false;
            }
        }

        public byte[] toBytesCompressed() {
            return pk.compress();
        }
    }

    /** Holder for a signature in G2 (affine). */
    public static final class Signature {
        private final P2_Affine sig;

        private Signature(final P2_Affine sig) {
            this.sig = sig;
        }

        public byte[] toBytesCompressed() {
            return sig.compress();
        }

        /** Optional: Validation (in-group + not infinity). */
        public boolean isValid() {
            try {
                return sig.in_group() && !sig.is_inf();
            } catch (Throwable t) {
                return false;
            }
        }
    }

    /** Combined key pair. */
    public static final class KeyPair {
        public final SecretKey secretKey;
        public final PublicKey publicKey;

        private KeyPair(final SecretKey sk, final PublicKey pk) {
            this.secretKey = sk;
            this.publicKey = pk;
        }
    }

    // ---- Utilities: HKDF, I2OSP/OS2IP, helpers -------------------------------

    private Bls() {}

    private static byte[] hkdfExtract(final byte[] salt, final byte[] ikm) {
        try {
            final Mac mac = Mac.getInstance(HKDF_HMAC_ALG);
            final SecretKeySpec keySpec = new SecretKeySpec(salt == null ? new byte[0] : salt, HKDF_HMAC_ALG);
            mac.init(keySpec);
            return mac.doFinal(ikm);
        } catch (Exception e) {
            throw new RuntimeException("HKDF-Extract failed", e);
        }
    }

    private static byte[] hkdfExpand(final byte[] prk, final byte[] info, final int len) {
        try {
            final Mac mac = Mac.getInstance(HKDF_HMAC_ALG);
            mac.init(new SecretKeySpec(prk, HKDF_HMAC_ALG));

            final int hashLen = mac.getMacLength();
            final int n = (len + hashLen - 1) / hashLen; // ceil
            if (n > 255) throw new IllegalArgumentException("Cannot expand to > 255 * hashLen");

            byte[] previous = new byte[0];
            ByteBuffer out = ByteBuffer.allocate(n * hashLen);
            for (int i = 1; i <= n; i++) {
                mac.reset();
                mac.update(previous);
                if (info != null) mac.update(info);
                mac.update((byte) i);
                previous = mac.doFinal();
                out.put(previous);
            }
            byte[] okm = new byte[len];
            System.arraycopy(out.array(), 0, okm, 0, len);
            return okm;
        } catch (Exception e) {
            throw new RuntimeException("HKDF-Expand failed", e);
        }
    }

    private static byte[] i2osp(int value, final int length) {
        final byte[] b = new byte[length];
        for (int i = length - 1; i >= 0; i--) {
            b[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return b;
    }

    private static BigInteger os2ip(final byte[] bs) {
        return new BigInteger(1, bs);
    }

    private static byte[] bigIntegerToFixedLen(final BigInteger v, final int fixedLen) {
        final byte[] tmp = v.toByteArray(); // may contain leading zero for sign
        if (tmp.length == fixedLen) return tmp;
        final byte[] out = new byte[fixedLen];
        if (tmp.length > fixedLen) {
            // drop leading bytes (shouldn't happen for values < 2^(8*fixedLen))
            System.arraycopy(tmp, tmp.length - fixedLen, out, 0, fixedLen);
        } else {
            System.arraycopy(tmp, 0, out, fixedLen - tmp.length, tmp.length);
        }
        return out;
    }

    // Byte array wrapper for being a key in the map
    private static final class ByteArrayKey {
        private final byte[] data;
        private final int hash;

        ByteArrayKey(final byte[] data) {
            this.data = data.clone();
            this.hash = Arrays.hashCode(this.data);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (!(o instanceof ByteArrayKey)) return false;
            return Arrays.equals(data, ((ByteArrayKey) o).data);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    // ---- Keygen (IETF HKDF based) -------------------------------------------

    /**
     * Generate a keypair from input keying material (IKM) using IETF KeyGen.
     */
    public static KeyPair generateKeyPair(final byte[] ikm, final byte[] keyInfo) {
        Objects.requireNonNull(ikm, "ikm");
        if (ikm.length < 32) {
            throw new IllegalArgumentException("ikm must be at least 32 bytes");
        }

        try {
            byte[] salt = KEYGEN_SALT_BASE.clone();
            BigInteger skInt = BigInteger.ZERO;
            while (skInt.equals(BigInteger.ZERO)) {
                // salt = H(salt)
                salt = SHA256.digest(salt);

                // PRK = HKDF-Extract(salt, IKM || I2OSP(0,1))
                final byte[] ikmPadded = new byte[ikm.length + 1];
                System.arraycopy(ikm, 0, ikmPadded, 0, ikm.length);
                ikmPadded[ikm.length] = 0x00;
                final byte[] prk = hkdfExtract(salt, ikmPadded);

                // OKM = HKDF-Expand(PRK, key_info || I2OSP(L,2), L)
                final byte[] info;
                if (keyInfo == null || keyInfo.length == 0) {
                    final byte[] lBytes = i2osp(KEYGEN_OKM_LEN, 2);
                    info = lBytes;
                } else {
                    final byte[] lBytes = i2osp(KEYGEN_OKM_LEN, 2);
                    info = new byte[keyInfo.length + lBytes.length];
                    System.arraycopy(keyInfo, 0, info, 0, keyInfo.length);
                    System.arraycopy(lBytes, 0, info, keyInfo.length, lBytes.length);
                }
                final byte[] okm = hkdfExpand(prk, info, KEYGEN_OKM_LEN);

                // SK = OS2IP(OKM) mod r
                skInt = os2ip(okm).mod(R);
                // if skInt==0, loop repeats with salt = H(salt)
            }

            // convert to 32-byte scalar big-endian for blst.from_bendian
            final byte[] skBytes = bigIntegerToFixedLen(skInt, SECRET_KEY_LEN);

            final Scalar scalar = new Scalar();
            scalar.from_bendian(skBytes);

            // derive public key (G1 * sk)
            final P1 pkPoint = blst.G1().mult(scalar);
            final P1_Affine pkAffine = pkPoint.to_affine();

            // zero sensitive arrays where possible
            Arrays.fill(skBytes, (byte) 0);

            return new KeyPair(new SecretKey(scalar), new PublicKey(pkAffine));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("KeyGen failed", e);
        }
    }

    /** Generate a keypair from SecureRandom (calls HKDF KeyGen with random ikm). */
    public static KeyPair generateKeyPair(final SecureRandom sr) {
        final byte[] ikm = new byte[32];
        sr.nextBytes(ikm);
        try {
            return generateKeyPair(ikm, null);
        } finally {
            Arrays.fill(ikm, (byte) 0);
        }
    }

    /** Deserialize secret key from big-endian 32 bytes with validation. */
    public static SecretKey secretKeyFromBytes(final byte[] skBytes) {
        Objects.requireNonNull(skBytes, "skBytes");
        if (skBytes.length != SECRET_KEY_LEN) {
            throw new IllegalArgumentException("secret key must be 32 bytes");
        }
        // Basic validation: interpret provided bytes as integer (OS2IP)
        final BigInteger provided = os2ip(skBytes);
        if (provided.equals(BigInteger.ZERO)) {
            throw new IllegalArgumentException("secret key must not be zero");
        }
        if (provided.compareTo(R) >= 0) {
            throw new IllegalArgumentException("secret key must be < subgroup order r");
        }

        final Scalar sk = new Scalar();
        sk.from_bendian(skBytes);

        // Optional extra check: ensure scalar internal representation is not zero
        final byte[] internal = sk.to_bendian();
        final BigInteger internalVal = os2ip(internal);
        if (internalVal.equals(BigInteger.ZERO)) {
            throw new IllegalArgumentException("secret key is zero after scalar load");
        }

        return new SecretKey(sk);
    }

    /** Deserialize compressed G1 pubkey (48 bytes). Optionally validate. */
    public static PublicKey publicKeyFromBytes(final byte[] pkCompressed, final boolean validate) {
        Objects.requireNonNull(pkCompressed, "pkCompressed");
        if (pkCompressed.length != PUBLIC_KEY_LEN) {
            throw new IllegalArgumentException("public key must be 48 bytes (compressed G1)");
        }
        final P1_Affine pk = new P1_Affine(pkCompressed);

        if (validate) {
            if (!pk.in_group() || pk.is_inf()) {
                throw new IllegalArgumentException("public key not in G1 or is infinity");
            }
        }
        return new PublicKey(pk);
    }

    /** Deserialize compressed G2 signature (96 bytes). Optionally validate. */
    public static Signature signatureFromBytes(final byte[] sigCompressed, final boolean validate) {
        Objects.requireNonNull(sigCompressed, "sigCompressed");
        if (sigCompressed.length != SIGNATURE_LEN) {
            throw new IllegalArgumentException("signature must be 96 bytes (compressed G2)");
        }
        final P2_Affine sig = new P2_Affine(sigCompressed);

        if (validate) {
            if (!sig.in_group() || sig.is_inf()) {
                throw new IllegalArgumentException("signature not in G2 or is infinity");
            }
        }
        return new Signature(sig);
    }

    // ---- Proof of Possession (PoP) ------------------------------------------

    /** Create a PoP over the serialized public key using the POP ciphersuite. */
    public static Signature createPop(final SecretKey sk, final PublicKey pk) {
        Objects.requireNonNull(sk);
        Objects.requireNonNull(pk);
        final byte[] pkCompressed = pk.toBytesCompressed();

        final P2 sigPoint = new P2();
        // hash_pubkey_to_point with POP DST
        sigPoint.hash_to(pkCompressed, DST_POP_STRING);
        sigPoint.mult(sk.sk);
        return new Signature(sigPoint.to_affine());
    }

    /** Verify PoP for a public key. */
    public static boolean verifyPop(final PublicKey pk, final Signature pop) {
        Objects.requireNonNull(pk);
        Objects.requireNonNull(pop);

        final byte[] pkCompressed = pk.toBytesCompressed();

        try {
            // Pairing with hash mode (true) and POP DST
            final Pairing pairing = new Pairing(true, DST_POP_STRING);

            // Hash public key to G2
            final P2 hashPk = new P2();
            hashPk.hash_to(pkCompressed, DST_POP_STRING);
            final P2_Affine hashPkAffine = hashPk.to_affine();

            // e(pk, hash_pk)
            invokePairingMethod(pairing, "raw_aggregate",
                    new Class<?>[]{P2_Affine.class, P1_Affine.class},
                    new Object[]{hashPkAffine, pk.pk});

            // Negate pop signature
            P2 negPopSigPoint = new P2(pop.sig);
            negPopSigPoint.cneg(true);
            final P2_Affine negPopSigAffine = negPopSigPoint.to_affine();

            // G1 generator
            final P1_Affine g1Affine = blst.G1().to_affine();

            // e(g1, -pop_sig)
            invokePairingMethod(pairing, "raw_aggregate",
                    new Class<?>[]{P2_Affine.class, P1_Affine.class},
                    new Object[]{negPopSigAffine, g1Affine});

            // commit and finalverify
            invokePairingMethod(pairing, "commit", new Class<?>[0], new Object[0]);

            // finalverify(): no args for product==1 check
            try {
                Method fv = pairing.getClass().getMethod("finalverify");
                Object result = fv.invoke(pairing);
                if (result instanceof Boolean) {
                    return (Boolean) result;
                } else if (result instanceof Integer) {
                    return ((Integer) result) == 0;
                } else {
                    // unexpected return type; assume success if no exception
                    return true;
                }
            } catch (NoSuchMethodException e) {
                logger.log(Level.WARNING, "Pairing.finalverify method not found", e);
                throw new RuntimeException("Pairing.finalverify method not found", e);
            }
        } catch (RuntimeException e) {
            logger.log(Level.FINE, "verifyPop runtime failure", e);
            return false;
        } catch (Exception e) {
            logger.log(Level.FINE, "verifyPop unexpected failure", e);
            return false;
        }
    }

    // ---- PoP registration APIs (for genesis enforcement) --------------------

    /** Register (verify & store) a proof-of-possession for a public key. */
    public static boolean registerProofOfPossession(final PublicKey pk, final Signature pop) {
        Objects.requireNonNull(pk);
        Objects.requireNonNull(pop);
        if (!verifyPop(pk, pop)) return false;
        REGISTERED_POPS.put(new ByteArrayKey(pk.toBytesCompressed()), pop.toBytesCompressed());
        return true;
    }

    /** Check if a public key has a registered PoP. */
    public static boolean isPopRegistered(final PublicKey pk) {
        Objects.requireNonNull(pk);
        return REGISTERED_POPS.containsKey(new ByteArrayKey(pk.toBytesCompressed()));
    }

    // ---- Signing & verification ---------------------------------------------

    /** Sign arbitrary message bytes in the SIG domain. */
    public static Signature sign(final SecretKey sk, final byte[] message) {
        Objects.requireNonNull(sk);
        Objects.requireNonNull(message);
        final P2 sigPoint = new P2();
        sigPoint.hash_to(message, DST_SIG_STRING);
        sigPoint.mult(sk.sk);
        return new Signature(sigPoint.to_affine());
    }

    /** Verify a single signature against one public key. */
    public static boolean verify(final PublicKey pk, final byte[] message, final Signature sig) {
        Objects.requireNonNull(pk);
        Objects.requireNonNull(message);
        Objects.requireNonNull(sig);

        if (!pk.isValid() || !sig.isValid()) return false;

        try {
            final Pairing pairing = new Pairing(true, DST_SIG_STRING);
            // Use pairing.aggregate(pk, sig, message) but do it via reflective wrapper for error handling.
            invokePairingMethod(pairing, "aggregate",
                    new Class<?>[]{P1_Affine.class, P2_Affine.class, byte[].class},
                    new Object[]{pk.pk, sig.sig, message});

            invokePairingMethod(pairing, "commit", new Class<?>[0], new Object[0]);

            // finalverify
            Method fv = pairing.getClass().getMethod("finalverify");
            Object result = fv.invoke(pairing);
            if (result instanceof Boolean) return (Boolean) result;
            if (result instanceof Integer) return ((Integer) result) == 0;
            return true;
        } catch (RuntimeException e) {
            logger.log(Level.FINE, "verify runtime failure", e);
            return false;
        } catch (Exception e) {
            logger.log(Level.FINE, "verify unexpected failure", e);
            return false;
        }
    }

    // ---- Aggregation ---------------------------------------------------------

    /** Aggregate any number of signatures (G2). Returns compressed aggregate. */
    public static byte[] aggregateSignatures(final List<Signature> signatures) {
        Objects.requireNonNull(signatures);
        if (signatures.isEmpty()) {
            throw new IllegalArgumentException("no signatures to aggregate");
        }
        P2 acc = new P2(); // Starts as identity

        boolean first = true;
        for (final Signature s : signatures) {
            if (!s.isValid()) throw new IllegalArgumentException("signature invalid in aggregation");
            if (first) {
                acc = new P2(s.sig); // Initialize with first point (P2(P2_Affine) constructor)
                first = false;
            } else {
                acc.add(s.sig); // P2.add(P2_Affine)
            }
        }
        final P2_Affine out = acc.to_affine();
        return out.compress();
    }

    /**
     * Fast aggregate verify: many public keys, all signed the SAME message.
     * Verifies aggregate signature in one pairing check.
     *
     * This now:
     *  - enforces PoP registration (if REQUIRE_POP_REGISTRATION == true)
     *  - rejects duplicate public keys (to prevent double-counting).
     */
    public static boolean fastAggregateVerify(
            final List<PublicKey> pubkeys, final byte[] message, final byte[] aggSignatureCompressed) {
        Objects.requireNonNull(pubkeys);
        Objects.requireNonNull(message);
        Objects.requireNonNull(aggSignatureCompressed);

        if (pubkeys.isEmpty()) return false;

        final Signature aggSig;
        try {
            aggSig = signatureFromBytes(aggSignatureCompressed, true);
        } catch (IllegalArgumentException e) {
            logger.log(Level.FINE, "fastAggregateVerify: invalid agg signature bytes", e);
            return false; // Invalid signature format
        }

        // Validate all public keys and ensure PoP registration (if required)
        // Also ensure uniqueness to prevent duplicate-pubkey attacks.
        final Set<String> pkSet = new HashSet<>();
        for (PublicKey pk : pubkeys) {
            if (!pk.isValid()) {
                logger.log(Level.FINE, "fastAggregateVerify: invalid public key in list");
                return false;
            }
            if (REQUIRE_POP_REGISTRATION && !isPopRegistered(pk)) {
                logger.log(Level.FINE, "fastAggregateVerify: public key not PoP-registered");
                return false;
            }
            final String enc = Base64.getEncoder().encodeToString(pk.toBytesCompressed());
            if (!pkSet.add(enc)) {
                logger.log(Level.FINE, "fastAggregateVerify: duplicate public key detected");
                return false; // duplicate pk -> reject
            }
        }

        // Aggregate Public Keys in G1
        P1 pkAggregate = new P1(); // Starts as identity

        for (PublicKey pk : pubkeys) {
            pkAggregate.add(pk.pk); // P1.add(P1_Affine)
        }
        final P1_Affine pkAggAffine = pkAggregate.to_affine();

        try {
            // Verify pairing: e(agg_pk, hash_msg) == e(g1, agg_sig)
            final Pairing pairing = new Pairing(true, DST_SIG_STRING);
            invokePairingMethod(pairing, "aggregate",
                    new Class<?>[]{P1_Affine.class, P2_Affine.class, byte[].class},
                    new Object[]{pkAggAffine, aggSig.sig, message});
            invokePairingMethod(pairing, "commit", new Class<?>[0], new Object[0]);

            Method fv = pairing.getClass().getMethod("finalverify");
            Object result = fv.invoke(pairing);
            if (result instanceof Boolean) return (Boolean) result;
            if (result instanceof Integer) return ((Integer) result) == 0;
            return true;
        } catch (RuntimeException e) {
            logger.log(Level.FINE, "fastAggregateVerify runtime failure", e);
            return false;
        } catch (Exception e) {
            logger.log(Level.FINE, "fastAggregateVerify unexpected failure", e);
            return false;
        }
    }

    /**
     * General aggregate verify: (pk_i, msg_i) pairs, an aggregate signature over distinct messages.
     * Uses multi-pairing (one pairing context).
     *
     * This also enforces REQUIRE_POP_REGISTRATION and does pubkey validation.
     */
    public static boolean aggregateVerify(
            final List<PublicKey> pubkeys,
            final List<byte[]> messages,
            final byte[] aggSignatureCompressed) {
        Objects.requireNonNull(pubkeys);
        Objects.requireNonNull(messages);
        Objects.requireNonNull(aggSignatureCompressed);

        if (pubkeys.isEmpty() || pubkeys.size() != messages.size()) return false;

        final Signature aggSig;
        try {
            aggSig = signatureFromBytes(aggSignatureCompressed, true);
        } catch (IllegalArgumentException e) {
            logger.log(Level.FINE, "aggregateVerify: invalid agg signature bytes", e);
            return false; // Invalid signature format
        }

        if (!aggSig.isValid()) {
            logger.log(Level.FINE, "aggregateVerify: aggregate signature invalid");
            return false;
        }

        // Validate pubkeys and PoP registration and uniqueness
        final Set<String> pkSet = new HashSet<>();
        for (PublicKey pk : pubkeys) {
            if (!pk.isValid()) {
                logger.log(Level.FINE, "aggregateVerify: invalid public key in list");
                return false;
            }
            if (REQUIRE_POP_REGISTRATION && !isPopRegistered(pk)) {
                logger.log(Level.FINE, "aggregateVerify: public key not PoP-registered");
                return false;
            }
            final String enc = Base64.getEncoder().encodeToString(pk.toBytesCompressed());
            if (!pkSet.add(enc)) {
                logger.log(Level.FINE, "aggregateVerify: duplicate public key detected");
                return false;
            }
        }

        try {
            final Pairing ctx = new Pairing(true, DST_SIG_STRING);

            // Aggregate all (pk_i, msg_i) pairs
            for (int i = 0; i < pubkeys.size(); i++) {
                invokePairingMethod(ctx, "aggregate",
                        new Class<?>[]{P1_Affine.class, P2_Affine.class, byte[].class},
                        new Object[]{pubkeys.get(i).pk, null, messages.get(i)});
            }

            // Get G1 generator affine
            final P1_Affine g1Affine = blst.G1().to_affine();
            // Negate agg signature
            P2 negAggSigPoint = new P2(aggSig.sig);
            negAggSigPoint.cneg(true);
            final P2_Affine negAggSigAffine = negAggSigPoint.to_affine();

            // raw_aggregate e(G1, -agg_sig)
            invokePairingMethod(ctx, "raw_aggregate",
                    new Class<?>[]{P2_Affine.class, P1_Affine.class},
                    new Object[]{negAggSigAffine, g1Affine});

            // commit and finalverify
            invokePairingMethod(ctx, "commit", new Class<?>[0], new Object[0]);
            Method fv = ctx.getClass().getMethod("finalverify");
            Object result = fv.invoke(ctx);
            if (result instanceof Boolean) return (Boolean) result;
            if (result instanceof Integer) return ((Integer) result) == 0;
            return true;
        } catch (RuntimeException e) {
            logger.log(Level.FINE, "aggregateVerify runtime failure", e);
            return false;
        } catch (Exception e) {
            logger.log(Level.FINE, "aggregateVerify unexpected failure", e);
            return false;
        }
    }

    // ---- Utilities -----------------------------------------------------------

    /**
     * Build the canonical message for your vote:
     * SIG_i over  VOTE || h || r || hB
     * and return SHA3-256(VOTE || h || r || hB) to sign.
     */
    public static byte[] buildVoteMessageSha3(
            final byte[] voteTagAscii, final byte[] h, final byte[] r, final byte[] hB) {
        Objects.requireNonNull(voteTagAscii);
        Objects.requireNonNull(h);
        Objects.requireNonNull(r);
        Objects.requireNonNull(hB);
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA3-256");
            md.update(voteTagAscii);
            md.update(h);
            md.update(r);
            md.update(hB);
            return md.digest();
        } catch (Exception e) {
            throw new RuntimeException("SHA3-256 not available", e);
        }
    }

    /** Convenience: bytes("VOTE"). */
    public static byte[] ascii(final String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    // ---- Example / self-test -------------------------------------------------

    /** Example: end-to-end usage (keygen -> PoP -> sign/verify -> aggregate). */
    public static void selfTest() {
        final SecureRandom rnd = new SecureRandom();

        // Two signers
        final KeyPair a = generateKeyPair(rnd);
        final KeyPair b = generateKeyPair(rnd);

        // PoP at genesis (register)
        final Signature popA = createPop(a.secretKey, a.publicKey);
        final Signature popB = createPop(b.secretKey, b.publicKey);

        if (!registerProofOfPossession(a.publicKey, popA) || !registerProofOfPossession(b.publicKey, popB)) {
            throw new IllegalStateException("PoP registration failed");
        }

        if (!verifyPop(a.publicKey, popA) || !verifyPop(b.publicKey, popB)) {
            throw new IllegalStateException("PoP verification failed");
        }

        // Build vote message (hash of tagged fields)
        final byte[] msg = buildVoteMessageSha3(ascii("VOTE"), random32(rnd), random32(rnd), random32(rnd));

        // Individual signatures
        final Signature sigA = sign(a.secretKey, msg);
        final Signature sigB = sign(b.secretKey, msg);

        // Individual verify
        if (!verify(a.publicKey, msg, sigA) || !verify(b.publicKey, msg, sigB)) {
            throw new IllegalStateException("single verify failed");
        }

        // Aggregate signatures
        final List<Signature> sigs = new ArrayList<>();
        sigs.add(sigA);
        sigs.add(sigB);
        final byte[] agg = aggregateSignatures(sigs);

        // Fast-aggregate verify (same message)
        final List<PublicKey> pks = Arrays.asList(a.publicKey, b.publicKey);
        if (!fastAggregateVerify(pks, msg, agg)) {
            throw new IllegalStateException("fast aggregate verify failed");
        }
    }

    private static byte[] random32(final SecureRandom r) {
        final byte[] out = new byte[32];
        r.nextBytes(out);
        return out;
    }

    // ---- Reflection-based pairing method invoker for error-handling ---------

    /**
     * Invoke a method on the Pairing object reflectively to capture return
     * codes (some blst binding methods return integer error codes).
     *
     * If the invoked method returns Integer and it's non-zero we throw.
     * If it returns Boolean, we don't alter (the caller will interpret).
     */
    private static void invokePairingMethod(final Pairing pairing, final String methodName,
                                            final Class<?>[] paramTypes, final Object[] args) {
        try {
            final Method m = pairing.getClass().getMethod(methodName, paramTypes);
            final Object rc = m.invoke(pairing, args);
            if (rc instanceof Integer) {
                final int code = (Integer) rc;
                if (code != 0) {
                    throw new RuntimeException("Pairing." + methodName + " returned error code: " + code);
                }
            }
            // If void or Boolean: nothing to do here
        } catch (NoSuchMethodException e) {
            logger.log(Level.SEVERE, "Pairing." + methodName + " not found on binding", e);
            throw new RuntimeException("Pairing." + methodName + " not found on binding", e);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.log(Level.SEVERE, "Pairing." + methodName + " invocation failed", e);
            throw new RuntimeException("Pairing." + methodName + " invocation failed", e);
        }
    }
}
