package com.anastasia.Anastasia_BackEnd.modules.payments.application.query;

import com.anastasia.Anastasia_BackEnd.modules.accounting.repository.FundRepository;
import com.anastasia.Anastasia_BackEnd.modules.payments.repository.PaymentIntentRepository;
import com.anastasia.Anastasia_BackEnd.modules.payments.web.dto.PaymentView;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for querying payment intents.
 * Provides methods to retrieve payment intents with pagination,
 * find by ID, and aggregate total captured amounts by fund.
 * Also enriches payment views with member and fund details.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentQueryService {

    private final PaymentIntentRepository paymentRepo;
    private final MemberRepository memberRepo;
    private final FundRepository fundRepo;


//    public Page<PaymentView> findAll(String tenantId, Pageable pageable) {
//        return paymentRepo.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable)
//                .map(PaymentView::fromEntity);
//    }

    public Page<PaymentView> findAll(UUID tenantId, Pageable pageable) {
        var page = paymentRepo.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);

        return page.map(pi -> {
            var view = PaymentView.fromEntity(pi);
            enrichWithMemberAndFund(view, tenantId);
            return view;
        });
    }

    public PaymentView findById(UUID tenantId, UUID id) {
        var entity = paymentRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + id));
        return PaymentView.fromEntity(entity);
    }

    // Enriches the PaymentView with member name, email, and fund name if available
    private void enrichWithMemberAndFund(PaymentView v, UUID tenantId) {
        if (tenantId == null) {
            return;
        }
        if (v.getMemberId() != null) {
            memberRepo.findByIdAndTenantId(v.getMemberId(), tenantId)
                    .ifPresent(m -> {
                        v.setMemberName(m.getFirstName() + " " + m.getFatherName() + " " + m.getGrandFatherName());
                        v.setMemberEmail(m.getEmail());
                        v.setUserId(m.getUserId());
                    });
        } else if (v.getUserId() != null) {
            memberRepo.findByUserIdAndTenantId(v.getUserId(), tenantId)
                    .ifPresent(m -> {
                        v.setMemberId(m.getId());
                        v.setMemberName(m.getFirstName() + " " + m.getFatherName() + " " + m.getGrandFatherName());
                        v.setMemberEmail(m.getEmail());
                    });
        }
        if (v.getFundId() != null) {
            try {
                Long fundId = Long.valueOf(v.getFundId());
                fundRepo.findByIdAndTenantId(fundId, tenantId.toString())
                        .ifPresent(f -> v.setFundName(f.getName()));
            } catch (NumberFormatException ex) {
                log.debug("Unable to parse fundId {} as Long for tenant {}", v.getFundId(), tenantId, ex);
            }
        }
    }

    public List<Map<String, Object>> totalCapturedByFund(UUID tenantId) {
        return paymentRepo.totalCapturedByFund(tenantId);
    }


//    public long countAll(String tenantId) {
//        return paymentRepo.countByTenantId(tenantId);
//    }
}
