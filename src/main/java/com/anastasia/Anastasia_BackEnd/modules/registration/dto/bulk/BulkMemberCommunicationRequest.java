package com.anastasia.Anastasia_BackEnd.modules.registration.dto.bulk;

import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkMemberCommunicationRequest {

    @NotNull
    private BulkMemberTargetType memberType;

    @NotEmpty
    private Set<Long> memberIds;

    @NotNull
    private NotificationChannelType channel;

    private String title;

    private String subject;

    @NotBlank
    private String message;
}
