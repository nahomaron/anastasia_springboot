package com.anastasia.Anastasia_BackEnd.modules.services.marriage.validation;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageCaseEntity;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageCaseStatus;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriagePartyEntity;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriagePartyRole;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.service.MarriageWorkflowPolicy;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;

@Component
public class MarriageCaseDomainValidator {

    private final MarriageWorkflowPolicy workflowPolicy;

    public MarriageCaseDomainValidator(MarriageWorkflowPolicy workflowPolicy) {
        this.workflowPolicy = workflowPolicy;
    }

    public void validateStatusTransition(MarriageCaseStatus from, MarriageCaseStatus to) {
        workflowPolicy.assertTransitionAllowed(from, to);
    }

    public void requireExactlyTwoDistinctParties(List<MarriagePartyEntity> parties) {
        if (parties == null || parties.size() != 2) {
            throw new IllegalStateException("A marriage case must contain exactly two parties.");
        }

        EnumSet<MarriagePartyRole> roles = EnumSet.noneOf(MarriagePartyRole.class);
        for (MarriagePartyEntity party : parties) {
            if (party == null || party.getPartyRole() == null) {
                throw new IllegalStateException("Each marriage party must declare a party role.");
            }
            roles.add(party.getPartyRole());
        }

        if (!roles.contains(MarriagePartyRole.BRIDE) || !roles.contains(MarriagePartyRole.GROOM)) {
            throw new IllegalStateException("Marriage parties must include one bride and one groom.");
        }
    }

    public void requireBothPartiesSubmitted(List<MarriagePartyEntity> parties) {
        requireExactlyTwoDistinctParties(parties);
        boolean allSubmitted = parties.stream().allMatch(MarriagePartyEntity::isSubmitted);
        if (!allSubmitted) {
            throw new IllegalStateException("Both parties must submit before the case can advance.");
        }
    }

    public void requireCaseReadyForCertificate(MarriageCaseEntity marriageCase) {
        if (marriageCase == null) {
            throw new IllegalStateException("Marriage case is required.");
        }
        if (!marriageCase.isCeremonyCompleted()) {
            throw new IllegalStateException("Certificate issuance requires ceremony completion.");
        }
        if (!marriageCase.isConfessorGateSatisfied()) {
            throw new IllegalStateException("Certificate issuance requires the confessor gate to be satisfied.");
        }
        if (!marriageCase.isAdminApprovalGranted()) {
            throw new IllegalStateException("Certificate issuance requires admin approval.");
        }
    }
}
