package com.anastasia.Anastasia_BackEnd.seeder.seeders;

import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

final class SeederRandomUtils {

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "@$!%*?&";
    private static final String ALL = UPPER + LOWER + DIGITS + SPECIAL;

    private SeederRandomUtils() {
        throw new IllegalStateException("Utility class");
    }

    static String generateSecurePassword(int minLength, int maxLength) {
        if (minLength < 4 || maxLength < minLength) {
            throw new IllegalArgumentException("Password length range must be at least 4 characters and valid.");
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int passwordLength = random.nextInt(minLength, maxLength + 1);

        StringBuilder builder = new StringBuilder(passwordLength);
        builder.append(pickRandomChar(random, UPPER));
        builder.append(pickRandomChar(random, LOWER));
        builder.append(pickRandomChar(random, DIGITS));
        builder.append(pickRandomChar(random, SPECIAL));

        for (int i = builder.length(); i < passwordLength; i++) {
            builder.append(pickRandomChar(random, ALL));
        }

        char[] passwordChars = builder.toString().toCharArray();
        for (int i = passwordChars.length - 1; i > 0; i--) {
            int swapIndex = random.nextInt(i + 1);
            char tmp = passwordChars[i];
            passwordChars[i] = passwordChars[swapIndex];
            passwordChars[swapIndex] = tmp;
        }

        return new String(passwordChars);
    }

    static LocalDate randomBirthdate(int minAgeInclusive, int maxAgeInclusive) {
        if (minAgeInclusive < 0 || maxAgeInclusive < minAgeInclusive) {
            throw new IllegalArgumentException("Age range must be non-negative and valid.");
        }

        LocalDate now = LocalDate.now();
        long minDay = now.minusYears(maxAgeInclusive).toEpochDay();
        long maxDay = now.minusYears(minAgeInclusive).toEpochDay();

        long randomDay = ThreadLocalRandom.current().nextLong(minDay, maxDay + 1);
        return LocalDate.ofEpochDay(randomDay);
    }

    private static char pickRandomChar(ThreadLocalRandom random, String pool) {
        return pool.charAt(random.nextInt(pool.length()));
    }
}

