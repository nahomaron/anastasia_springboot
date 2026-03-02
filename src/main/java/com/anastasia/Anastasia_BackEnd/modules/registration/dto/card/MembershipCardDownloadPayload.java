package com.anastasia.Anastasia_BackEnd.modules.registration.dto.card;

public record MembershipCardDownloadPayload(
        byte[] content,
        String fileName,
        String contentType
) {
}
