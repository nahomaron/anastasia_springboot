package com.anastasia.Anastasia_BackEnd.modules.registration.dto.card;

import lombok.Builder;

@Builder
public record MembershipCardTemplateResponse(
        Long id,
        String templateKey,
        String displayName,
        String primaryColor,
        String accentColor,
        String textColor,
        String backgroundImageUrl,
        boolean isDefault,
        boolean builtIn,
        int sortOrder
) {
}
