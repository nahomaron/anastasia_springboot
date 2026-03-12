package com.anastasia.Anastasia_BackEnd.modules.payments.web.dto;

import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentIntent;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class PaymentView {
    private UUID id;
    private String purpose;
    private long amount;
    private String currency;
    private String status;
    private Long memberId;
    private UUID userId;
    private String memberName;
    private String memberEmail;
    private String fundId;
    private String fundName;
    private String provider;
    private String providerPaymentReference;
    private String providerCheckoutReference;
    private Instant createdAt;
    private Instant authorizedAt;
    private Instant capturedAt;
    private Instant failedAt;
    private Instant refundedAt;
    private Instant statusChangedAt;

    public static PaymentView fromEntity(PaymentIntent p) {
        var v = new PaymentView();
        v.setId(p.getId());
        v.setPurpose(p.getPurpose().name());
        v.setAmount(p.getAmount().getAmount());
        v.setCurrency(p.getAmount().getCurrency());
        v.setStatus(p.getStatus().name());
        v.setMemberId(p.getMemberId());
        v.setUserId(p.getUserId());
        v.setFundId(p.getFundId());
        v.setFundName(p.getFundName());
        v.setProvider(p.getProvider());
        v.setProviderPaymentReference(p.getProviderPaymentReference());
        v.setProviderCheckoutReference(p.getProviderCheckoutReference());
        v.setCreatedAt(p.getCreatedAt());
        v.setAuthorizedAt(p.getAuthorizedAt());
        v.setCapturedAt(p.getCapturedAt());
        v.setFailedAt(p.getFailedAt());
        v.setRefundedAt(p.getRefundedAt());
        v.setStatusChangedAt(p.getStatusChangedAt());
        return v;
    }
}
