package com.anastasia.Anastasia_BackEnd.core.notification.service;

import com.anastasia.Anastasia_BackEnd.core.notification.domain.EmailSuppressionReason;

public interface EmailSuppressionService {

    void markSuppressed(String email, EmailSuppressionReason reason, String rawNotificationType);

    boolean isSuppressed(String email);
}
