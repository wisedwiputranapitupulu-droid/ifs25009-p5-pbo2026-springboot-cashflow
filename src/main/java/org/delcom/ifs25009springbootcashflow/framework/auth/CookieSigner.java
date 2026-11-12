package org.delcom.ifs25009springbootcashflow.framework.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Menandatangani nilai cookie dengan HMAC-SHA256 agar ID pengguna di cookie tidak bisa dipalsukan.
 * Format nilai: {@code <userId>.<signature-hex>}.
 *
 * <p>Kunci diambil dari {@code COOKIE_SECRET} (env / .env). Bila kosong, dibuat kunci acak per proses
 * (cookie lama otomatis tidak berlaku setelah aplikasi restart).</p>
 */
final class CookieSigner {
    private static final String ALGORITHM = "HmacSHA256";
    private static final byte[] KEY = resolveKey();

    private CookieSigner() {
    }

    private static byte[] resolveKey() {
        String configured = System.getProperty("COOKIE_SECRET");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("COOKIE_SECRET");
        }
        if (configured != null && !configured.isBlank()) {
            return configured.getBytes(StandardCharsets.UTF_8);
        }
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        return random;
    }

    static String sign(int userId) {
        return userId + "." + signature(userId, KEY);
    }

    /** Mengembalikan ID pengguna bila tanda tangan valid, selain itu null. */
    static Integer verify(String cookieValue) {
        return verify(cookieValue, KEY);
    }

    static String sign(int userId, byte[] key) {
        return userId + "." + signature(userId, key);
    }

    static Integer verify(String cookieValue, byte[] key) {
        if (cookieValue == null) {
            return null;
        }
        int dot = cookieValue.indexOf('.');
        if (dot <= 0 || dot == cookieValue.length() - 1) {
            return null;
        }
        try {
            int userId = Integer.parseInt(cookieValue.substring(0, dot));
            byte[] given = cookieValue.substring(dot + 1).getBytes(StandardCharsets.UTF_8);
            byte[] expected = signature(userId, key).getBytes(StandardCharsets.UTF_8);
            return MessageDigest.isEqual(given, expected) ? userId : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String signature(int userId, byte[] key) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key, ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(Integer.toString(userId).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HMAC-SHA256 tidak tersedia", ex);
        }
    }
}
