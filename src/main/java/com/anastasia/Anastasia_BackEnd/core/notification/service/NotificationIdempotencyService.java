package com.anastasia.Anastasia_BackEnd.core.notification.service;

import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationChannelType;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationEvent;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Service
public class NotificationIdempotencyService {

    public String computeKey(NotificationEvent event, NotificationChannelType channel, String recipient) {
        if (event == null) {
            return null;
        }
        StringBuilder data = new StringBuilder();
        data.append(event.getType().name()).append('|');
        data.append(channel.name()).append('|');
        data.append(recipient == null ? "" : recipient.toLowerCase(Locale.ROOT)).append('|');
        if (event.getUser() != null && event.getUser().getUuid() != null) {
            data.append(event.getUser().getUuid());
        }
        data.append('|');
        Map<String, Object> sorted = new TreeMap<>(event.getProperties());
        data.append(sorted);
        return sha256Hex(data.toString());
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate idempotency key", ex);
        }
    }
}
