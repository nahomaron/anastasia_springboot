package com.anastasia.Anastasia_BackEnd.modules.registration.service.card;

import com.anastasia.Anastasia_BackEnd.common.config.PublicUrlUtils;
import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.card.MembershipCardDownloadPayload;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.card.MembershipCardSummaryResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.card.MembershipCardTemplateResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.card.MembershipCardVerifyResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.MemberStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.card.MembershipCardAuditEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.card.MembershipCardAuditEventType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.card.MembershipCardEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.card.MembershipCardStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.card.MembershipCardTemplateEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MembershipCardAuditRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MembershipCardRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MembershipCardTemplateRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MembershipCardService {

    private final MembershipCardRepository membershipCardRepository;
    private final MembershipCardAuditRepository membershipCardAuditRepository;
    private final MembershipCardTemplateRepository membershipCardTemplateRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final MembershipCardTokenService tokenService;
    private final MembershipCardStorageService storageService;
    private final MembershipCardRenderService renderService;
    private final LocalizedMessageService messageService;

    @Value("${app.public.backend-base-url:}")
    private String verifyBaseUrl;

    @Transactional
    public void issueOrRefreshForApprovedMember(Adult_MemberEntity member) {
        if (member == null || (!MemberStatus.ACTIVE.name().equals(member.getStatus())
                && !MemberStatus.APPROVED.name().equals(member.getStatus()))) {
            return;
        }
        UUID tenantId = member.getTenantId();
        if (tenantId == null) {
            return;
        }

        ensureDefaultTemplates(tenantId, member.getChurchId());
        MembershipCardTemplateEntity template = resolveDefaultTemplate(tenantId)
                .orElseThrow(() -> new IllegalStateException("No default membership card template configured"));

        MembershipCardEntity card = membershipCardRepository.findByTenantIdAndMemberId(tenantId, member.getId())
                .orElseGet(MembershipCardEntity::new);

        LocalDate issueDate = LocalDate.now();
        LocalDate expirationDate = issueDate.plusYears(5);

        card.setTenantId(tenantId);
        card.setMemberId(member.getId());
        card.setMembershipNumber(member.getMembershipNumber());
        card.setMemberFullName(fullName(member));
        card.setDateOfBirth(member.getBirthday());
        card.setChurchName(member.getChurch() != null ? member.getChurch().getChurchNameLocal() : "Church");
        card.setIssueDate(issueDate);
        card.setExpirationDate(expirationDate);
        card.setStatus(MembershipCardStatus.ACTIVE);
        card.setTemplate(template);
        card.setIssuedByUserId(currentUserId().orElse(null));
        card.setMemberAvatarUrl(member.getAvatar() != null ? member.getAvatar().getImageUrl() : null);
        card.setChurchLogoUrl(member.getChurch() != null && member.getChurch().getProfilePicture() != null
                ? member.getChurch().getProfilePicture().getImageUrl()
                : null);
        if (card.getCardSerialNumber() == null || card.getCardSerialNumber().isBlank()) {
            card.setCardSerialNumber(generateCardSerial(member.getMembershipNumber()));
        }
        if (card.getId() == null) {
            card.setQrTokenHash("pending");
            card.setQrPayloadUrl("pending");
            card.setCardImageObjectKey("pending");
            card.setCardPdfObjectKey("pending");
            card = membershipCardRepository.save(card);
        }

        String token = tokenService.generate(card.getId(), tenantId, member.getMembershipNumber(), expirationDate);
        String verifyUrl = normalizeBaseUrl(verifyBaseUrl) + "/api/v1/membership-cards/verify/" + token;
        String tokenHash = tokenService.hash(token);

        MembershipCardRenderModel renderModel = new MembershipCardRenderModel(
                card.getMemberFullName(),
                card.getDateOfBirth(),
                card.getChurchName(),
                card.getIssueDate(),
                card.getExpirationDate(),
                card.getMembershipNumber(),
                card.getCardSerialNumber(),
                verifyUrl,
                card.getMemberAvatarUrl(),
                card.getChurchLogoUrl(),
                template.getDisplayName(),
                template.getPrimaryColor(),
                template.getAccentColor(),
                template.getTextColor()
        );

        byte[] cardImage = renderService.renderCardImage(renderModel);
        byte[] cardPdf = renderService.renderCardPdf(renderModel, cardImage);

        String baseObjectKey = "membership-cards/" + tenantId + "/member-" + member.getId() + "/" + System.currentTimeMillis();
        String pngObjectKey = baseObjectKey + "/card.png";
        String pdfObjectKey = baseObjectKey + "/card.pdf";

        storageService.upload(pngObjectKey, cardImage, "image/png");
        storageService.upload(pdfObjectKey, cardPdf, "application/pdf");

        card.setQrTokenHash(tokenHash);
        card.setQrPayloadUrl(verifyUrl);
        card.setCardImageObjectKey(pngObjectKey);
        card.setCardPdfObjectKey(pdfObjectKey);
        membershipCardRepository.save(card);

        audit(card, MembershipCardAuditEventType.ISSUED,
                "Membership card issued. Template=" + template.getTemplateKey() + ", Expiration=" + expirationDate);
    }

    @Transactional(readOnly = true)
    public MembershipCardSummaryResponse getCurrentUserCardSummary() {
        Adult_MemberEntity membership = requireCurrentUserMembership();

        MembershipCardEntity card = membershipCardRepository.findByTenantIdAndMemberId(membership.getTenantId(), membership.getId())
                .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                        "registration.membershipCard.notIssued",
                        "Membership card not issued yet"
                )));

        return toSummary(card);
    }

    @Transactional
    public MembershipCardDownloadPayload downloadCurrentUserCard(String format) {
        UserEntity user = requireCurrentUser();
        Adult_MemberEntity membership = requireCurrentUserMembership(user);
        MembershipCardEntity card = membershipCardRepository.findByTenantIdAndMemberId(membership.getTenantId(), membership.getId())
                .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                        "registration.membershipCard.notIssued",
                        "Membership card not issued yet"
                )));

        return buildDownloadPayload(card, format, user.getUuid());
    }

    @Transactional
    public MembershipCardDownloadPayload downloadByMemberId(Long memberId, String format) {
        UUID tenantId = requireTenantId();
        MembershipCardEntity card = membershipCardRepository.findByTenantIdAndMemberId(tenantId, memberId)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                        "registration.membershipCard.notFound",
                        "Membership card not found"
                )));
        return buildDownloadPayload(card, format, currentUserId().orElse(null));
    }

    @Transactional(readOnly = true)
    public List<MembershipCardTemplateResponse> listTemplatesForCurrentTenant() {
        UUID tenantId = requireTenantId();
        ensureDefaultTemplates(tenantId, null);
        return membershipCardTemplateRepository.findByTenantIdAndActiveTrueOrderBySortOrderAscDisplayNameAsc(tenantId)
                .stream()
                .map(t -> MembershipCardTemplateResponse.builder()
                        .id(t.getId())
                        .templateKey(t.getTemplateKey())
                        .displayName(t.getDisplayName())
                        .primaryColor(t.getPrimaryColor())
                        .accentColor(t.getAccentColor())
                        .textColor(t.getTextColor())
                        .backgroundImageUrl(t.getBackgroundImageUrl())
                        .isDefault(t.isDefault())
                        .builtIn(t.isBuiltIn())
                        .sortOrder(t.getSortOrder())
                        .build())
                .toList();
    }

    @Transactional
    public MembershipCardTemplateResponse setDefaultTemplateForCurrentTenant(Long templateId) {
        UUID tenantId = requireTenantId();
        MembershipCardTemplateEntity template = membershipCardTemplateRepository.findByIdAndTenantId(templateId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                        "registration.membershipCard.template.notFound",
                        "Template not found"
                )));

        List<MembershipCardTemplateEntity> templates = membershipCardTemplateRepository
                .findByTenantIdAndActiveTrueOrderBySortOrderAscDisplayNameAsc(tenantId);
        for (MembershipCardTemplateEntity candidate : templates) {
            candidate.setDefault(candidate.getId().equals(template.getId()));
        }
        membershipCardTemplateRepository.saveAll(templates);

        return MembershipCardTemplateResponse.builder()
                .id(template.getId())
                .templateKey(template.getTemplateKey())
                .displayName(template.getDisplayName())
                .primaryColor(template.getPrimaryColor())
                .accentColor(template.getAccentColor())
                .textColor(template.getTextColor())
                .backgroundImageUrl(template.getBackgroundImageUrl())
                .isDefault(true)
                .builtIn(template.isBuiltIn())
                .sortOrder(template.getSortOrder())
                .build();
    }

    @Transactional
    public void revokeCardForMember(Long memberId, String reason) {
        UUID tenantId = requireTenantId();
        membershipCardRepository.findByTenantIdAndMemberId(tenantId, memberId)
                .ifPresent(card -> revokeCard(card, reason));
    }

    @Transactional
    public void revokeCardByMembershipNumber(UUID tenantId, String membershipNumber, String reason) {
        membershipCardRepository.findByTenantIdAndMembershipNumber(tenantId, membershipNumber)
                .ifPresent(card -> revokeCard(card, reason));
    }

    @Transactional
    public MembershipCardVerifyResponse verifyToken(String token, HttpServletRequest request) {
        MembershipCardTokenService.ParsedMembershipCardToken parsed = tokenService.parse(token);

        MembershipCardEntity card = membershipCardRepository.findById(parsed.cardId())
                .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                        "registration.membershipCard.card.notFound",
                        "Card not found"
                )));

        boolean hashMatches = tokenService.hash(token).equals(card.getQrTokenHash());
        boolean tenantMatches = parsed.tenantId().equals(card.getTenantId());
        boolean memberMatches = parsed.membershipNumber().equals(card.getMembershipNumber());

        if (!hashMatches || !tenantMatches || !memberMatches) {
            audit(card, MembershipCardAuditEventType.VERIFIED,
                    buildVerificationAuditDetails(false, "token-mismatch", request, card));
            return MembershipCardVerifyResponse.builder()
                    .valid(false)
                    .build();
        }

        if (card.getStatus() == MembershipCardStatus.ACTIVE && LocalDate.now().isAfter(card.getExpirationDate())) {
            card.setStatus(MembershipCardStatus.EXPIRED);
            membershipCardRepository.save(card);
        }

        boolean valid = card.getStatus() == MembershipCardStatus.ACTIVE
                && !LocalDate.now().isAfter(card.getExpirationDate());

        Adult_MemberEntity member = memberRepository.findByIdAndTenantId(card.getMemberId(), card.getTenantId())
                .orElse(null);
        ChurchEntity church = member != null ? member.getChurch() : null;

        audit(card, MembershipCardAuditEventType.VERIFIED,
                buildVerificationAuditDetails(valid, valid ? "verified" : "inactive-or-expired", request, card));

        return MembershipCardVerifyResponse.builder()
                .valid(valid)
                .churchName(card.getChurchName())
                .diocese(trimToNull(church != null ? church.getDiocese() : null))
                .expirationDate(card.getExpirationDate())
                .maskedMemberLabel(maskMemberLabel(card))
                .build();
    }

    private MembershipCardDownloadPayload buildDownloadPayload(MembershipCardEntity card, String format, UUID actorUserId) {
        if (card.getStatus() == MembershipCardStatus.ACTIVE && LocalDate.now().isAfter(card.getExpirationDate())) {
            card.setStatus(MembershipCardStatus.EXPIRED);
        }

        String normalized = normalizeFormat(format);
        String objectKey = "png".equals(normalized)
                ? card.getCardImageObjectKey()
                : card.getCardPdfObjectKey();
        String contentType = "png".equals(normalized) ? "image/png" : "application/pdf";
        String extension = "png".equals(normalized) ? ".png" : ".pdf";

        byte[] content = storageService.read(objectKey);

        card.setDownloadedCount(card.getDownloadedCount() + 1);
        card.setLastDownloadedAt(Instant.now());
        membershipCardRepository.save(card);

        audit(card, MembershipCardAuditEventType.DOWNLOADED,
                "Downloaded format=" + normalized + ", by=" + actorUserId);

        return new MembershipCardDownloadPayload(
                content,
                "membership-card-" + card.getMembershipNumber() + extension,
                contentType
        );
    }

    private MembershipCardSummaryResponse toSummary(MembershipCardEntity card) {
        String templateKey = card.getTemplate() != null ? card.getTemplate().getTemplateKey() : null;
        String templateDisplayName = card.getTemplate() != null ? card.getTemplate().getDisplayName() : null;

        return MembershipCardSummaryResponse.builder()
                .cardId(card.getId())
                .membershipNumber(card.getMembershipNumber())
                .memberFullName(card.getMemberFullName())
                .churchName(card.getChurchName())
                .issueDate(card.getIssueDate())
                .expirationDate(card.getExpirationDate())
                .cardSerialNumber(card.getCardSerialNumber())
                .status(card.getStatus())
                .templateKey(templateKey)
                .templateDisplayName(templateDisplayName)
                .memberAvatarUrl(card.getMemberAvatarUrl())
                .churchLogoUrl(card.getChurchLogoUrl())
                .build();
    }

    private void revokeCard(MembershipCardEntity card, String reason) {
        card.setStatus(MembershipCardStatus.REVOKED);
        membershipCardRepository.save(card);
        audit(card, MembershipCardAuditEventType.REVOKED, "Reason=" + (reason == null ? "N/A" : reason));
    }

    private void audit(MembershipCardEntity card, MembershipCardAuditEventType type, String details) {
        MembershipCardAuditEntity audit = MembershipCardAuditEntity.builder()
                .tenantId(card.getTenantId())
                .membershipCard(card)
                .eventType(type)
                .actorUserId(currentUserId().orElse(null))
                .details(details)
                .build();
        membershipCardAuditRepository.save(audit);
    }

    private String buildVerificationAuditDetails(
            boolean valid,
            String outcome,
            HttpServletRequest request,
            MembershipCardEntity card
    ) {
        List<String> parts = new ArrayList<>();
        parts.add("result=" + valid);
        parts.add("outcome=" + outcome);
        parts.add("cardStatus=" + card.getStatus());
        parts.add("expiresOn=" + card.getExpirationDate());
        parts.add("clientIp=" + safeAuditValue(extractClientIp(request), 96));
        parts.add("userAgent=" + safeAuditValue(request != null ? request.getHeader("User-Agent") : null, 256));
        parts.add("forwardedFor=" + safeAuditValue(request != null ? request.getHeader("X-Forwarded-For") : null, 256));
        return String.join(", ", parts);
    }

    private String maskMemberLabel(MembershipCardEntity card) {
        String name = trimToNull(card.getMemberFullName());
        String membershipNumber = trimToNull(card.getMembershipNumber());

        if (name == null && membershipNumber == null) {
            return null;
        }

        String maskedName = name == null ? null : maskName(name);
        String maskedMembershipNumber = membershipNumber == null ? null : maskMembershipNumber(membershipNumber);

        if (maskedName == null) {
            return maskedMembershipNumber;
        }
        if (maskedMembershipNumber == null) {
            return maskedName;
        }
        return maskedName + " • " + maskedMembershipNumber;
    }

    private String maskName(String fullName) {
        String[] parts = fullName.trim().split("\\s+");
        List<String> maskedParts = new ArrayList<>();
        for (String part : parts) {
            String maskedPart = maskNamePart(part);
            if (!maskedPart.isBlank()) {
                maskedParts.add(maskedPart);
            }
        }
        return maskedParts.isEmpty() ? null : String.join(" ", maskedParts);
    }

    private String maskNamePart(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() == 1) {
            return trimmed;
        }
        return trimmed.charAt(0) + "*";
    }

    private String maskMembershipNumber(String membershipNumber) {
        String trimmed = membershipNumber.trim();
        if (trimmed.length() <= 4) {
            return trimmed;
        }
        return "****" + trimmed.substring(trimmed.length() - 4);
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return "n/a";
        }
        String forwardedFor = trimToNull(request.getHeader("X-Forwarded-For"));
        if (forwardedFor != null) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = trimToNull(request.getHeader("X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }
        return request.getRemoteAddr();
    }

    private String safeAuditValue(String value, int maxLength) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return "n/a";
        }
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void ensureDefaultTemplates(UUID tenantId, Long churchId) {
        if (membershipCardTemplateRepository.existsByTenantId(tenantId)) {
            return;
        }

        MembershipCardTemplateEntity t1 = MembershipCardTemplateEntity.builder()
                .tenantId(tenantId)
                .churchId(churchId)
                .templateKey("classic-gold")
                .displayName("Classic Gold")
                .primaryColor("#183661")
                .accentColor("#C49B3F")
                .textColor("#F8F8F8")
                .active(true)
                .isDefault(true)
                .builtIn(true)
                .sortOrder(1)
                .build();

        MembershipCardTemplateEntity t2 = MembershipCardTemplateEntity.builder()
                .tenantId(tenantId)
                .churchId(churchId)
                .templateKey("royal-blue")
                .displayName("Royal Blue")
                .primaryColor("#0F3D6E")
                .accentColor("#1F78B4")
                .textColor("#F4FAFF")
                .active(true)
                .isDefault(false)
                .builtIn(true)
                .sortOrder(2)
                .build();

        MembershipCardTemplateEntity t3 = MembershipCardTemplateEntity.builder()
                .tenantId(tenantId)
                .churchId(churchId)
                .templateKey("emerald-light")
                .displayName("Emerald Light")
                .primaryColor("#1D5C4A")
                .accentColor("#3E9E73")
                .textColor("#F3FFF8")
                .active(true)
                .isDefault(false)
                .builtIn(true)
                .sortOrder(3)
                .build();

        membershipCardTemplateRepository.saveAll(List.of(t1, t2, t3));
    }

    private Optional<MembershipCardTemplateEntity> resolveDefaultTemplate(UUID tenantId) {
        return membershipCardTemplateRepository.findByTenantIdAndIsDefaultTrue(tenantId);
    }

    private String generateCardSerial(String membershipNumber) {
        return "CARD-" + membershipNumber + "-" + System.currentTimeMillis();
    }

    private String normalizeBaseUrl(String rawUrl) {
        return PublicUrlUtils.normalizeBaseUrl(rawUrl, "app.public.backend-base-url");
    }

    private String normalizeFormat(String format) {
        if (format == null || format.isBlank()) {
            return "pdf";
        }
        String normalized = format.toLowerCase(Locale.ROOT);
        return "png".equals(normalized) ? "png" : "pdf";
    }

    private String fullName(Adult_MemberEntity member) {
        String first = member.getFirstName() == null ? "" : member.getFirstName();
        String father = member.getFatherName() == null ? "" : member.getFatherName();
        String grand = member.getGrandFatherName() == null ? "" : member.getGrandFatherName();
        return (first + " " + father + " " + grand).trim();
    }

    private UUID requireTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is required for membership card operations");
        }
        return tenantId;
    }

    private UserEntity requireCurrentUser() {
        UUID userId = currentUserId().orElseThrow(() -> new IllegalStateException("No authenticated user"));
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    private Adult_MemberEntity requireCurrentUserMembership() {
        return requireCurrentUserMembership(requireCurrentUser());
    }

    private Adult_MemberEntity requireCurrentUserMembership(UserEntity user) {
        UUID tenantId = requireTenantId();
        Adult_MemberEntity membership = user.getMembership();
        if (membership == null || membership.getId() == null || membership.getTenantId() == null) {
            throw new EntityNotFoundException(messageService.get(
                    "registration.membership.currentUser.missing",
                    "No membership associated with current user"
            ));
        }
        if (!tenantId.equals(membership.getTenantId())) {
            throw new org.springframework.security.access.AccessDeniedException(messageService.get(
                    "registration.membership.currentUser.tenantMismatch",
                    "Current membership is not in the active tenant"
            ));
        }
        return membership;
    }

    private Optional<UUID> currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.ofNullable(principal.getUserUuid());
    }
}
