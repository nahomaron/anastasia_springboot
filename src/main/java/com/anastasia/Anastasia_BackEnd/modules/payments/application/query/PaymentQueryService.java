package com.anastasia.Anastasia_BackEnd.modules.payments.application.query;

import com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.repository.FundRepository;
import com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.repository.PaymentIntentRepository;
import com.anastasia.Anastasia_BackEnd.modules.payments.web.dto.PaymentView;
import com.anastasia.Anastasia_BackEnd.repository.registration.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentQueryService {

    private final PaymentIntentRepository paymentRepo;
    private final MemberRepository memberRepo;
    private final FundRepository fundRepo;


//    public Page<PaymentView> findAll(String tenantId, Pageable pageable) {
//        return paymentRepo.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable)
//                .map(PaymentView::fromEntity);
//    }

    public Page<PaymentView> findAll(String tenantId, Pageable pageable) {
        var page = paymentRepo.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);

        return page.map(pi -> {
            var view = PaymentView.fromEntity(pi);
            enrichWithMemberAndFund(view, tenantId);
            return view;
        });
    }

    public PaymentView findById(String tenantId, UUID id) {
        var entity = paymentRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + id));
        return PaymentView.fromEntity(entity);
    }

    private void enrichWithMemberAndFund(PaymentView v, String tenantId) {
        if (v.getMemberId() != null) {
            memberRepo.findByMembershipIdAndTenantId(UUID.fromString(v.getMemberId()), tenantId)
                    .ifPresent(m -> {
                        v.setMemberName(m.getFirstName() + " " + m.getFatherName() + " " + m.getGrandFatherName());
                        v.setMemberEmail(m.getEmail());
                    });
        }
        if (v.getFundId() != null) {
            fundRepo.findByIdAndTenantId(UUID.fromString(v.getFundId()), tenantId)
                    .ifPresent(f -> v.setFundName(f.getName()));
        }
    }

    public List<Map<String, Object>> totalCapturedByFund(String tenantId) {
        return paymentRepo.totalCapturedByFund(tenantId);
    }


//    public long countAll(String tenantId) {
//        return paymentRepo.countByTenantId(tenantId);
//    }
}
