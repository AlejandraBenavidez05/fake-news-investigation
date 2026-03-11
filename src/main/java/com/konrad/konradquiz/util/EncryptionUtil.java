package com.konrad.konradquiz.util;

import com.konrad.konradquiz.exception.EncryptionException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.Base64;

@Component
public class EncryptionUtil {

    // PBKDF2 — slow by design, defeats brute force attacks on emails
    private static final String ALGORITHM   = "PBKDF2WithHmacSHA256";
    private static final int    ITERATIONS  = 310_000;  // OWASP 2024 recommendation
    private static final int    KEY_LENGTH  = 256;
    // Static salt is acceptable here — we only need consistency, not password security
    private static final byte[] SALT        = "konradquiz-email-salt".getBytes();

    public String hash(String value) {
        if (value == null) return null;
        try {
            PBEKeySpec spec = new PBEKeySpec(
                    value.toLowerCase().trim().toCharArray(),
                    SALT,
                    ITERATIONS,
                    KEY_LENGTH
            );
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            spec.clearPassword(); // security best practice — clear from memory
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception ex) {
            throw new EncryptionException("Failed to hash value.", ex);
        }
    }
}