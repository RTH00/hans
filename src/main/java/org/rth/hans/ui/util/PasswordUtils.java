package org.rth.hans.ui.util;

import org.rth.hans.ui.User;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.security.auth.DestroyFailedException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

public class PasswordUtils {

    private static final int ITERATIONS = 1804;

    public static User.Identification generateHashing(final String password)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        final byte[] salt = generateSalt();
        final String hashedPassword = generateStrongPasswordHash(password, salt);
        return new User.Identification(hashedPassword, encode64(salt));
    }

    public static boolean verifyPassword(final User.Identification userIdentification,
                                         final String passwordToTest)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        final byte[] salt = decode64(userIdentification.getSalt());
        final String hashedPassword = userIdentification.getHashedPassword();
        return hashedPassword.equals(generateStrongPasswordHash(passwordToTest, salt));
    }

    private static String generateStrongPasswordHash(final String password, final byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        char[] chars = password.toCharArray();
        final PBEKeySpec spec = new PBEKeySpec(chars, salt, ITERATIONS, 2048);
        final SecretKey secretKey = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec);
        final byte[] hash = secretKey.getEncoded();
        return encode64(hash);
    }

    private static byte[] generateSalt() {
        final SecureRandom sr = new SecureRandom();
        final byte[] salt = new byte[256];
        sr.nextBytes(salt);
        return salt;
    }

    private static String encode64(final byte[] salt) {
        return Base64.getEncoder().encodeToString(salt);
    }

    private static byte[] decode64(final String salt) {
        return Base64.getDecoder().decode(salt);
    }

}
