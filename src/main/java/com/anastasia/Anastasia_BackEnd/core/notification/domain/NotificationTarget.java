package com.anastasia.Anastasia_BackEnd.core.notification.domain;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Snapshot of recipient contact points for a notification.
 * centralized, computed target (email/phone/whatsapp/tenant) to avoid scattering contact lookups
 */
public record NotificationTarget(
        UUID userId,
        String email,
        String phoneNumber,
        String whatsAppNumber,
        UUID tenantId
) {

    public static NotificationTarget fromUser(UserEntity user, Map<String, Object> properties) {
        if (user == null) {
            return new NotificationTarget(
                    null,
                    readString(properties, "email"),
                    readString(properties, "phone"),
                    readString(properties, "whatsApp"),
                    null
            );
        }

        MemberEntity membership = user.getMembership();

        return new NotificationTarget(
                user.getUuid(),
                user.getEmail(),
                Optional.ofNullable(membership)
                        .map(MemberEntity::getPhone)
                        .filter(StringUtils::hasText)
                        .orElse(readString(properties, "phone")),
                Optional.ofNullable(membership)
                        .map(MemberEntity::getWhatsApp)
                        .filter(StringUtils::hasText)
                        .orElse(readString(properties, "whatsApp")),
                user.getTenantId()
        );
    }

    private static String readString(Map<String, Object> properties, String key) {
        if (properties == null) {
            return null;
        }
        Object value = properties.get(key);
        return value == null ? null : value.toString();
    }
}

