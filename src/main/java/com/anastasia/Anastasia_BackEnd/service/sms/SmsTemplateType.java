package com.anastasia.Anastasia_BackEnd.service.sms;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum SmsTemplateType {

    // --- Pre‑defined templates -------------------------------------------
    OTP("Your Anastasia verification code is %s. Valid %d min.") {
        @Override public String format(Map<String, Object> p) {
            return String.format(template,
                    require(p, "otp_code"),
                    p.getOrDefault("otp_expiry_minutes", 5));
        }
    },

    NOTIFICATION("Hello %s, %s") {
        @Override public String format(Map<String, Object> p) {
            return String.format(template,
                    require(p, "username"),
                    require(p, "message_content"));
        }
    },

    MARKETING("Special offer for %s: %s Visit %s") {
        @Override public String format(Map<String, Object> p) {
            return String.format(template,
                    require(p, "username"),
                    require(p, "promo_message"),
                    require(p, "link"));
        }
    },

    // --- Ad‑hoc messages --------------------------------------------------
    CUSTOM("") {                        // Body supplied directly in props
        @Override public String format(Map<String, Object> p) {
            return (String) p.getOrDefault("message_content", "");
        }
    };

    /* --------------------------------------------------------------------- */
    protected final String template;

    /** Override per‑constant where placeholders are used. */
    public String format(Map<String, Object> p) { return template; }

    /** Utility to enforce presence & type of a property. */
    @SuppressWarnings("unchecked")
    protected <T> T require(Map<String, Object> p, String key) {
        return Optional.ofNullable((T) p.get(key))
                .orElseThrow(() -> new IllegalArgumentException(key + " is required"));
    }
}
