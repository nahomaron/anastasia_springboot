package com.anastasia.Anastasia_BackEnd.modules.payments.web.dto;

import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentSubscription;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class SubscriptionView {
    private UUID id;
    private String purpose;
    private long amount;
    private String currency;
    private String status;
    private String memberId;
    private UUID userId;
    private String userEmail;
    private String fundId;
    private String provider;
    private String providerSubscriptionReference;
    private String providerCheckoutReference;
    private Instant createdAt;
    private Instant activatedAt;
    private Instant canceledAt;
    private Instant statusChangedAt;

    public static SubscriptionView fromEntity(PaymentSubscription s) {
        var v = new SubscriptionView();
        v.setId(s.getId());
        v.setPurpose(s.getPurpose().name());
        v.setAmount(s.getAmount().getAmount());
        v.setCurrency(s.getAmount().getCurrency());
        v.setStatus(s.getStatus().name());
        v.setMemberId(s.getMemberId());
        v.setUserId(s.getUserId());
        v.setUserEmail(s.getUserEmail());
        v.setFundId(s.getFundId());
        v.setProvider(s.getProvider());
        v.setProviderSubscriptionReference(s.getProviderSubscriptionReference());
        v.setProviderCheckoutReference(s.getProviderCheckoutReference());
        v.setCreatedAt(s.getCreatedAt());
        v.setActivatedAt(s.getActivatedAt());
        v.setCanceledAt(s.getCanceledAt());
        v.setStatusChangedAt(s.getStatusChangedAt());
        return v;
    }
}
