package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageConfessorApprovalMode;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageConfessorApprovalStatus;

import java.time.LocalDate;
import java.util.UUID;

public record MarriageConfessorApprovalResponse(
        UUID id,
        UUID partyId,
        MarriageConfessorApprovalStatus approvalStatus,
        MarriageConfessorApprovalMode approvalMode,
        UUID priestUserId,
        String priestPersonName,
        String churchName,
        String dioceseName,
        LocalDate approvalDate,
        UUID evidenceDocumentId,
        String notes,
        boolean blocking,
        String overrideReason,
        UUID overrideDocumentId
) {
}
