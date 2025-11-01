package com.anastasia.Anastasia_BackEnd.Api.utils;

/**
 * Maintains request metadata for the current thread so helpers like {@link SchemaValidator}
 * can correlate responses back to the originating request.
 */
public final class RequestTracker {

    private static final ThreadLocal<String> LAST_REQUEST_URI = new ThreadLocal<>();

    private RequestTracker() {
    }

    public static void record(String uri) {
        if (uri != null && !uri.isBlank()) {
            LAST_REQUEST_URI.set(uri);
        }
    }

    public static String getLastRequestUri() {
        return LAST_REQUEST_URI.get();
    }

    public static void clear() {
        LAST_REQUEST_URI.remove();
    }
}

