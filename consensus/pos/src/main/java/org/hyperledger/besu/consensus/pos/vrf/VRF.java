package org.hyperledger.besu.consensus.pos.vrf;

import net.i2p.crypto.eddsa.math.Curve;
import net.i2p.crypto.eddsa.math.Field;
import net.i2p.crypto.eddsa.math.GroupElement;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveSpec;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class VRF {

    private static final byte SUITE = 0x03;
    private static final int cLen = 16;
    private static final int qLen = 32;
    private static final int ptLen = 32;
    private static final int LIMIT = 256;
    private static final String QS =
            "1000000000000000000000000000000014def9dea2f79cd65812631a5cf5d3ed";

    private static final EdDSANamedCurveSpec spec =
            EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519);
    private static final Curve curve = spec.getCurve();
    private static final Field field = curve.getField();
    private static final GroupElement B = spec.getB();
    private static final BigInteger q = new BigInteger(QS, 16);

    public static final int PublicKeySize = 32;
    public static final int PrivateKeySize = 32; // ✅ فقط seed 32 بایتی
    public static final int ProofSize = ptLen + cLen + qLen; // 80

    // Add near top of class:
    private static final byte[] IDENTITY_ENC = B.scalarMultiply(new byte[32]).toByteArray();

    private static boolean isIdentity(GroupElement P) {
        return Arrays.equals(P.toByteArray(), IDENTITY_ENC);
    }

    // Optional (defense-in-depth): reject points that collapse under cofactor
    private static boolean isSmallSubgroup(GroupElement P) {
        return isIdentity(cofactorMultiply(P));
    }


    private static MessageDigest sha512() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-512");
    }

    private static byte[] intToLE(BigInteger v, int len) {
        if (v.signum() < 0) v = v.add(q);
        byte[] be = v.toByteArray();
        if (be.length > len && be[0] == 0) be = Arrays.copyOfRange(be, 1, be.length);
        if (be.length > len) throw new IllegalArgumentException("Integer too large");
        byte[] out = new byte[len];
        for (int i = 0; i < be.length; i++) out[i] = be[be.length - 1 - i];
        return out;
    }

    private static BigInteger leToInt(byte[] le) {
        byte[] be = new byte[le.length];
        for (int i = 0; i < le.length; i++) be[i] = le[le.length - 1 - i];
        return new BigInteger(1, be);
    }

    private static byte[] point_to_string(GroupElement P) {
        return P.toByteArray();
    }

    private static GroupElement string_to_point(byte[] enc) {
        if (enc.length != ptLen) return null;
        try {
            return curve.createPoint(enc, true);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static GroupElement cofactorMultiply(GroupElement P) {
        GroupElement R = P;
        R = R.dbl().toP3(); // *2
        R = R.dbl().toP3(); // *4
        R = R.dbl().toP3(); // *8
        return R;
    }

    private static byte[] expandSecretScalar(byte[] seed32) throws NoSuchAlgorithmException {
        MessageDigest d = sha512();
        byte[] seed = Arrays.copyOf(seed32, 32);
        byte[] h = d.digest(seed);
        h[0] &= (byte) 248;
        h[31] &= (byte) 127;
        h[31] |= (byte) 64;
        return Arrays.copyOf(h, 32);
    }

    private static BigInteger nonceRFC8032(byte[] seed32, byte[] h_string) throws NoSuchAlgorithmException {
        MessageDigest d = sha512();
        byte[] hashed = d.digest(Arrays.copyOf(seed32, 32));
        byte[] prefix = Arrays.copyOfRange(hashed, 32, 64);
        d.reset();
        d.update(prefix);
        d.update(h_string);
        byte[] k_string = d.digest();
        BigInteger k = leToInt(k_string);
        return k.mod(q);
    }

    private static GroupElement encodeToCurveTAI(byte[] encode_to_curve_salt, byte[] alpha) throws Exception {
        for (int ctr = 0; ctr < LIMIT; ctr++) {
            MessageDigest h = sha512();
            h.update(SUITE);
            h.update((byte) 0x01);
            h.update(encode_to_curve_salt);
            h.update(alpha);
            h.update((byte) (ctr & 0xFF));
            h.update((byte) 0x00);
            byte[] digest = h.digest();
            byte[] attempt = Arrays.copyOfRange(digest, 0, 32);
            GroupElement P = string_to_point(attempt);
            if (P != null) return P;
        }
        throw new Exception("ECVRF: encode_to_curve failed");
    }

    private static BigInteger challenge(GroupElement Y, GroupElement H, GroupElement Gamma,
                                        GroupElement U, GroupElement V) throws NoSuchAlgorithmException {
        MessageDigest h = sha512();
        h.update(SUITE);
        h.update((byte) 0x02);
        h.update(point_to_string(Y));
        h.update(point_to_string(H));
        h.update(point_to_string(Gamma));
        h.update(point_to_string(U));
        h.update(point_to_string(V));
        h.update((byte) 0x00);
        byte[] c_string = h.digest();
        byte[] truncated = Arrays.copyOfRange(c_string, 0, cLen);
        return leToInt(truncated);
    }

    private static byte[] proof_to_hash_from_gamma(GroupElement Gamma) throws NoSuchAlgorithmException {
        MessageDigest h = sha512();
        h.update(SUITE);
        h.update((byte) 0x03);
        h.update(point_to_string(cofactorMultiply(Gamma)));
        h.update((byte) 0x00);
        return h.digest();
    }

    // 32-بایتی و معتبر
    private static byte[] normalizePublicKey(byte[] pkInput) throws Exception {
        if (pkInput == null || pkInput.length != 32)
            throw new Exception("ECVRF: malformed public key");
        if (string_to_point(pkInput) == null)
            throw new Exception("ECVRF: invalid public key encoding");
        return pkInput;
    }

    // فقط seed 32-بایتی + تطابق با pk
    private static byte[] extractSeed32(byte[] skInput, byte[] normalizedPk) throws Exception {
        if (skInput == null || skInput.length != 32)
            throw new Exception("ECVRF: malformed private key");
        byte[] x_le = expandSecretScalar(skInput);
        GroupElement Y = B.scalarMultiply(x_le);
        if (!Arrays.equals(point_to_string(Y), normalizedPk))
            throw new Exception("ECVRF: SK does not match PK");
        return skInput;
    }

    public static byte[] Hash(byte[] pi) throws Exception {
        DecodedProof dp = decodeProof(pi);
        if (dp == null) throw new Exception("ECVRF: decode error");
        return proof_to_hash_from_gamma(dp.Gamma);
    }

    public static class Result {
        public final byte[] pi;
        public final byte[] hash;
        public Result(byte[] pi, byte[] hash) { this.pi = pi; this.hash = hash; }
    }

    public static Result Prove(byte[] pkInput, byte[] skInput, byte[] m) throws Exception {
        byte[] pk = normalizePublicKey(pkInput);
        byte[] seed32 = extractSeed32(skInput, pk);

        byte[] x_le = expandSecretScalar(seed32);
        GroupElement Y = B.scalarMultiply(x_le);
        byte[] Yenc = point_to_string(Y);
        if (!Arrays.equals(Yenc, pk)) throw new Exception("ECVRF: SK does not match PK (seed mismatch)");

        GroupElement H = encodeToCurveTAI(pk, m);
        byte[] h_string = point_to_string(H);

        GroupElement Gamma = H.scalarMultiply(x_le);

        BigInteger k = nonceRFC8032(seed32, h_string);
        byte[] k_le = intToLE(k, qLen);

        GroupElement kB = B.scalarMultiply(k_le);
        GroupElement kH = H.scalarMultiply(k_le);

        BigInteger c = challenge(Y, H, Gamma, kB, kH);

        BigInteger xBig = leToInt(x_le);
        BigInteger s = k.add(c.multiply(xBig)).mod(q);

        byte[] pi = new byte[ProofSize];
        byte[] gamma_enc = point_to_string(Gamma);
        byte[] c_le_16 = intToLE(c, cLen);
        byte[] s_le_32 = intToLE(s, qLen);
        System.arraycopy(gamma_enc, 0, pi, 0, ptLen);
        System.arraycopy(c_le_16, 0, pi, ptLen, cLen);
        System.arraycopy(s_le_32, 0, pi, ptLen + cLen, qLen);

        byte[] beta = proof_to_hash_from_gamma(Gamma);
        return new Result(pi, beta);
    }

    public static boolean Verify(byte[] pkInput, byte[] pi, byte[] m) throws Exception {
        byte[] pk = normalizePublicKey(pkInput);

        GroupElement Y = string_to_point(pk);
        if (Y == null) throw new Exception("ECVRF: malformed input (pk)");

        // RFC 9381 §5.3 step 3: validate key
        if (isSmallSubgroup(Y)) return false;

        DecodedProof dp = decodeProof(pi);
        if (dp == null) return false;

        GroupElement Gamma = dp.Gamma;
        BigInteger c = dp.c;
        BigInteger s = dp.s;

        // Optional hardening: ensure Gamma isn't small-subgroup
        if (isSmallSubgroup(Gamma)) return false;

        GroupElement H = encodeToCurveTAI(pk, m);

        // U = sB - cY == sB + (q-c)Y
        byte[] s_le = intToLE(s, qLen);
        byte[] negc_le = intToLE(q.subtract(c).mod(q), qLen);

        GroupElement Pc = Y.scalarMultiply(negc_le);                // P3
        GroupElement GsCached = B.scalarMultiply(s_le).toCached();   // Cached
        GroupElement U = Pc.add(GsCached);                           // P3.add(Cached)

        // V = sH - cΓ == sH + (q-c)Γ
        GroupElement GammaC = Gamma.scalarMultiply(negc_le);         // P3
        GroupElement HsCached = H.scalarMultiply(s_le).toCached();   // Cached
        GroupElement V = GammaC.add(HsCached);                       // P3.add(Cached)

        BigInteger cPrime = challenge(Y, H, Gamma, U, V);
        return cPrime.compareTo(c) == 0;
    }

    private static class DecodedProof {
        final GroupElement Gamma;
        final BigInteger c;
        final BigInteger s;
        DecodedProof(GroupElement G, BigInteger c, BigInteger s) {
            this.Gamma = G; this.c = c; this.s = s;
        }
    }

    private static DecodedProof decodeProof(byte[] pi) throws Exception {
        if (pi == null || pi.length != ProofSize) throw new Exception("ECVRF: decode error");

        byte[] gamma_enc = Arrays.copyOfRange(pi, 0, ptLen);
        byte[] c_enc = Arrays.copyOfRange(pi, ptLen, ptLen + cLen);
        byte[] s_enc = Arrays.copyOfRange(pi, ptLen + cLen, ptLen + cLen + qLen);

        GroupElement Gamma = string_to_point(gamma_enc);
        if (Gamma == null) throw new Exception("ECVRF: decode error (Gamma)");

        BigInteger c = leToInt(c_enc);
        BigInteger s = leToInt(s_enc);
        if (s.compareTo(q) >= 0) throw new Exception("ECVRF: decode error (s out of range)");

        return new DecodedProof(Gamma, c, s);
    }

    public static byte[] HashToCurve(byte[] pkInput, byte[] m) throws Exception {
        byte[] pk = normalizePublicKey(pkInput);
        return point_to_string(encodeToCurveTAI(pk, m));
    }
}
