package com.anastasia.Anastasia_BackEnd.modules.users.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;

public final class TotpUtils {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int DEFAULT_SECRET_BYTES = 20;
    private static final int TIME_STEP_SECONDS = 30;
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private TotpUtils() {
    }

    public static String generateSecretBase32() {
        byte[] secret = new byte[DEFAULT_SECRET_BYTES];
        RANDOM.nextBytes(secret);
        return encodeBase32(secret);
    }

    public static boolean verifyTotpCode(String base32Secret, String code, Instant now, int windowSteps) {
        if (base32Secret == null || base32Secret.isBlank() || code == null || !code.matches("\\d{6}")) {
            return false;
        }

        byte[] secret = decodeBase32(base32Secret);
        long counter = now.getEpochSecond() / TIME_STEP_SECONDS;

        for (int offset = -windowSteps; offset <= windowSteps; offset++) {
            long candidateCounter = counter + offset;
            if (candidateCounter < 0) {
                continue;
            }
            String candidate = generateTotp(secret, candidateCounter);
            if (candidate.equals(code)) {
                return true;
            }
        }
        return false;
    }

    public static String buildOtpauthUri(String issuer, String accountName, String base32Secret) {
        String safeIssuer = issuer == null || issuer.isBlank() ? "Anastasia" : issuer.trim();
        String safeAccount = accountName == null || accountName.isBlank() ? "user" : accountName.trim();

        String label = URLEncoder.encode(safeIssuer + ":" + safeAccount, StandardCharsets.UTF_8);
        String issuerParam = URLEncoder.encode(safeIssuer, StandardCharsets.UTF_8);
        return "otpauth://totp/" + label
                + "?secret=" + base32Secret
                + "&issuer=" + issuerParam
                + "&algorithm=SHA1&digits=6&period=30";
    }

    private static String generateTotp(byte[] secret, long counter) {
        byte[] data = new byte[8];
        long value = counter;
        for (int i = 7; i >= 0; i--) {
            data[i] = (byte) (value & 0xFF);
            value >>>= 8;
        }

        byte[] hash;
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret, "HmacSHA1"));
            hash = mac.doFinal(data);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to generate TOTP", ex);
        }

        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);
        int otp = binary % 1_000_000;
        return String.format(Locale.ROOT, "%06d", otp);
    }

    private static String encodeBase32(byte[] input) {
        StringBuilder output = new StringBuilder((input.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsLeft = 0;

        for (byte b : input) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0x1F;
                bitsLeft -= 5;
                output.append(BASE32_ALPHABET.charAt(index));
            }
        }

        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1F;
            output.append(BASE32_ALPHABET.charAt(index));
        }

        return output.toString();
    }

    private static byte[] decodeBase32(String input) {
        String normalized = input.trim().replace("=", "").replace(" ", "").toUpperCase(Locale.ROOT);
        int buffer = 0;
        int bitsLeft = 0;
        byte[] output = new byte[(normalized.length() * 5) / 8];
        int outPos = 0;

        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            int val = BASE32_ALPHABET.indexOf(c);
            if (val < 0) {
                throw new IllegalArgumentException("Invalid base32 character: " + c);
            }
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                output[outPos++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }

        if (outPos == output.length) {
            return output;
        }

        byte[] truncated = new byte[outPos];
        System.arraycopy(output, 0, truncated, 0, outPos);
        return truncated;
    }
}
