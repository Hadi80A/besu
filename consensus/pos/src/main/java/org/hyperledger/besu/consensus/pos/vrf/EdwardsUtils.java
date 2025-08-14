

package org.hyperledger.besu.consensus.pos.vrf;

import net.i2p.crypto.eddsa.math.Curve;
import net.i2p.crypto.eddsa.math.GroupElement;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveSpec;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;

import java.math.BigInteger;
import java.util.Arrays;

final class EdwardsUtils {

    private static final EdDSANamedCurveSpec spec =
            EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519);
    private static final Curve curve = spec.getCurve();
    private static final GroupElement B = spec.getB();

    private static final String QS =
            "1000000000000000000000000000000014def9dea2f79cd65812631a5cf5d3ed";
    private static final BigInteger q = new BigInteger(QS, 16);

    private EdwardsUtils() {}

    static byte[] encodeBasePoint() { return B.toByteArray(); }

    static byte[] pointToString(byte[] enc32) {
        if (enc32.length != 32) throw new IllegalArgumentException("expected 32 bytes");
        return Arrays.copyOf(enc32, 32);
    }

    static GroupElement stringToPoint(byte[] enc32) {
        if (enc32.length != 32) return null;
        try { return curve.createPoint(enc32, true); }
        catch (IllegalArgumentException ex) { return null; }
    }

    static boolean isValidPoint(byte[] enc32) { return stringToPoint(enc32) != null; }

    static byte[] pointAddEncoded(byte[] aEnc, byte[] bEnc) {
        GroupElement a = stringToPoint(aEnc);
        GroupElement b = stringToPoint(bEnc);
        if (a == null || b == null) throw new IllegalArgumentException("invalid point");
        return a.add(b).toByteArray();
    }

    static BigInteger scalarToBigInt(byte[] scalarLE) {
        if (scalarLE.length != 32) throw new IllegalArgumentException("need 32 bytes");
        byte[] be = new byte[32];
        for (int i = 0; i < 32; i++) be[i] = scalarLE[31 - i];
        return new BigInteger(1, be);
    }

    static byte[] bigIntToScalar(BigInteger v) {
        v = v.mod(q);
        byte[] be = v.toByteArray();
        if (be.length > 32 && be[0] == 0) be = Arrays.copyOfRange(be, 1, be.length);
        if (be.length > 32) throw new IllegalArgumentException("BigInteger too large");
        byte[] le = new byte[32];
        for (int i = 0; i < be.length; i++) le[i] = be[be.length - 1 - i];
        return le;
    }

    static BigInteger littleEndianToBigInt(byte[] le) {
        byte[] be = new byte[le.length];
        for (int i = 0; i < le.length; i++) be[i] = le[le.length - 1 - i];
        return new BigInteger(1, be);
    }
}