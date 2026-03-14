package com.anastasia.Anastasia_BackEnd.common.utils;

public final class JwtSecretGenerator {

    private JwtSecretGenerator() {
    }

    public static void main(String[] args) {
        String secret = JwtUtil.generateBase64Secret();
        System.out.println("Generated JWT secret:");
        System.out.println(secret);
        System.out.println();
        System.out.println(".env:");
        System.out.println("ANASTASIA_JWT_CURRENT_SECRET=" + secret);
        System.out.println("ANASTASIA_JWT_PREVIOUS_SECRET=");
    }
}
