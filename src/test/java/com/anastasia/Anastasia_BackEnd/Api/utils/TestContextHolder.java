package com.anastasia.Anastasia_BackEnd.Api.utils;

/**
 * Thread-safe context holder for storing test-specific data,
 * such as the current user's email or tenant ID.
 * Useful when running tests in parallel or when TestWatcher needs
 * access to the current test's user identity.
 */
public final class TestContextHolder {

    private static final ThreadLocal<String> currentEmail = new ThreadLocal<>();

    private TestContextHolder() {
        // Utility class — prevent instantiation
    }

    /** Store the current test user's email */
    public static void setEmail(String email) {
        currentEmail.set(email);
    }

    /** Retrieve the current test user's email (null-safe) */
    public static String getEmail() {
        return currentEmail.get();
    }

    /** Clear the stored context — call this at the end of each test */
    public static void clear() {
        currentEmail.remove();
    }
}
