package com.anastasia.Anastasia_BackEnd.modules.registration.service.card;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.card.MembershipCardDownloadPayload;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.card.MembershipCardSummaryResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.card.MembershipCardTemplateResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.card.MembershipCardVerifyResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.MemberStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.card.MembershipCardAuditEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.card.MembershipCardAuditEventType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.card.MembershipCardEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.card.MembershipCardStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.card.MembershipCardTemplateEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MembershipCardAuditRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MembershipCardRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MembershipCardTemplateRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MembershipCardService {

    private final MembershipCardRepository membershipCardRepository;
    private final MembershipCardAuditRepository membershipCardAuditRepository;
    private final MembershipCardTemplateRepository membershipCardTemplateRepository;
    private final UserRepository userRepository;
    private final MembershipCardTokenService tokenService;
    private final MembershipCardStorageService storageService;
    private final MembershipCardRenderService renderService;

    @Value("${app.membership-card.verify-base-url:http://localhost:8080}")
    private String verifyBaseUrl;

    @Transactional
    public void issueOrRefreshForApprovedMember(Adult_MemberEntity member) {
        if (member == null || !MemberStatus.ACTIVE.name().equals(member.getStatus())) {
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
        card.setChurchName(member.getChurch() != null ? member.getChurch().getChurchName() : "Church");
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
        UserEntity user = requireCurrentUser();
        Adult_MemberEntity membership = user.getMembership();
        if (membership == null || membership.getId() == null || membership.getTenantId() == null) {
            throw new EntityNotFoundException("No membership associated with current user");
        }

        MembershipCardEntity card = membershipCardRepository.findByTenantIdAndMemberId(membership.getTenantId(), membership.getId())
                .orElseThrow(() -> new EntityNotFoundException("Membership card not issued yet"));

        return toSummary(card);
    }

    @Transactional
    public MembershipCardDownloadPayload downloadCurrentUserCard(String format) {
        UserEntity user = requireCurrentUser();
        Adult_MemberEntity membership = user.getMembership();
        if (membership == null || membership.getId() == null || membership.getTenantId() == null) {
            throw new EntityNotFoundException("No membership associated with current user");
        }
        MembershipCardEntity card = membershipCardRepository.findByTenantIdAndMemberId(membership.getTenantId(), membership.getId())
                .orElseThrow(() -> new EntityNotFoundException("Membership card not issued yet"));

        return buildDownloadPayload(card, format, user.getUuid());
    }

    @Transactional
    public MembershipCardDownloadPayload downloadByMemberId(Long memberId, String format) {
        UUID tenantId = requireTenantId();
        MembershipCardEntity card = membershipCardRepository.findByTenantIdAndMemberId(tenantId, memberId)
                .orElseThrow(() -> new EntityNotFoundException("Membership card not found"));
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
                .orElseThrow(() -> new EntityNotFoundException("Template not found"));

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
    public MembershipCardVerifyResponse verifyToken(String token) {
        MembershipCardTokenService.ParsedMembershipCardToken parsed = tokenService.parse(token);

        MembershipCardEntity card = membershipCardRepository.findById(parsed.cardId())
                .orElseThrow(() -> new EntityNotFoundException("Card not found"));

        boolean hashMatches = tokenService.hash(token).equals(card.getQrTokenHash());
        boolean tenantMatches = parsed.tenantId().equals(card.getTenantId());
        boolean memberMatches = parsed.membershipNumber().equals(card.getMembershipNumber());

        if (!hashMatches || !tenantMatches || !memberMatches) {
            return MembershipCardVerifyResponse.builder()
                    .valid(false)
                    .message("Invalid card token")
                    .build();
        }

        if (card.getStatus() == MembershipCardStatus.ACTIVE && LocalDate.now().isAfter(card.getExpirationDate())) {
            card.setStatus(MembershipCardStatus.EXPIRED);
            membershipCardRepository.save(card);
        }

        boolean valid = card.getStatus() == MembershipCardStatus.ACTIVE
                && !LocalDate.now().isAfter(card.getExpirationDate());

        audit(card, MembershipCardAuditEventType.VERIFIED,
                "Verification result=" + valid + " at " + LocalDateTime.now());

        return MembershipCardVerifyResponse.builder()
                .valid(valid)
                .message(valid ? "Card is valid" : "Card is not valid")
                .memberFullName(card.getMemberFullName())
                .churchName(card.getChurchName())
                .membershipNumber(card.getMembershipNumber())
                .issueDate(card.getIssueDate())
                .expirationDate(card.getExpirationDate())
                .status(card.getStatus())
                .cardSerialNumber(card.getCardSerialNumber())
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
        card.setLastDownloadedAt(LocalDateTime.now());
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
        if (rawUrl == null || rawUrl.isBlank()) {
            return "http://localhost:8080";
        }
        return rawUrl.endsWith("/") ? rawUrl.substring(0, rawUrl.length() - 1) : rawUrl;
    }

    private String normalizeFormat(String format) {
        if (format == null || format.isBlank()) {
            return "pdf";
        }
        String normalized = format.toLowerCase();
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

    private Optional<UUID> currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.ofNullable(principal.getUserUuid());
    }
}
