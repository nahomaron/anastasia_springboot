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
    private String memberId;
    private String memberName;
    private String memberEmail;
    private String fundId;
    private String fundName;
    private Instant createdAt;

    public static PaymentView fromEntity(PaymentIntent p) {
        var v = new PaymentView();
        v.setId(p.getId());
        v.setPurpose(p.getPurpose().name());
        v.setAmount(p.getAmount().getAmount());
        v.setCurrency(p.getAmount().getCurrency());
        v.setStatus(p.getStatus().name());
        v.setMemberId(p.getMemberId());
//        v.setMemberName(p.getMemberName());
//        v.setMemberEmail(p.getMemberEmail());
        v.setFundId(p.getFundId());
//        v.setFundName(p.getFundName());
        v.setCreatedAt(p.getCreatedAt());
        return v;
    }
}
