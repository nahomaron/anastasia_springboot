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
    private String fundId;
    private Instant createdAt;

    public static SubscriptionView fromEntity(PaymentSubscription s) {
        var v = new SubscriptionView();
        v.setId(s.getId());
        v.setPurpose(s.getPurpose().name());
        v.setAmount(s.getAmount().getAmount());
        v.setCurrency(s.getAmount().getCurrency());
        v.setStatus(s.getStatus().name());
        v.setMemberId(s.getMemberId());
        v.setFundId(s.getFundId());
        v.setCreatedAt(s.getCreatedAt());
        return v;
    }
}
