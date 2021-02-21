package org.keycloak.multipleds.storage.user.utils;

import java.math.BigInteger;
import java.security.MessageDigest;

/**
 * Utiliy class for SHA-1 salted encoding
 */
public class SHA1Utils {
    private static final String ALGORITHM = "SHA-1";

    private static String encode(String rawPassword) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            md.update(rawPassword.getBytes());
            BigInteger aux = new BigInteger(1, md.digest());

            // convert BigInteger to 40-char lowercase string using leading 0s
            return String.format("%040x", aux);
        } catch (Exception ignored) {
        }
        return null;
    }

    public static String encodeWithSalt(String rawPassword, String salt) {
        return encode(saltPassword(rawPassword, salt));
    }

    private static String saltPassword(String password, String salt) {
        return salt + password;
    }
}
