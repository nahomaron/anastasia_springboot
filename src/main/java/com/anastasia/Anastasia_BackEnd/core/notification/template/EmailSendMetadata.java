package com.anastasia.Anastasia_BackEnd.core.notification.template;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;

import java.util.UUID;

public record EmailSendMetadata(
        EmailCategory category,
        String correlationId,
        UUID tenantId,
        String idempotencyKey,
        String templateKey
) {

    public static EmailSendMetadata of(EmailCategory category, String templateKey) {
        return new EmailSendMetadata(category, null, TenantContext.getTenantId(), null, templateKey);
    }

    public EmailSendMetadata withTenantId(UUID tenantId) {
        return new EmailSendMetadata(category, correlationId, tenantId, idempotencyKey, templateKey);
    }
}
