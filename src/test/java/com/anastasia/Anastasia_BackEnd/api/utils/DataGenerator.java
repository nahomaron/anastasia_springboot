package com.anastasia.Anastasia_BackEnd.api.utils;

import com.github.javafaker.Faker;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DataGenerator {
    private static final Faker faker = new Faker();
    private static final SecureRandom random = new SecureRandom();
    private static final String ALLOWED_SPECIAL = "@$!%*?&";
    private static final String ALLOWED_ALPHA_NUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String PASSWORD_ALLOWED = ALLOWED_ALPHA_NUM + ALLOWED_SPECIAL;

    public static String randomEmail() {
        return faker.internet().emailAddress();
    }

    public static String randomName() {
        String generated = faker.name().firstName() + " " + faker.name().lastName();
        generated = generated.replaceAll("[^a-zA-Z0-9_\\s]", "").trim();

        if (generated.length() < 3) {
            generated = "User" + faker.number().digits(3);
        }

        if (generated.length() > 30) {
            generated = generated.substring(0, 30).trim();
        }

        if (generated.isEmpty()) {
            generated = "User" + faker.number().digits(4);
        }

        return generated;
    }

    public static String randomPassword() {
        int targetLength = 8 + random.nextInt(8); // between 8 and 15 inclusive

        List<Character> passwordChars = new ArrayList<>();
        passwordChars.add(randomChar("ABCDEFGHIJKLMNOPQRSTUVWXYZ"));
        passwordChars.add(randomChar("abcdefghijklmnopqrstuvwxyz"));
        passwordChars.add(randomChar("0123456789"));
        passwordChars.add(randomChar(ALLOWED_SPECIAL));

        while (passwordChars.size() < targetLength) {
            passwordChars.add(randomChar(PASSWORD_ALLOWED));
        }

        Collections.shuffle(passwordChars, random);

        StringBuilder password = new StringBuilder(targetLength);
        for (Character ch : passwordChars) {
            password.append(ch);
        }
        return password.toString();
    }

    private static char randomChar(String candidates) {
        return candidates.charAt(random.nextInt(candidates.length()));
    }
}
