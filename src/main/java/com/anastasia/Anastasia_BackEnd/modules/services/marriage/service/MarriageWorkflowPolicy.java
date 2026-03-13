package com.anastasia.Anastasia_BackEnd.modules.services.marriage.service;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageCaseStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class MarriageWorkflowPolicy {

    private final Map<MarriageCaseStatus, Set<MarriageCaseStatus>> allowedTransitions =
            new EnumMap<>(MarriageCaseStatus.class);

    public MarriageWorkflowPolicy() {
        allow(MarriageCaseStatus.DRAFT,
                MarriageCaseStatus.WAITING_FOR_COUNTERPART,
                MarriageCaseStatus.WAITING_FOR_BOTH_SUBMISSIONS);
        allow(MarriageCaseStatus.WAITING_FOR_COUNTERPART, MarriageCaseStatus.WAITING_FOR_BOTH_SUBMISSIONS);
        allow(MarriageCaseStatus.WAITING_FOR_BOTH_SUBMISSIONS, MarriageCaseStatus.BOTH_SUBMITTED);
        allow(MarriageCaseStatus.BOTH_SUBMITTED, MarriageCaseStatus.SECRETARY_REVIEW_PENDING);
        allow(MarriageCaseStatus.SECRETARY_REVIEW_PENDING,
                MarriageCaseStatus.SECRETARY_DOCUMENTS_REQUESTED,
                MarriageCaseStatus.SECRETARY_RETURNED_FOR_CORRECTION,
                MarriageCaseStatus.SECRETARY_CIVIL_CHECKS_APPROVED,
                MarriageCaseStatus.SECRETARY_REJECTED);
        allow(MarriageCaseStatus.SECRETARY_DOCUMENTS_REQUESTED, MarriageCaseStatus.SECRETARY_REVIEW_PENDING);
        allow(MarriageCaseStatus.SECRETARY_RETURNED_FOR_CORRECTION, MarriageCaseStatus.WAITING_FOR_BOTH_SUBMISSIONS);
        allow(MarriageCaseStatus.SECRETARY_CIVIL_CHECKS_APPROVED, MarriageCaseStatus.WAITING_FOR_CONFESSOR_APPROVAL);
        allow(MarriageCaseStatus.WAITING_FOR_CONFESSOR_APPROVAL,
                MarriageCaseStatus.CONFESSOR_BLOCKED,
                MarriageCaseStatus.ADMIN_REVIEW_PENDING,
                MarriageCaseStatus.DIOCESE_OVERRIDE_RECORDED);
        allow(MarriageCaseStatus.CONFESSOR_BLOCKED, MarriageCaseStatus.DIOCESE_OVERRIDE_RECORDED);
        allow(MarriageCaseStatus.DIOCESE_OVERRIDE_RECORDED, MarriageCaseStatus.ADMIN_REVIEW_PENDING);
        allow(MarriageCaseStatus.ADMIN_REVIEW_PENDING,
                MarriageCaseStatus.ADMIN_ON_HOLD,
                MarriageCaseStatus.ADMIN_RETURNED_FOR_CORRECTION,
                MarriageCaseStatus.SECRETARY_REJECTED,
                MarriageCaseStatus.ADMIN_APPROVED_PENDING_PAYMENT);
        allow(MarriageCaseStatus.ADMIN_ON_HOLD, MarriageCaseStatus.ADMIN_REVIEW_PENDING);
        allow(MarriageCaseStatus.ADMIN_RETURNED_FOR_CORRECTION, MarriageCaseStatus.WAITING_FOR_BOTH_SUBMISSIONS);
        allow(MarriageCaseStatus.ADMIN_APPROVED_PENDING_PAYMENT,
                MarriageCaseStatus.WAITING_FOR_MANUAL_PAYMENT,
                MarriageCaseStatus.READY_FOR_SCHEDULING);
        allow(MarriageCaseStatus.WAITING_FOR_MANUAL_PAYMENT, MarriageCaseStatus.PAYMENT_CONFIRMED);
        allow(MarriageCaseStatus.PAYMENT_CONFIRMED, MarriageCaseStatus.READY_FOR_SCHEDULING);
        allow(MarriageCaseStatus.READY_FOR_SCHEDULING, MarriageCaseStatus.PRIEST_ASSIGNED);
        allow(MarriageCaseStatus.PRIEST_ASSIGNED, MarriageCaseStatus.SCHEDULED);
        allow(MarriageCaseStatus.SCHEDULED,
                MarriageCaseStatus.CEREMONY_CONFIRMED,
                MarriageCaseStatus.CEREMONY_POSTPONED,
                MarriageCaseStatus.CEREMONY_CANCELLED,
                MarriageCaseStatus.CEREMONY_COMPLETED);
        allow(MarriageCaseStatus.CEREMONY_CONFIRMED,
                MarriageCaseStatus.CEREMONY_POSTPONED,
                MarriageCaseStatus.CEREMONY_CANCELLED,
                MarriageCaseStatus.CEREMONY_COMPLETED);
        allow(MarriageCaseStatus.CEREMONY_POSTPONED, MarriageCaseStatus.SCHEDULED);
        allow(MarriageCaseStatus.CEREMONY_COMPLETED, MarriageCaseStatus.CERTIFICATE_READY);
        allow(MarriageCaseStatus.CERTIFICATE_READY, MarriageCaseStatus.CERTIFICATE_ISSUED);
        allow(MarriageCaseStatus.CERTIFICATE_ISSUED, MarriageCaseStatus.CASE_CLOSED);
    }

    public boolean canTransition(MarriageCaseStatus from, MarriageCaseStatus to) {
        if (from == null || to == null) {
            return false;
        }
        if (from == to) {
            return true;
        }
        return allowedTransitions.getOrDefault(from, EnumSet.noneOf(MarriageCaseStatus.class)).contains(to);
    }

    public void assertTransitionAllowed(MarriageCaseStatus from, MarriageCaseStatus to) {
        if (!canTransition(from, to)) {
            throw new IllegalStateException("Illegal marriage case status transition: " + from + " -> " + to);
        }
    }

    public Set<MarriageCaseStatus> getAllowedTransitions(MarriageCaseStatus from) {
        Set<MarriageCaseStatus> transitions = allowedTransitions.get(from);
        if (transitions == null || transitions.isEmpty()) {
            return EnumSet.noneOf(MarriageCaseStatus.class);
        }
        return EnumSet.copyOf(transitions);
    }

    private void allow(MarriageCaseStatus from, MarriageCaseStatus... toStatuses) {
        allowedTransitions.computeIfAbsent(from, ignored -> EnumSet.noneOf(MarriageCaseStatus.class))
                .addAll(Set.of(toStatuses));
    }
}
