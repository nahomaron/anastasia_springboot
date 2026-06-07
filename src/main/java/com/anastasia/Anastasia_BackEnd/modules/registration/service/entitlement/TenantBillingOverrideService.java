package com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement;

import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.TenantBillingChargeSummaryResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.TenantBillingOverrideRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.BillingOverrideType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantBillingOverrideAuditAction;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantBillingOverrideAuditEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantBillingOverrideEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantBillingOverrideAuditRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantBillingOverrideRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.TenantPlanBillingCatalog;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantBillingOverrideService {

    private final TenantBillingOverrideRepository tenantBillingOverrideRepository;
    private final TenantBillingOverrideAuditRepository tenantBillingOverrideAuditRepository;
    private final TenantRepository tenantRepository;
    private final TenantPlanBillingCatalog billingCatalog;
    private final LocalizedMessageService messageService;

    @Transactional(readOnly = true)
    public Optional<TenantBillingOverrideEntity> findActiveOverride(UUID tenantId, Instant now) {
        return tenantBillingOverrideRepository.findActiveCandidatesByTenantId(tenantId).stream()
                .filter(override -> override.isEffective(now))
                .max(Comparator.comparing(TenantBillingOverrideEntity::getStartsAt)
                        .thenComparing(TenantBillingOverrideEntity::getCreatedAt));
    }

    @Transactional(readOnly = true)
    public List<TenantBillingOverrideEntity> listOverrideHistory(UUID tenantId) {
        requireTenant(tenantId);
        return tenantBillingOverrideRepository.findByTenant_IdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional
    public TenantBillingOverrideEntity createOverride(UUID tenantId,
                                                      @Valid TenantBillingOverrideRequest request,
                                                      UUID actorUserId) {
        TenantEntity tenant = requireTenant(tenantId);
        Instant now = Instant.now();
        Instant startsAt = request.getStartsAt() != null ? request.getStartsAt() : now;
        validateRequest(request, startsAt, request.getEndsAt());
        ensureNoOverlappingOverride(tenantId, startsAt, request.getEndsAt(), null);

        TenantBillingOverrideEntity entity = TenantBillingOverrideEntity.builder()
                .tenant(tenant)
                .overrideType(request.getOverrideType())
                .startsAt(startsAt)
                .endsAt(request.getEndsAt())
                .discountPercent(normalizeDiscount(request.getDiscountPercent()))
                .fixedAmountMinor(request.getFixedAmountMinor())
                .currency(resolveCurrency(request.getCurrency()))
                .reason(normalizeText(request.getReason(), 512))
                .internalNote(normalizeText(request.getInternalNote(), 1024))
                .createdByUserId(actorUserId)
                .updatedByUserId(actorUserId)
                .build();
        TenantBillingOverrideEntity saved = tenantBillingOverrideRepository.save(entity);
        audit(saved, TenantBillingOverrideAuditAction.CREATED, null, summarize(saved), saved.getReason(), actorUserId);
        return saved;
    }

    @Transactional
    public TenantBillingOverrideEntity updateOverride(UUID tenantId,
                                                      UUID overrideId,
                                                      @Valid TenantBillingOverrideRequest request,
                                                      UUID actorUserId) {
        TenantBillingOverrideEntity entity = tenantBillingOverrideRepository.findByIdAndTenantId(overrideId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                        "tenant.billingOverride.notFound",
                        "Billing override not found"
                )));
        if (entity.getRevokedAt() != null || !entity.isActive()) {
            throw new IllegalStateException(messageService.get(
                    "tenant.billingOverride.revoked",
                    "Revoked billing overrides cannot be updated"
            ));
        }

        Instant startsAt = request.getStartsAt() != null ? request.getStartsAt() : entity.getStartsAt();
        Instant endsAt = request.getEndsAt();
        validateRequest(request, startsAt, endsAt);
        ensureNoOverlappingOverride(tenantId, startsAt, endsAt, entity.getId());

        String oldSummary = summarize(entity);
        entity.setOverrideType(request.getOverrideType());
        entity.setStartsAt(startsAt);
        entity.setEndsAt(endsAt);
        entity.setDiscountPercent(normalizeDiscount(request.getDiscountPercent()));
        entity.setFixedAmountMinor(request.getFixedAmountMinor());
        entity.setCurrency(resolveCurrency(request.getCurrency()));
        entity.setReason(normalizeText(request.getReason(), 512));
        entity.setInternalNote(normalizeText(request.getInternalNote(), 1024));
        entity.setUpdatedByUserId(actorUserId);
        TenantBillingOverrideEntity saved = tenantBillingOverrideRepository.save(entity);
        audit(saved, TenantBillingOverrideAuditAction.UPDATED, oldSummary, summarize(saved), saved.getReason(), actorUserId);
        return saved;
    }

    @Transactional
    public void revokeOverride(UUID tenantId, UUID overrideId, String reason, UUID actorUserId) {
        TenantBillingOverrideEntity entity = tenantBillingOverrideRepository.findByIdAndTenantId(overrideId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                        "tenant.billingOverride.notFound",
                        "Billing override not found"
                )));
        if (entity.getRevokedAt() != null || !entity.isActive()) {
            return;
        }

        String oldSummary = summarize(entity);
        Instant now = Instant.now();
        entity.setActive(false);
        entity.setRevokedAt(now);
        entity.setRevokedByUserId(actorUserId);
        entity.setUpdatedByUserId(actorUserId);
        entity.setEndsAt(entity.getEndsAt() == null || entity.getEndsAt().isAfter(now) ? now : entity.getEndsAt());
        entity.setReason(normalizeText(reason, 512));
        TenantBillingOverrideEntity saved = tenantBillingOverrideRepository.save(entity);
        audit(saved, TenantBillingOverrideAuditAction.REVOKED, oldSummary, summarize(saved), saved.getReason(), actorUserId);
    }

    @Transactional(readOnly = true)
    public TenantBillingChargeSummaryResponse calculateCharge(UUID tenantId, SubscriptionPlan plan, Instant now) {
        String currency = billingCatalog.getCurrency();
        long normalAmountMinor = resolvePlanAmountMinor(plan);
        Optional<TenantBillingOverrideEntity> activeOverride = findActiveOverride(tenantId, now);
        if (activeOverride.isEmpty()) {
            return TenantBillingChargeSummaryResponse.builder()
                    .normalAmountMinor(normalAmountMinor)
                    .discountAmountMinor(0)
                    .effectiveAmountMinor(normalAmountMinor)
                    .currency(currency)
                    .build();
        }

        TenantBillingOverrideEntity override = activeOverride.get();
        long effectiveAmountMinor = normalAmountMinor;
        long discountAmountMinor = 0;
        switch (override.getOverrideType()) {
            case FREE_ACCESS, TRIAL_EXTENSION, COMPED_UNTIL_DATE -> {
                effectiveAmountMinor = 0;
                discountAmountMinor = normalAmountMinor;
            }
            case PERCENT_DISCOUNT -> {
                BigDecimal percent = override.getDiscountPercent() == null
                        ? BigDecimal.ZERO
                        : override.getDiscountPercent().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                BigDecimal discount = BigDecimal.valueOf(normalAmountMinor).multiply(percent);
                discountAmountMinor = discount.setScale(0, RoundingMode.HALF_UP).longValue();
                effectiveAmountMinor = Math.max(0L, normalAmountMinor - discountAmountMinor);
            }
            case FIXED_PRICE -> {
                effectiveAmountMinor = Math.max(0L, override.getFixedAmountMinor() != null ? override.getFixedAmountMinor() : 0L);
                discountAmountMinor = Math.max(0L, normalAmountMinor - effectiveAmountMinor);
            }
        }

        return TenantBillingChargeSummaryResponse.builder()
                .normalAmountMinor(normalAmountMinor)
                .discountAmountMinor(discountAmountMinor)
                .effectiveAmountMinor(effectiveAmountMinor)
                .currency(resolveCurrency(override.getCurrency()))
                .appliedOverrideId(override.getId())
                .appliedBillingOverrideType(override.getOverrideType())
                .overrideEndsAt(override.getEndsAt())
                .build();
    }

    @Transactional(readOnly = true)
    public boolean preservesAccess(TenantSubscriptionEntity subscription, Instant now) {
        if (subscription == null || subscription.getTenant() == null || subscription.getTenant().getId() == null) {
            return false;
        }
        Optional<TenantBillingOverrideEntity> activeOverride = findActiveOverride(subscription.getTenant().getId(), now);
        if (activeOverride.isEmpty()) {
            return false;
        }
        BillingOverrideType type = activeOverride.get().getOverrideType();
        return type == BillingOverrideType.TRIAL_EXTENSION
                || type == BillingOverrideType.FREE_ACCESS
                || type == BillingOverrideType.COMPED_UNTIL_DATE;
    }

    private void validateRequest(TenantBillingOverrideRequest request, Instant startsAt, Instant endsAt) {
        if (startsAt == null) {
            throw new IllegalArgumentException(messageService.get(
                    "tenant.billingOverride.startsAt.required",
                    "startsAt is required"
            ));
        }
        if (endsAt != null && !endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException(messageService.get(
                    "tenant.billingOverride.dateRange.invalid",
                    "endsAt must be after startsAt"
            ));
        }
        if (request.getOverrideType() == BillingOverrideType.PERCENT_DISCOUNT) {
            if (request.getDiscountPercent() == null
                    || request.getDiscountPercent().compareTo(BigDecimal.ZERO) <= 0
                    || request.getDiscountPercent().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException(messageService.get(
                        "tenant.billingOverride.discount.invalid",
                        "discountPercent must be greater than 0 and less than or equal to 100"
                ));
            }
        } else if (request.getDiscountPercent() != null) {
            throw new IllegalArgumentException(messageService.get(
                    "tenant.billingOverride.discount.unexpected",
                    "discountPercent is only allowed for PERCENT_DISCOUNT overrides"
            ));
        }

        if (request.getOverrideType() == BillingOverrideType.FIXED_PRICE) {
            if (request.getFixedAmountMinor() == null || request.getFixedAmountMinor() < 0) {
                throw new IllegalArgumentException(messageService.get(
                        "tenant.billingOverride.fixedAmount.invalid",
                        "fixedAmountMinor must be zero or positive"
                ));
            }
        } else if (request.getFixedAmountMinor() != null) {
            throw new IllegalArgumentException(messageService.get(
                    "tenant.billingOverride.fixedAmount.unexpected",
                    "fixedAmountMinor is only allowed for FIXED_PRICE overrides"
            ));
        }

        if ((request.getOverrideType() == BillingOverrideType.TRIAL_EXTENSION
                || request.getOverrideType() == BillingOverrideType.COMPED_UNTIL_DATE)
                && endsAt == null) {
            throw new IllegalArgumentException(messageService.get(
                    "tenant.billingOverride.endsAt.required",
                    "endsAt is required for temporary billing overrides"
            ));
        }
    }

    private void ensureNoOverlappingOverride(UUID tenantId, Instant startsAt, Instant endsAt, UUID currentOverrideId) {
        List<TenantBillingOverrideEntity> candidates = tenantBillingOverrideRepository.findActiveCandidatesByTenantId(tenantId);
        for (TenantBillingOverrideEntity candidate : candidates) {
            if (currentOverrideId != null && currentOverrideId.equals(candidate.getId())) {
                continue;
            }
            if (intervalsOverlap(startsAt, endsAt, candidate.getStartsAt(), candidate.getEndsAt())) {
                throw new IllegalStateException(messageService.get(
                        "tenant.billingOverride.overlap",
                        "Only one active billing override can apply to a tenant at a time"
                ));
            }
        }
    }

    private boolean intervalsOverlap(Instant firstStart, Instant firstEnd, Instant secondStart, Instant secondEnd) {
        Instant effectiveFirstEnd = firstEnd != null ? firstEnd : Instant.MAX;
        Instant effectiveSecondEnd = secondEnd != null ? secondEnd : Instant.MAX;
        return firstStart.isBefore(effectiveSecondEnd) && secondStart.isBefore(effectiveFirstEnd);
    }

    private String summarize(TenantBillingOverrideEntity entity) {
        return "type=" + entity.getOverrideType()
                + ",startsAt=" + entity.getStartsAt()
                + ",endsAt=" + entity.getEndsAt()
                + ",discountPercent=" + entity.getDiscountPercent()
                + ",fixedAmountMinor=" + entity.getFixedAmountMinor()
                + ",currency=" + entity.getCurrency()
                + ",active=" + entity.isActive();
    }

    private void audit(TenantBillingOverrideEntity entity,
                       TenantBillingOverrideAuditAction action,
                       String oldSummary,
                       String newSummary,
                       String reason,
                       UUID actorUserId) {
        tenantBillingOverrideAuditRepository.save(TenantBillingOverrideAuditEntity.builder()
                .tenant(entity.getTenant())
                .billingOverrideId(entity.getId())
                .action(action)
                .overrideType(entity.getOverrideType())
                .oldValueSummary(oldSummary)
                .newValueSummary(newSummary)
                .reason(reason)
                .actorUserId(actorUserId)
                .build());
    }

    private long resolvePlanAmountMinor(SubscriptionPlan plan) {
        if (plan == null || plan == SubscriptionPlan.FREE) {
            return 0L;
        }
        try {
            return billingCatalog.resolve(plan).getAmountMinor();
        } catch (RuntimeException ex) {
            return 0L;
        }
    }

    private String resolveCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return billingCatalog.getCurrency();
        }
        return currency.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal normalizeDiscount(BigDecimal discountPercent) {
        if (discountPercent == null) {
            return null;
        }
        return discountPercent.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }

    private TenantEntity requireTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                        "tenant.notFound",
                        "Tenant not found"
                )));
    }
}
