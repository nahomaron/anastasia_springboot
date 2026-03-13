package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

public record MarriageCertificateIssueRequest(
        String registryReference,
        String issueNote
) {
}
