package com.anastasia.Anastasia_BackEnd.modules.services.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.notification.service.TenantAdminNotificationService;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.services.dto.BaptismLanguageDetailsRequest;
import com.anastasia.Anastasia_BackEnd.modules.services.dto.BaptismServiceRequestCreateRequest;
import com.anastasia.Anastasia_BackEnd.modules.services.dto.BaptismServiceRequestResponse;
import com.anastasia.Anastasia_BackEnd.modules.services.dto.MemberServiceRequestListItemResponse;
import com.anastasia.Anastasia_BackEnd.modules.services.model.BaptismLanguageDetails;
import com.anastasia.Anastasia_BackEnd.modules.services.model.BaptismRequestEntity;
import com.anastasia.Anastasia_BackEnd.modules.services.model.BaptismRequestStatus;
import com.anastasia.Anastasia_BackEnd.modules.services.repository.BaptismRequestRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BaptismRequestServiceImpl implements BaptismRequestService {

    private final BaptismRequestRepository baptismRequestRepository;
    private final ChurchRepository churchRepository;
    private final UserRepository userRepository;
    private final TenantAdminNotificationService tenantAdminNotificationService;

    @Override
    @Transactional
    public BaptismServiceRequestResponse create(BaptismServiceRequestCreateRequest request) {
        UserEntity currentUser = getCurrentAuthenticatedUser();
        validateRequestDates(request);

        String normalizedChurchNumber = normalizeChurchNumber(request.churchNumber());
        ChurchEntity church = churchRepository.findByChurchNumber(normalizedChurchNumber)
                .orElseThrow(() -> new IllegalStateException("Church not found for number: " + normalizedChurchNumber));

        UUID tenantId = church.getTenant() != null ? church.getTenant().getId() : TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is required to submit a baptism request");
        }

        BaptismRequestEntity entity = BaptismRequestEntity.builder()
                .requestNumber(generateRequestNumber())
                .tenantId(tenantId)
                .church(church)
                .churchNumber(normalizedChurchNumber)
                .requestedByUser(currentUser)
                .status(BaptismRequestStatus.PENDING)
                .birthDate(request.birthDate())
                .baptismDate(request.baptismDate())
                .localLanguage(toEmbeddable(request.localLanguage()))
                .english(toEmbeddable(request.english()))
                .babyPhotoUrl(request.babyPhoto().imageUrl().trim())
                .babyPhotoSize(trimToNull(request.babyPhoto().imageSize()))
                .birthCertificateUrl(request.birthCertificate().imageUrl().trim())
                .birthCertificateSize(trimToNull(request.birthCertificate().imageSize()))
                .fatherSignatureUrl(request.fatherSignature().imageUrl().trim())
                .fatherSignatureSize(trimToNull(request.fatherSignature().imageSize()))
                .priestSignatureUrl(request.priestSignature().imageUrl().trim())
                .priestSignatureSize(trimToNull(request.priestSignature().imageSize()))
                .build();

        BaptismRequestEntity saved = baptismRequestRepository.save(entity);
        tenantAdminNotificationService.notifyBaptismRequestSubmitted(saved, currentUser.getUuid());

        return new BaptismServiceRequestResponse(
                saved.getId(),
                saved.getRequestNumber(),
                saved.getStatus(),
                saved.getCreatedAt() != null ? saved.getCreatedAt() : Instant.now()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberServiceRequestListItemResponse> listMine() {
        UserEntity currentUser = getCurrentAuthenticatedUser();

        return baptismRequestRepository.findByRequestedByUser_UuidOrderByCreatedAtDesc(currentUser.getUuid())
                .stream()
                .map(request -> new MemberServiceRequestListItemResponse(
                        request.getId(),
                        request.getRequestNumber(),
                        "BAPTISM",
                        request.getStatus(),
                        request.getCreatedAt(),
                        request.getReviewedAt(),
                        request.getEnglish() != null ? request.getEnglish().getFullName() : null,
                        request.getChurch() != null ? request.getChurch().getChurchName() : request.getChurchNumber(),
                        request.getChurchNumber()
                ))
                .toList();
    }

    private BaptismLanguageDetails toEmbeddable(BaptismLanguageDetailsRequest request) {
        return BaptismLanguageDetails.builder()
                .fullName(request.fullName().trim())
                .baptismalName(request.baptismalName().trim())
                .fatherFullName(request.fatherFullName().trim())
                .motherFullName(request.motherFullName().trim())
                .godParentFullName(request.godParentFullName().trim())
                .priestFullName(request.priestFullName().trim())
                .churchOfBaptismName(request.churchOfBaptismName().trim())
                .build();
    }

    private UserEntity getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalStateException("User not authenticated");
        }

        return userRepository.findById(principal.getUserUuid())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    private String normalizeChurchNumber(String rawChurchNumber) {
        if (!StringUtils.hasText(rawChurchNumber)) {
            return rawChurchNumber;
        }
        return rawChurchNumber.replace("\"", "").trim();
    }

    private String generateRequestNumber() {
        String prefix = "BAP";
        String candidate;
        do {
            candidate = prefix + "-" + UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 10)
                    .toUpperCase(Locale.ROOT);
        } while (baptismRequestRepository.existsByRequestNumber(candidate));

        return candidate;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void validateRequestDates(BaptismServiceRequestCreateRequest request) {
        if (request.birthDate() != null
                && request.baptismDate() != null
                && request.baptismDate().isBefore(request.birthDate())) {
            throw new IllegalArgumentException("Baptism date cannot be earlier than birth date");
        }
    }
}
