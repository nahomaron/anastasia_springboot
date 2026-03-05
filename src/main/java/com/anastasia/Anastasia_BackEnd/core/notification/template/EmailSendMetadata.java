package com.anastasia.Anastasia_BackEnd.core.notification.template;

import java.util.UUID;

public record EmailSendMetadata(
        EmailCategory category,
        String correlationId,
        UUID tenantId,
        String idempotencyKey,
        String templateKey
) {

    public static EmailSendMetadata of(EmailCategory category, String templateKey) {
        return new EmailSendMetadata(category, null, null, null, templateKey);
    }
}
