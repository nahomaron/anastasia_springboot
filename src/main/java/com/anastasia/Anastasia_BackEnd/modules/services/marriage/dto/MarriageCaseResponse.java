package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageCaseOriginType;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageCaseStatus;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageLanguageCode;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriagePairingMode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MarriageCaseResponse(
        UUID id,
        String caseReference,
        MarriageCaseStatus status,
        MarriageCaseOriginType originType,
        MarriagePairingMode pairingMode,
        MarriageLanguageCode primaryLanguage,
        UUID tenantId,
        Long churchId,
        String churchNumber,
        String churchName,
        boolean bothSubmitted,
        boolean secretaryClearanceComplete,
        boolean adminApprovalGranted,
        boolean confessorGateSatisfied,
        boolean manualPaymentSatisfied,
        boolean readyForScheduling,
        boolean ceremonyCompleted,
        boolean certificateIssued,
        LocalDateTime createdAt,
        List<MarriagePartySummaryResponse> parties
) {
}
