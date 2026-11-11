package org.delcom.ifs25009springbootcashflow.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Hash dan verifikasi password memakai SHA-256 (hex). */
public final class PasswordHasher {

    private PasswordHasher() {
    }

    public static String hash(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((rawPassword == null ? "" : rawPassword).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 tidak tersedia", ex);
        }
    }

    public static boolean matches(String rawPassword, String expectedHash) {
        return expectedHash != null && hash(rawPassword).equals(expectedHash);
    }
}
