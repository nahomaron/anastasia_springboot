package com.anastasia.Anastasia_BackEnd.core.notification.dto;

import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UpdateNotificationPreferencesRequest {
    @NotNull
    private Boolean emailEnabled;
    @NotNull
    private Boolean smsEnabled;
    @NotNull
    private Boolean inAppEnabled;
    private Set<NotificationType> mutedTypes;
}
