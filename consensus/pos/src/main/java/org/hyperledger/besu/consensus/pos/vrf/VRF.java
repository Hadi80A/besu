package org.hyperledger.besu.consensus.pos.vrf;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.crypto.*;
import org.hyperledger.besu.cryptoservices.NodeKey;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * ===================== IMPORTANT =====================
 * This is a VRF-shaped construction using secp256k1 ECDSA via NodeKey.
 * It is publicly verifiable and compact, but it is NOT a standards-compliant EC-VRF
 * (because NodeKey does not expose "return x*P as an EC point" to build Γ = x·H(m)).
 *
 * Determinism/Uniqueness: If the security module uses deterministic ECDSA (RFC6979),
 * the signature (and thus y) is unique for (sk, seed). If it randomizes nonces,
 * multiple valid (π, y) values may exist for the same (sk, seed); verification and
 * leader election still work, but formal VRF uniqueness is weakened.
 * =====================================================
 */
public final class VRF {

    // ---- Suite/domain labels (ASCII, explicit UTF-8) ----
    public static final String SUITE_LABEL = "NEXUS-VRF-SECP256K1-ECDSA-KECCAK-V1";
    private static final byte[] SUITE_LABEL_BYTES = SUITE_LABEL.getBytes(StandardCharsets.UTF_8);
    private static final byte[] OUT_LABEL_BYTES   = "VRF-OUT".getBytes(StandardCharsets.UTF_8);

    // Besu signature algorithm handle (secp256k1)
    private static final SignatureAlgorithm SIG = SignatureAlgorithmFactory.getInstance();

    // secp256k1 curve order n (BigInteger)
    private static final BigInteger SECP256K1_N =
            new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16);

    // Proof format: r(32) || s(32) || v(1)
    public static final int PROOF_SIZE = 65;

    private VRF() {}

    /** Opaque proof: 65 bytes (r||s||v). */
    public static final class Proof {
        private final byte[] bytes;

        public Proof(final byte[] bytes) {
            if (bytes == null || bytes.length != PROOF_SIZE) {
                throw new IllegalArgumentException("invalid proof length (need 65 bytes r||s||v)");
            }
            this.bytes = Arrays.copyOf(bytes, PROOF_SIZE);
        }

        public byte[] bytes() { return Arrays.copyOf(bytes, PROOF_SIZE); }
    }

    /** Prover result: (π, y). */
    public static final class Result {
        private final Proof proof;
        private final Bytes32 output;

        public Result(final Proof proof, final Bytes32 output) {
            this.proof = proof;
            this.output = output;
        }
        public Proof proof()   { return proof; }
        public Bytes32 output(){ return output; }
    }

    /**
     * Compute the message hash to be signed by NodeKey:
     *   dataHash = keccak256( SUITE_LABEL || pkCompressed || seed )
     */
    private static Bytes32 messageHash(final SECPPublicKey pk, final Bytes32 seed) {
        final byte[] pkc = compressKey(pk.getEncodedBytes().toArray());
        return Hash.keccak256(Bytes.concatenate(
                Bytes.wrap(SUITE_LABEL_BYTES),
                Bytes.wrap(pkc),
                seed
        ));
    }

    /**
     * Compute the VRF output:
     *   y = keccak256( "VRF-OUT" || pkCompressed || seed || r || s )
     */
    private static Bytes32 outputHash(final SECPPublicKey pk, final Bytes32 seed, final SECPSignature sig) {
        final byte[] pkc = compressKey(pk.getEncodedBytes().toArray());
        final byte[] r = i2be(sig.getR(), 32);
        final byte[] s = i2be(sig.getS(), 32);
        return Hash.keccak256(Bytes.concatenate(
                Bytes.wrap(OUT_LABEL_BYTES),
                Bytes.wrap(pkc),
                seed,
                Bytes.wrap(r),
                Bytes.wrap(s)
        ));
    }

    /**
     * Prover (local node): use NodeKey to sign the domain-separated message and
     * emit (π, y). π carries (r,s,v) so remote verifiers can reconstruct the signature.
     */
    public static Result prove(final NodeKey nodeKey, final Bytes32 seed) {
        if (nodeKey == null) throw new IllegalArgumentException("nodeKey null");
        if (seed == null) throw new IllegalArgumentException("seed null");

        final SECPPublicKey pk = nodeKey.getPublicKey();
        final Bytes32 dataHash = messageHash(pk, seed);

        // Besu NodeKey.sign returns a normalized low-s signature and a recovery id (v/recId).
        final SECPSignature sig = nodeKey.sign(dataHash);

        final byte[] proofBytes = encodeProof(sig);
        final Bytes32 y = outputHash(pk, seed, sig);
        return new Result(new Proof(proofBytes), y);
    }

    /**
     * Verifier (remote): check π against (pk, seed).
     * If true, both sides derive the same y with {@link #hash(SECPPublicKey, Bytes32, Proof)}.
     */
    public static boolean verify(final SECPPublicKey pk, final Bytes32 seed, final Proof proof) {
        if (pk == null || seed == null || proof == null) return false;
        final SECPSignature sig = decodeProof(proof.bytes());
        final Bytes32 dataHash = messageHash(pk, seed);
        return SIG.verify(dataHash, sig,pk );
    }

    /** Deterministically derive y from a verified proof. */
    public static Bytes32 hash(final SECPPublicKey pk, final Bytes32 seed, final Proof proof) {
        final SECPSignature sig = decodeProof(proof.bytes());
        return outputHash(pk, seed, sig);
    }

    // ---------------- Encoding helpers ----------------

    private static byte[] encodeProof(final SECPSignature sig) {
        final byte[] out = new byte[PROOF_SIZE];
        final byte[] r = i2be(sig.getR(), 32);
        final byte[] s = i2be(sig.getS(), 32);

        // Use reflection to get recovery id if present (avoid compile-time dependency on method presence).
        byte recId = 0;
        try {
            // Try getRecId()
            final Method m = SECPSignature.class.getMethod("getRecId");
            final Object vObj = m.invoke(sig);
            if (vObj instanceof Number) {
                recId = ((Number) vObj).byteValue();
            }
        } catch (NoSuchMethodException nsme) {
            // If method doesn't exist, try fallback "getV" if some builds expose it.
            try {
                final Method m2 = SECPSignature.class.getMethod("getV");
                final Object vObj = m2.invoke(sig);
                if (vObj instanceof Number) {
                    byte vv = ((Number) vObj).byteValue();
                    if (vv == 27 || vv == 28) recId = (byte) (vv - 27);
                    else recId = vv;
                }
            } catch (ReflectiveOperationException ignore) {
                // no method available -> fall through to default recId=0
            } catch (Throwable t) {
                // any other issue -> default
            }
        } catch (ReflectiveOperationException roe) {
            // invocation failed -> default
        } catch (Throwable t) {
            // other unexpected error -> default
        }

        System.arraycopy(r, 0, out, 0, 32);
        System.arraycopy(s, 0, out, 32, 32);
        out[64] = recId;
        return out;
    }

    private static SECPSignature decodeProof(final byte[] proof65) {
        if (proof65 == null || proof65.length != PROOF_SIZE) {
            throw new IllegalArgumentException("bad proof length; need 65 bytes r||s||v");
        }
        final byte[] r = Arrays.copyOfRange(proof65, 0, 32);
        final byte[] s = Arrays.copyOfRange(proof65, 32, 64);
        final byte v   = proof65[64];

        // Fix for your error: supply recId and the curve order.
        // recId is not used for verification-with-known-pk, but the factory demands it.
        final byte recId = normalizeRecId(v);
        return SECPSignature.create(new BigInteger(1, r), new BigInteger(1, s), recId, SECP256K1_N);
    }

    private static byte normalizeRecId(final byte v) {
        if (v == 27 || v == 28) return (byte) (v - 27);
        if (v == 0 || v == 1) return v;
        // Unknown encodings: clamp to 0 (safe; verify() does not use recId when pk is provided).
        return 0;
    }

    private static byte[] i2be(final BigInteger v, final int len) {
        final byte[] be = v.toByteArray();
        if (be.length == len) return be;
        if (be.length == len + 1 && be[0] == 0x00) {
            return Arrays.copyOfRange(be, 1, be.length);
        }
        if (be.length > len) throw new IllegalArgumentException("integer too large");
        final byte[] out = new byte[len];
        System.arraycopy(be, 0, out, len - be.length, be.length);
        return out;
    }

    /**
     * Compress a secp256k1 pubkey to 33 bytes.
     * Accepts:
     *  - 33-byte compressed (0x02/0x03 || X)
     *  - 65-byte uncompressed (0x04 || X(32) || Y(32))
     *  - 64-byte raw XY        (      X(32) || Y(32))
     */
    private static byte[] compressKey(final byte[] enc) {
        if (enc == null) throw new IllegalArgumentException("pk bytes null");

        if (enc.length == 33 && (enc[0] == 0x02 || enc[0] == 0x03)) {
            return Arrays.copyOf(enc, 33);
        }

        byte[] x, y;
        if (enc.length == 65 && enc[0] == 0x04) {
            x = Arrays.copyOfRange(enc, 1, 33);
            y = Arrays.copyOfRange(enc, 33, 65);
        } else if (enc.length == 64) {
            x = Arrays.copyOfRange(enc, 0, 32);
            y = Arrays.copyOfRange(enc, 32, 64);
        } else {
            throw new IllegalArgumentException("unsupported pubkey encoding; expected 33/64/65 bytes");
        }

        final boolean yIsOdd = (y[31] & 1) == 1;
        final byte[] out = new byte[33];
        out[0] = (byte) (yIsOdd ? 0x03 : 0x02);
        System.arraycopy(x, 0, out, 1, 32);
        return out;
    }
}
