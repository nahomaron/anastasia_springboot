package com.anastasia.Anastasia_BackEnd.modules.services.marriage.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.notification.service.TenantAdminNotificationService;
import com.anastasia.Anastasia_BackEnd.modules.calendar.dto.CalendarEntryRequest;
import com.anastasia.Anastasia_BackEnd.modules.calendar.dto.CalendarEntryResponse;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarCategory;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryEntity;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryStatus;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryType;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarSystem;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarVisibility;
import com.anastasia.Anastasia_BackEnd.modules.calendar.repository.CalendarEntryRepository;
import com.anastasia.Anastasia_BackEnd.modules.calendar.service.CalendarEntryService;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto.*;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.*;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository.*;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.support.MarriageCaseReferenceGenerator;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.support.MarriageSecuritySupport;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.validation.MarriageCaseDomainValidator;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.SimpleUserDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.math.BigDecimal;

@Service
public class MarriageCaseServiceImpl implements MarriageCaseService {

    private final MarriageCaseRepository marriageCaseRepository;
    private final MarriagePartyRepository marriagePartyRepository;
    private final MarriagePartySubmissionRepository marriagePartySubmissionRepository;
    private final MarriagePartyDocumentRepository marriagePartyDocumentRepository;
    private final MarriageRequirementTemplateRepository marriageRequirementTemplateRepository;
    private final MarriageRequirementAssignmentRepository marriageRequirementAssignmentRepository;
    private final MarriagePairingTokenRepository marriagePairingTokenRepository;
    private final MarriageStatusHistoryRepository marriageStatusHistoryRepository;
    private final MarriageAuditEventRepository marriageAuditEventRepository;
    private final MarriageReviewRepository marriageReviewRepository;
    private final MarriageCaseNoteRepository marriageCaseNoteRepository;
    private final MarriageConfessorApprovalRepository marriageConfessorApprovalRepository;
    private final MarriageImpedimentRepository marriageImpedimentRepository;
    private final MarriageManualPaymentRepository marriageManualPaymentRepository;
    private final MarriageWitnessRepository marriageWitnessRepository;
    private final MarriagePriestAssignmentRepository marriagePriestAssignmentRepository;
    private final MarriageScheduleRepository marriageScheduleRepository;
    private final MarriageCertificateRepository marriageCertificateRepository;
    private final MarriageCertificateSequenceConfigRepository marriageCertificateSequenceConfigRepository;
    private final MarriageCertificateAmendmentRepository marriageCertificateAmendmentRepository;
    private final MarriageSecuritySupport marriageSecuritySupport;
    private final MarriageCaseReferenceGenerator marriageCaseReferenceGenerator;
    private final MarriageCaseDomainValidator marriageCaseDomainValidator;
    private final ChurchRepository churchRepository;
    private final UserRepository userRepository;
    private final CalendarEntryService calendarEntryService;
    private final CalendarEntryRepository calendarEntryRepository;
    private final ObjectMapper objectMapper;
    private final TenantAdminNotificationService tenantAdminNotificationService;

    public MarriageCaseServiceImpl(
            MarriageCaseRepository marriageCaseRepository,
            MarriagePartyRepository marriagePartyRepository,
            MarriagePartySubmissionRepository marriagePartySubmissionRepository,
            MarriagePartyDocumentRepository marriagePartyDocumentRepository,
            MarriageRequirementTemplateRepository marriageRequirementTemplateRepository,
            MarriageRequirementAssignmentRepository marriageRequirementAssignmentRepository,
            MarriagePairingTokenRepository marriagePairingTokenRepository,
            MarriageStatusHistoryRepository marriageStatusHistoryRepository,
            MarriageAuditEventRepository marriageAuditEventRepository,
            MarriageReviewRepository marriageReviewRepository,
            MarriageCaseNoteRepository marriageCaseNoteRepository,
            MarriageConfessorApprovalRepository marriageConfessorApprovalRepository,
            MarriageImpedimentRepository marriageImpedimentRepository,
            MarriageManualPaymentRepository marriageManualPaymentRepository,
            MarriageWitnessRepository marriageWitnessRepository,
            MarriagePriestAssignmentRepository marriagePriestAssignmentRepository,
            MarriageScheduleRepository marriageScheduleRepository,
            MarriageCertificateRepository marriageCertificateRepository,
            MarriageCertificateSequenceConfigRepository marriageCertificateSequenceConfigRepository,
            MarriageCertificateAmendmentRepository marriageCertificateAmendmentRepository,
            MarriageSecuritySupport marriageSecuritySupport,
            MarriageCaseReferenceGenerator marriageCaseReferenceGenerator,
            MarriageCaseDomainValidator marriageCaseDomainValidator,
            ChurchRepository churchRepository,
            UserRepository userRepository,
            CalendarEntryService calendarEntryService,
            CalendarEntryRepository calendarEntryRepository,
            ObjectMapper objectMapper,
            TenantAdminNotificationService tenantAdminNotificationService
    ) {
        this.marriageCaseRepository = marriageCaseRepository;
        this.marriagePartyRepository = marriagePartyRepository;
        this.marriagePartySubmissionRepository = marriagePartySubmissionRepository;
        this.marriagePartyDocumentRepository = marriagePartyDocumentRepository;
        this.marriageRequirementTemplateRepository = marriageRequirementTemplateRepository;
        this.marriageRequirementAssignmentRepository = marriageRequirementAssignmentRepository;
        this.marriagePairingTokenRepository = marriagePairingTokenRepository;
        this.marriageStatusHistoryRepository = marriageStatusHistoryRepository;
        this.marriageAuditEventRepository = marriageAuditEventRepository;
        this.marriageReviewRepository = marriageReviewRepository;
        this.marriageCaseNoteRepository = marriageCaseNoteRepository;
        this.marriageConfessorApprovalRepository = marriageConfessorApprovalRepository;
        this.marriageImpedimentRepository = marriageImpedimentRepository;
        this.marriageManualPaymentRepository = marriageManualPaymentRepository;
        this.marriageWitnessRepository = marriageWitnessRepository;
        this.marriagePriestAssignmentRepository = marriagePriestAssignmentRepository;
        this.marriageScheduleRepository = marriageScheduleRepository;
        this.marriageCertificateRepository = marriageCertificateRepository;
        this.marriageCertificateSequenceConfigRepository = marriageCertificateSequenceConfigRepository;
        this.marriageCertificateAmendmentRepository = marriageCertificateAmendmentRepository;
        this.marriageSecuritySupport = marriageSecuritySupport;
        this.marriageCaseReferenceGenerator = marriageCaseReferenceGenerator;
        this.marriageCaseDomainValidator = marriageCaseDomainValidator;
        this.churchRepository = churchRepository;
        this.userRepository = userRepository;
        this.calendarEntryService = calendarEntryService;
        this.calendarEntryRepository = calendarEntryRepository;
        this.objectMapper = objectMapper;
        this.tenantAdminNotificationService = tenantAdminNotificationService;
    }

    @Override
    @Transactional
    public MarriageCaseResponse startMemberInitiatedCase(MarriageMemberInitiationRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        ChurchEntity church = resolveChurch(request.churchNumber());
        UUID tenantId = resolveTenantId(church);

        MarriageCaseEntity marriageCase = MarriageCaseEntity.builder()
                .tenantId(tenantId)
                .church(church)
                .caseReference(marriageCaseReferenceGenerator.nextReference())
                .status(MarriageCaseStatus.WAITING_FOR_BOTH_SUBMISSIONS)
                .originType(MarriageCaseOriginType.MEMBER_INITIATED)
                .pairingMode(isDirectPairing(request) ? MarriagePairingMode.INVITATION_LINK : MarriagePairingMode.MEMBER_SELF_PAIRING)
                .primaryLanguage(request.primaryLanguage() == null ? MarriageLanguageCode.EN : request.primaryLanguage())
                .build();
        marriageCase = marriageCaseRepository.save(marriageCase);

        MarriagePartyRole counterpartRole = counterpartRoleOf(request.initiatorPartyRole());
        MarriagePartyEntity initiator = MarriagePartyEntity.builder()
                .marriageCase(marriageCase)
                .partyRole(request.initiatorPartyRole())
                .member(currentUser.getMembership())
                .linkedUser(currentUser)
                .externalApplicant(false)
                .counterpartPlaceholder(false)
                .editable(true)
                .fullLegalNameEnglish(currentUser.getFullName())
                .fullLegalNameLocal(currentUser.getFullName())
                .build();
        MarriagePartyEntity counterpart = MarriagePartyEntity.builder()
                .marriageCase(marriageCase)
                .partyRole(counterpartRole)
                .externalApplicant(Boolean.TRUE.equals(request.counterpartExternalApplicant()))
                .counterpartPlaceholder(true)
                .editable(true)
                .fullLegalNameEnglish(trimToNull(request.counterpartFullLegalNameEnglish()))
                .fullLegalNameLocal(trimToNull(request.counterpartFullLegalNameLocal()))
                .contactInfo(MarriageContactInfo.builder()
                        .email(trimToNull(request.counterpartEmail()))
                        .phone(trimToNull(request.counterpartPhone()))
                        .build())
                .build();

        initiator = marriagePartyRepository.save(initiator);
        counterpart = marriagePartyRepository.save(counterpart);
        attachPartyIds(marriageCase, initiator, counterpart);
        instantiateRequirementAssignments(marriageCase, initiator, counterpart);
        writeStatusHistory(marriageCase, null, marriageCase.getStatus(), "Marriage case created", currentUser.getUuid());
        writeAuditEvent(marriageCase, MarriageCaseAuditEventType.CASE_CREATED, currentUser.getUuid(), null, "Marriage case created");
        if (counterpart.isCounterpartPlaceholder()) {
            writeAuditEvent(marriageCase, MarriageCaseAuditEventType.COUNTERPART_PLACEHOLDER_CREATED, currentUser.getUuid(), counterpart.getId(), "Counterpart placeholder created");
        }

        tenantAdminNotificationService.notifyMarriageCaseSubmitted(marriageCase, currentUser.getUuid());
        return toCaseResponse(marriageCaseRepository.save(marriageCase));
    }

    @Override
    @Transactional
    public MarriageCaseResponse createAdminInitiatedCase(MarriageAdminInitiationRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        ensureAdminLike();
        ChurchEntity church = resolveChurch(request.churchNumber());
        UUID tenantId = resolveTenantId(church);

        MarriageCaseEntity marriageCase = MarriageCaseEntity.builder()
                .tenantId(tenantId)
                .church(church)
                .caseReference(marriageCaseReferenceGenerator.nextReference())
                .status(MarriageCaseStatus.WAITING_FOR_BOTH_SUBMISSIONS)
                .originType(MarriageCaseOriginType.ADMIN_INITIATED)
                .pairingMode(MarriagePairingMode.ADMIN_MANUAL_LINK)
                .primaryLanguage(request.primaryLanguage() == null ? MarriageLanguageCode.EN : request.primaryLanguage())
                .build();
        marriageCase = marriageCaseRepository.save(marriageCase);

        MarriagePartyEntity bride = marriagePartyRepository.save(buildParty(marriageCase, MarriagePartyRole.BRIDE, request.bride()));
        MarriagePartyEntity groom = marriagePartyRepository.save(buildParty(marriageCase, MarriagePartyRole.GROOM, request.groom()));
        attachPartyIds(marriageCase, bride, groom);
        instantiateRequirementAssignments(marriageCase, bride, groom);
        writeStatusHistory(marriageCase, null, marriageCase.getStatus(), "Marriage case created by admin", currentUser.getUuid());
        writeAuditEvent(marriageCase, MarriageCaseAuditEventType.CASE_CREATED, currentUser.getUuid(), null, "Marriage case created by admin");

        return toCaseResponse(marriageCaseRepository.save(marriageCase));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarriageCaseResponse> listMine() {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        return marriageCaseRepository.findVisibleForUser(currentUser.getUuid())
                .stream()
                .map(this::toCaseResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarriageCaseResponse> listAccessibleCases() {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        UUID tenantId = currentUser.getAffiliatedTenantId() != null
                ? currentUser.getAffiliatedTenantId()
                : TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is required.");
        }

        return marriageCaseRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(this::toCaseResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MarriageCaseResponse getCase(UUID caseId) {
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        return toCaseResponse(marriageCase);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarriageCaseNoteResponse> listNotes(UUID caseId) {
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        if (marriageSecuritySupport.isSecretaryLike() || marriageSecuritySupport.hasAnyRole("PRIEST")) {
            return marriageCaseNoteRepository.findByMarriageCaseIdOrderByCreatedAtDesc(marriageCase.getId())
                    .stream()
                    .map(this::toCaseNoteResponse)
                    .toList();
        }

        return marriageCaseNoteRepository.findByMarriageCaseIdAndVisibilityOrderByCreatedAtDesc(
                        marriageCase.getId(),
                        MarriageNoteVisibility.APPLICANT_VISIBLE
                ).stream()
                .map(this::toCaseNoteResponse)
                .toList();
    }

    @Override
    @Transactional
    public MarriageCaseNoteResponse addNote(UUID caseId, MarriageCaseNoteRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        MarriagePartyEntity party = request.partyRole() == null ? null : resolveParty(marriageCase, request.partyRole());

        if (!mayCreateNoteVisibility(request.visibility())) {
            throw new IllegalStateException("You do not have permission to create notes with this visibility.");
        }

        MarriageCaseNoteEntity note = marriageCaseNoteRepository.save(MarriageCaseNoteEntity.builder()
                .marriageCase(marriageCase)
                .party(party)
                .authorUserId(currentUser.getUuid())
                .noteType(request.noteType().trim())
                .visibility(request.visibility())
                .content(request.content().trim())
                .build());

        writeAuditEvent(marriageCase, MarriageCaseAuditEventType.NOTE_ADDED, currentUser.getUuid(), party == null ? null : party.getId(), "Marriage case note added");
        return toCaseNoteResponse(note);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarriageStatusHistoryResponse> listStatusHistory(UUID caseId) {
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        return marriageStatusHistoryRepository.findByMarriageCaseIdOrderByChangedAtDesc(marriageCase.getId())
                .stream()
                .map(this::toStatusHistoryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarriageAuditEventResponse> listAuditEvents(UUID caseId) {
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        ensureSecretaryViewer();
        return marriageAuditEventRepository.findByMarriageCaseIdOrderByOccurredAtDesc(marriageCase.getId())
                .stream()
                .map(this::toAuditEventResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarriageReviewResponse> listReviews(UUID caseId) {
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        ensureSecretaryViewer();
        return marriageReviewRepository.findByMarriageCaseIdOrderByReviewedAtDesc(marriageCase.getId())
                .stream()
                .map(this::toReviewResponse)
                .toList();
    }

    @Override
    @Transactional
    public MarriageCaseResponse createCounterpartPlaceholder(UUID caseId, MarriagePartyRole partyRole, MarriageCounterpartPlaceholderRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        MarriagePartyEntity party = resolveParty(marriageCase, partyRole);

        if (!party.isCounterpartPlaceholder()) {
            throw new IllegalStateException("Counterpart placeholder already exists as a real linked party.");
        }

        party.setFullLegalNameEnglish(trimToNull(request.fullLegalNameEnglish()));
        party.setFullLegalNameLocal(trimToNull(request.fullLegalNameLocal()));
        party.setExternalApplicant(Boolean.TRUE.equals(request.externalApplicant()));
        party.setContactInfo(MarriageContactInfo.builder()
                .email(trimToNull(request.email()))
                .phone(trimToNull(request.phone()))
                .build());
        marriagePartyRepository.save(party);

        writeAuditEvent(marriageCase, MarriageCaseAuditEventType.COUNTERPART_PLACEHOLDER_CREATED, currentUser.getUuid(), party.getId(), "Counterpart placeholder updated");
        return toCaseResponse(marriageCase);
    }

    @Override
    @Transactional
    public MarriagePairingTokenResponse createPairingToken(UUID caseId, MarriagePairingTokenCreateRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        MarriagePartyEntity party = resolveParty(marriageCase, request.partyRole());

        if (!party.isCounterpartPlaceholder() && party.getLinkedUserId() != null) {
            throw new IllegalStateException("Target party is already linked.");
        }

        MarriagePairingTokenEntity token = MarriagePairingTokenEntity.builder()
                .marriageCase(marriageCase)
                .targetParty(party)
                .tokenValue(generatePairingToken())
                .inviteEmail(trimToNull(request.inviteEmail()))
                .issuedByUserId(currentUser.getUuid())
                .expiresAt(Instant.now().plusSeconds((long) Math.max(1, request.expiresInDays() == null ? 7 : request.expiresInDays()) * 86400))
                .active(true)
                .build();
        token = marriagePairingTokenRepository.save(token);

        writeAuditEvent(marriageCase, MarriageCaseAuditEventType.COUNTERPART_PAIRED, currentUser.getUuid(), party.getId(), "Pairing token created");
        return toPairingTokenResponse(token);
    }

    @Override
    @Transactional
    public MarriageCaseResponse acceptPairing(MarriagePairingAcceptRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        MarriagePairingTokenEntity token = marriagePairingTokenRepository.findByTokenValueAndActiveTrue(request.token().trim())
                .orElseThrow(() -> new IllegalStateException("Pairing token not found or inactive."));

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalStateException("Pairing token has expired.");
        }

        MarriagePartyEntity targetParty = token.getTargetParty();
        if (targetParty.getLinkedUserId() != null && !currentUser.getUuid().equals(targetParty.getLinkedUserId())) {
            throw new IllegalStateException("Pairing token is already linked to another user.");
        }

        targetParty.setLinkedUser(currentUser);
        targetParty.setMember(currentUser.getMembership());
        targetParty.setCounterpartPlaceholder(false);
        if (!StringUtils.hasText(targetParty.getFullLegalNameEnglish())) {
            targetParty.setFullLegalNameEnglish(currentUser.getFullName());
        }

        token.setAcceptedByUserId(currentUser.getUuid());
        token.setAcceptedAt(Instant.now());
        token.setActive(false);
        marriagePartyRepository.save(targetParty);
        marriagePairingTokenRepository.save(token);

        writeAuditEvent(token.getMarriageCase(), MarriageCaseAuditEventType.COUNTERPART_PAIRED, currentUser.getUuid(), targetParty.getId(), "Counterpart accepted pairing token");
        return toCaseResponse(token.getMarriageCase());
    }

    @Override
    @Transactional(readOnly = true)
    public MarriagePartyApplicationResponse getPartyApplication(UUID caseId, MarriagePartyRole partyRole) {
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        MarriagePartyEntity party = resolveParty(marriageCase, partyRole);
        MarriagePartySubmissionEntity latest = latestSubmission(party);
        return toPartyApplicationResponse(party, latest);
    }

    @Override
    @Transactional
    public MarriagePartyApplicationResponse savePartyDraft(UUID caseId, MarriagePartyRole partyRole, MarriagePartyDraftRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        MarriagePartyEntity party = resolveParty(marriageCase, partyRole);
        ensurePartyEditableByCurrentUser(party, currentUser);

        applyPartySummary(party, request);
        MarriagePartySubmissionEntity draft = upsertDraftSubmission(marriageCase, party, request);
        party.setLatestSubmissionStatus(MarriagePartySubmissionStatus.DRAFT);
        party.setEditable(true);
        marriagePartyRepository.save(party);

        if (marriageCase.getStatus() == MarriageCaseStatus.DRAFT || marriageCase.getStatus() == MarriageCaseStatus.WAITING_FOR_COUNTERPART) {
            updateCaseStatus(marriageCase, MarriageCaseStatus.WAITING_FOR_BOTH_SUBMISSIONS, "Draft saved", currentUser.getUuid());
        }

        return toPartyApplicationResponse(party, draft);
    }

    @Override
    @Transactional
    public MarriagePartyApplicationResponse submitPartyApplication(UUID caseId, MarriagePartyRole partyRole, MarriagePartyDraftRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        MarriagePartyEntity party = resolveParty(marriageCase, partyRole);
        ensurePartyEditableByCurrentUser(party, currentUser);

        applyPartySummary(party, request);
        MarriagePartySubmissionEntity draft = upsertDraftSubmission(marriageCase, party, request);
        draft.setStatus(MarriagePartySubmissionStatus.SUBMITTED);
        draft.setSubmittedAt(Instant.now());
        draft.setLockedAt(Instant.now());
        marriagePartySubmissionRepository.save(draft);

        party.setSubmitted(true);
        party.setEditable(false);
        party.setLatestSubmissionStatus(MarriagePartySubmissionStatus.SUBMITTED);
        party.setSubmittedAt(Instant.now());
        marriagePartyRepository.save(party);

        writeAuditEvent(marriageCase, MarriageCaseAuditEventType.PARTY_SUBMITTED, currentUser.getUuid(), party.getId(), party.getPartyRole() + " submission completed");

        List<MarriagePartyEntity> parties = marriagePartyRepository.findByMarriageCaseId(marriageCase.getId());
        boolean bothSubmitted = parties.stream().allMatch(MarriagePartyEntity::isSubmitted);
        marriageCase.setBothSubmitted(bothSubmitted);
        if (bothSubmitted) {
            updateCaseStatus(marriageCase, MarriageCaseStatus.BOTH_SUBMITTED, "Both parties submitted", currentUser.getUuid());
            writeAuditEvent(marriageCase, MarriageCaseAuditEventType.BOTH_PARTIES_SUBMITTED, currentUser.getUuid(), null, "Both parties submitted");
            updateCaseStatus(marriageCase, MarriageCaseStatus.SECRETARY_REVIEW_PENDING, "Secretary review pending", currentUser.getUuid());
            tenantAdminNotificationService.notifyMarriageCaseBothSubmitted(marriageCase, currentUser.getUuid());
        } else {
            marriageCaseRepository.save(marriageCase);
        }

        return toPartyApplicationResponse(party, draft);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarriageDocumentResponse> listDocuments(UUID caseId) {
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        return marriagePartyDocumentRepository.findByMarriageCaseId(marriageCase.getId())
                .stream()
                .map(this::toDocumentResponse)
                .toList();
    }

    @Override
    @Transactional
    public MarriageDocumentResponse addDocument(UUID caseId, MarriageDocumentMetadataRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        MarriagePartyEntity party = request.partyRole() == null ? null : resolveParty(marriageCase, request.partyRole());
        if (party != null) {
            ensurePartyEditableByCurrentUser(party, currentUser);
        } else if (!marriageSecuritySupport.isAdminLike()) {
            throw new IllegalStateException("Case-level document upload requires admin access.");
        }

        MarriagePartyDocumentEntity document = MarriagePartyDocumentEntity.builder()
                .marriageCase(marriageCase)
                .party(party)
                .documentCategory(normalizeCode(request.documentCategory()))
                .originalFileName(request.originalFileName().trim())
                .storageReference(request.storageReference().trim())
                .contentType(trimToNull(request.contentType()))
                .expiryDate(request.expiryDate())
                .documentNumber(trimToNull(request.documentNumber()))
                .notes(trimToNull(request.notes()))
                .uploadedByUserId(currentUser.getUuid())
                .uploadedAt(Instant.now())
                .build();
        document = marriagePartyDocumentRepository.save(document);
        return toDocumentResponse(document);
    }

    @Override
    @Transactional
    public void deleteDocument(UUID caseId, UUID documentId) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        MarriagePartyDocumentEntity document = marriagePartyDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalStateException("Marriage document not found."));
        if (!marriageCase.getId().equals(document.getMarriageCase().getId())) {
            throw new IllegalStateException("Marriage document does not belong to the specified case.");
        }

        if (document.getParty() != null) {
            ensurePartyEditableByCurrentUser(document.getParty(), currentUser);
        } else if (!marriageSecuritySupport.isAdminLike()) {
            throw new IllegalStateException("Only admin users may delete case-level documents.");
        }

        marriagePartyDocumentRepository.delete(document);
    }

    @Override
    @Transactional
    public MarriageCaseResponse secretaryReturnForCorrection(UUID caseId, MarriageReviewActionRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        ensureSecretaryLike();
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        requireCurrentStatus(marriageCase, MarriageCaseStatus.SECRETARY_REVIEW_PENDING);

        updateCaseStatus(marriageCase, MarriageCaseStatus.SECRETARY_RETURNED_FOR_CORRECTION, request.reason(), currentUser.getUuid());
        recordReview(marriageCase, MarriageReviewStage.SECRETARY_CIVIL_REVIEW, MarriageReviewDecision.RETURNED_FOR_CORRECTION,
                currentUser.getUuid(), resolveActorRole(currentUser), request.reason(), request.notes(), MarriageNoteVisibility.INTERNAL_ADMIN);
        return toCaseResponse(marriageCase);
    }

    @Override
    @Transactional
    public MarriageCaseResponse secretaryApproveCivilChecks(UUID caseId, MarriageReviewActionRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        ensureSecretaryLike();
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        if (marriageCase.getStatus() == MarriageCaseStatus.BOTH_SUBMITTED) {
            updateCaseStatus(marriageCase, MarriageCaseStatus.SECRETARY_REVIEW_PENDING, "Secretary review opened", currentUser.getUuid());
        }
        requireCurrentStatus(marriageCase, MarriageCaseStatus.SECRETARY_REVIEW_PENDING);

        marriageCase.setSecretaryClearanceComplete(true);
        marriageCaseRepository.save(marriageCase);
        updateCaseStatus(marriageCase, MarriageCaseStatus.SECRETARY_CIVIL_CHECKS_APPROVED, request.reason(), currentUser.getUuid());
        updateCaseStatus(marriageCase, MarriageCaseStatus.WAITING_FOR_CONFESSOR_APPROVAL, "Waiting for confessor approval", currentUser.getUuid());
        recordReview(marriageCase, MarriageReviewStage.SECRETARY_CIVIL_REVIEW, MarriageReviewDecision.APPROVED,
                currentUser.getUuid(), resolveActorRole(currentUser), request.reason(), request.notes(), MarriageNoteVisibility.INTERNAL_ADMIN);
        writeAuditEvent(marriageCase, MarriageCaseAuditEventType.SECRETARY_CIVIL_CHECKS_APPROVED, currentUser.getUuid(), null, "Secretary civil checks approved");
        return toCaseResponse(marriageCase);
    }

    @Override
    @Transactional
    public MarriageCaseResponse adminHold(UUID caseId, MarriageReviewActionRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        ensureAdminLike();
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        requireCurrentStatus(marriageCase, MarriageCaseStatus.ADMIN_REVIEW_PENDING);

        updateCaseStatus(marriageCase, MarriageCaseStatus.ADMIN_ON_HOLD, request.reason(), currentUser.getUuid());
        recordReview(marriageCase, MarriageReviewStage.ADMIN_REVIEW, MarriageReviewDecision.ON_HOLD,
                currentUser.getUuid(), resolveActorRole(currentUser), request.reason(), request.notes(), MarriageNoteVisibility.INTERNAL_ADMIN);
        return toCaseResponse(marriageCase);
    }

    @Override
    @Transactional
    public MarriageCaseResponse adminReturnForCorrection(UUID caseId, MarriageReviewActionRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        ensureAdminLike();
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        requireCurrentStatus(marriageCase, MarriageCaseStatus.ADMIN_REVIEW_PENDING);

        updateCaseStatus(marriageCase, MarriageCaseStatus.ADMIN_RETURNED_FOR_CORRECTION, request.reason(), currentUser.getUuid());
        recordReview(marriageCase, MarriageReviewStage.ADMIN_REVIEW, MarriageReviewDecision.RETURNED_FOR_CORRECTION,
                currentUser.getUuid(), resolveActorRole(currentUser), request.reason(), request.notes(), MarriageNoteVisibility.INTERNAL_ADMIN);
        return toCaseResponse(marriageCase);
    }

    @Override
    @Transactional
    public MarriageCaseResponse adminReject(UUID caseId, MarriageReviewActionRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        ensureAdminLike();
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        requireCurrentStatus(marriageCase, MarriageCaseStatus.ADMIN_REVIEW_PENDING);

        updateCaseStatus(marriageCase, MarriageCaseStatus.SECRETARY_REJECTED, request.reason(), currentUser.getUuid());
        recordReview(marriageCase, MarriageReviewStage.ADMIN_REVIEW, MarriageReviewDecision.REJECTED,
                currentUser.getUuid(), resolveActorRole(currentUser), request.reason(), request.notes(), MarriageNoteVisibility.INTERNAL_ADMIN);
        return toCaseResponse(marriageCase);
    }

    @Override
    @Transactional
    public MarriageCaseResponse adminApprove(UUID caseId, MarriageReviewActionRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        ensureAdminLike();
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        requireCurrentStatus(marriageCase, MarriageCaseStatus.ADMIN_REVIEW_PENDING);

        marriageCase.setAdminApprovalGranted(true);
        marriageCaseRepository.save(marriageCase);
        updateCaseStatus(marriageCase, MarriageCaseStatus.ADMIN_APPROVED_PENDING_PAYMENT, request.reason(), currentUser.getUuid());
        recordReview(marriageCase, MarriageReviewStage.ADMIN_REVIEW, MarriageReviewDecision.APPROVED,
                currentUser.getUuid(), resolveActorRole(currentUser), request.reason(), request.notes(), MarriageNoteVisibility.INTERNAL_ADMIN);
        writeAuditEvent(marriageCase, MarriageCaseAuditEventType.ADMIN_APPROVED, currentUser.getUuid(), null, "Marriage case approved by admin");
        return toCaseResponse(marriageCase);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarriageConfessorApprovalResponse> listConfessorApprovals(UUID caseId) {
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        return marriageConfessorApprovalRepository.findByMarriageCaseIdOrderByApprovalDateDesc(marriageCase.getId())
                .stream()
                .map(this::toConfessorApprovalResponse)
                .toList();
    }

    @Override
    @Transactional
    public MarriageConfessorApprovalResponse recordInAppConfessorApproval(UUID caseId, MarriageConfessorApprovalRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        ensureSecretaryLike();
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        requireCurrentStatus(marriageCase, MarriageCaseStatus.WAITING_FOR_CONFESSOR_APPROVAL);

        UserEntity priestUser = userRepository.findById(request.priestUserId())
                .orElseThrow(() -> new IllegalStateException("Priest user not found."));
        MarriagePartyEntity party = request.partyRole() == null ? null : resolveParty(marriageCase, request.partyRole());

        MarriageConfessorApprovalEntity approval = marriageConfessorApprovalRepository.save(MarriageConfessorApprovalEntity.builder()
                .marriageCase(marriageCase)
                .party(party)
                .approvalStatus(MarriageConfessorApprovalStatus.APPROVED)
                .approvalMode(MarriageConfessorApprovalMode.IN_APP_PRIEST)
                .priestUserId(priestUser.getUuid())
                .priestPersonName(priestUser.getFullName())
                .approvalDate(request.approvalDate() == null ? LocalDate.now() : request.approvalDate())
                .notes(trimToNull(request.notes()) == null ? "Confessor approved in-app." : request.notes().trim())
                .blocking(true)
                .build());

        marriageCase.setConfessorGateSatisfied(true);
        marriageCaseRepository.save(marriageCase);
        updateCaseStatus(marriageCase, MarriageCaseStatus.ADMIN_REVIEW_PENDING, "Confessor approval recorded", currentUser.getUuid());
        recordReview(marriageCase, MarriageReviewStage.CONFESSOR_GATE_REVIEW, MarriageReviewDecision.APPROVED,
                currentUser.getUuid(), resolveActorRole(currentUser), "Confessor approval recorded", request.notes(), MarriageNoteVisibility.CONFIDENTIAL_PASTORAL);
        writeAuditEvent(marriageCase, MarriageCaseAuditEventType.CONFESSOR_APPROVED, currentUser.getUuid(), party == null ? null : party.getId(), "Confessor approval recorded");
        return toConfessorApprovalResponse(approval);
    }

    @Override
    @Transactional
    public MarriageConfessorApprovalResponse recordExternalConfessorApproval(UUID caseId, MarriageExternalConfessorApprovalRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        ensureSecretaryLike();
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        requireCurrentStatus(marriageCase, MarriageCaseStatus.WAITING_FOR_CONFESSOR_APPROVAL);

        MarriagePartyEntity party = request.partyRole() == null ? null : resolveParty(marriageCase, request.partyRole());
        MarriageConfessorApprovalEntity approval = marriageConfessorApprovalRepository.save(MarriageConfessorApprovalEntity.builder()
                .marriageCase(marriageCase)
                .party(party)
                .approvalStatus(MarriageConfessorApprovalStatus.APPROVED)
                .approvalMode(MarriageConfessorApprovalMode.EXTERNAL_RECORDED)
                .priestPersonName(request.priestPersonName().trim())
                .churchName(trimToNull(request.churchName()))
                .dioceseName(trimToNull(request.dioceseName()))
                .approvalDate(request.approvalDate() == null ? LocalDate.now() : request.approvalDate())
                .evidenceDocumentId(request.evidenceDocumentId())
                .notes(request.notes().trim())
                .blocking(true)
                .build());

        marriageCase.setConfessorGateSatisfied(true);
        marriageCaseRepository.save(marriageCase);
        updateCaseStatus(marriageCase, MarriageCaseStatus.ADMIN_REVIEW_PENDING, "External confessor approval recorded", currentUser.getUuid());
        recordReview(marriageCase, MarriageReviewStage.CONFESSOR_GATE_REVIEW, MarriageReviewDecision.APPROVED,
                currentUser.getUuid(), resolveActorRole(currentUser), "External confessor approval recorded", request.notes(), MarriageNoteVisibility.CONFIDENTIAL_PASTORAL);
        writeAuditEvent(marriageCase, MarriageCaseAuditEventType.CONFESSOR_APPROVED, currentUser.getUuid(), party == null ? null : party.getId(), "External confessor approval recorded");
        return toConfessorApprovalResponse(approval);
    }

    @Override
    @Transactional
    public MarriageConfessorApprovalResponse recordConfessorBlock(UUID caseId, MarriageConfessorBlockRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        ensureSecretaryLike();
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        requireCurrentStatus(marriageCase, MarriageCaseStatus.WAITING_FOR_CONFESSOR_APPROVAL);

        MarriagePartyEntity party = request.partyRole() == null ? null : resolveParty(marriageCase, request.partyRole());
        MarriageConfessorApprovalEntity approval = marriageConfessorApprovalRepository.save(MarriageConfessorApprovalEntity.builder()
                .marriageCase(marriageCase)
                .party(party)
                .approvalStatus(MarriageConfessorApprovalStatus.BLOCKED)
                .approvalMode(MarriageConfessorApprovalMode.EXTERNAL_RECORDED)
                .priestUserId(request.priestUserId())
                .notes(request.notes().trim())
                .blocking(true)
                .approvalDate(LocalDate.now())
                .build());

        marriageCase.setConfessorGateSatisfied(false);
        marriageCaseRepository.save(marriageCase);
        updateCaseStatus(marriageCase, MarriageCaseStatus.CONFESSOR_BLOCKED, request.notes(), currentUser.getUuid());
        recordReview(marriageCase, MarriageReviewStage.CONFESSOR_GATE_REVIEW, MarriageReviewDecision.REJECTED,
                currentUser.getUuid(), resolveActorRole(currentUser), "Confessor blocked case", request.notes(), MarriageNoteVisibility.CONFIDENTIAL_PASTORAL);
        writeAuditEvent(marriageCase, MarriageCaseAuditEventType.CONFESSOR_BLOCKED, currentUser.getUuid(), party == null ? null : party.getId(), "Confessor blocked the case");
        return toConfessorApprovalResponse(approval);
    }

    @Override
    @Transactional
    public MarriageConfessorApprovalResponse recordDioceseOverride(UUID caseId, MarriageDioceseOverrideRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        ensureAdminLike();
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        if (marriageCase.getStatus() != MarriageCaseStatus.WAITING_FOR_CONFESSOR_APPROVAL
                && marriageCase.getStatus() != MarriageCaseStatus.CONFESSOR_BLOCKED) {
            throw new IllegalStateException("Diocese override is only allowed during confessor gating.");
        }

        MarriagePartyEntity party = request.partyRole() == null ? null : resolveParty(marriageCase, request.partyRole());
        MarriageConfessorApprovalEntity approval = marriageConfessorApprovalRepository.save(MarriageConfessorApprovalEntity.builder()
                .marriageCase(marriageCase)
                .party(party)
                .approvalStatus(MarriageConfessorApprovalStatus.OVERRIDDEN)
                .approvalMode(MarriageConfessorApprovalMode.DIOCESE_OVERRIDE)
                .notes(request.notes().trim())
                .blocking(false)
                .overrideReason(request.overrideReason().trim())
                .overrideDocumentId(request.overrideDocumentId())
                .approvalDate(LocalDate.now())
                .build());

        marriageCase.setConfessorGateSatisfied(true);
        marriageCaseRepository.save(marriageCase);
        updateCaseStatus(marriageCase, MarriageCaseStatus.DIOCESE_OVERRIDE_RECORDED, request.overrideReason(), currentUser.getUuid());
        writeAuditEvent(marriageCase, MarriageCaseAuditEventType.DIOCESE_OVERRIDE_RECORDED, currentUser.getUuid(), party == null ? null : party.getId(), "Diocese override recorded");
        updateCaseStatus(marriageCase, MarriageCaseStatus.ADMIN_REVIEW_PENDING, "Moved to admin review after diocesan override", currentUser.getUuid());
        recordReview(marriageCase, MarriageReviewStage.CONFESSOR_GATE_REVIEW, MarriageReviewDecision.APPROVED,
                currentUser.getUuid(), resolveActorRole(currentUser), request.overrideReason(), request.notes(), MarriageNoteVisibility.CONFIDENTIAL_PASTORAL);
        return toConfessorApprovalResponse(approval);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarriageImpedimentResponse> listImpediments(UUID caseId) {
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        return marriageImpedimentRepository.findByMarriageCaseId(marriageCase.getId())
                .stream()
                .map(this::toImpedimentResponse)
                .toList();
    }

    @Override
    @Transactional
    public MarriageImpedimentResponse createImpediment(UUID caseId, MarriageImpedimentCreateRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        ensureSecretaryLike();
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        MarriagePartyEntity party = request.partyRole() == null ? null : resolveParty(marriageCase, request.partyRole());

        MarriageImpedimentEntity impediment = marriageImpedimentRepository.save(MarriageImpedimentEntity.builder()
                .marriageCase(marriageCase)
                .party(party)
                .impedimentType(request.impedimentType())
                .severity(request.severity())
                .sourceStage(request.sourceStage().trim())
                .blocking(request.blocking())
                .status(MarriageImpedimentStatus.OPEN)
                .createdByUserId(currentUser.getUuid())
                .evidenceNote(request.evidenceNote().trim())
                .build());
        writeAuditEvent(marriageCase, MarriageCaseAuditEventType.IMPEDIMENT_OPENED, currentUser.getUuid(), party == null ? null : party.getId(), "Marriage impediment opened");
        return toImpedimentResponse(impediment);
    }

    @Override
    @Transactional
    public MarriageImpedimentResponse resolveImpediment(UUID caseId, UUID impedimentId, MarriageImpedimentResolveRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        ensureSecretaryLike();
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        MarriageImpedimentEntity impediment = marriageImpedimentRepository.findById(impedimentId)
                .orElseThrow(() -> new IllegalStateException("Marriage impediment not found."));
        if (!marriageCase.getId().equals(impediment.getMarriageCase().getId())) {
            throw new IllegalStateException("Marriage impediment does not belong to the specified case.");
        }

        impediment.setStatus(request.waive() ? MarriageImpedimentStatus.WAIVED : MarriageImpedimentStatus.RESOLVED);
        impediment.setResolvedByUserId(currentUser.getUuid());
        impediment.setEvidenceNote(request.evidenceNote().trim());
        marriageImpedimentRepository.save(impediment);
        writeAuditEvent(marriageCase, MarriageCaseAuditEventType.IMPEDIMENT_RESOLVED, currentUser.getUuid(),
                impediment.getParty() == null ? null : impediment.getParty().getId(), "Marriage impediment resolved");
        return toImpedimentResponse(impediment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarriagePriestLookupResponse> listActivePriests(Long churchId, String query) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        UUID tenantId = currentUser.getAffiliatedTenantId() != null ? currentUser.getAffiliatedTenantId() : TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is required.");
        }
        String effectiveQuery = trimToNull(query);
        List<SimpleUserDTO> users = StringUtils.hasText(effectiveQuery)
                ? userRepository.searchByTenantIdAndRoles(tenantId, effectiveQuery, Set.of("PRIEST"))
                : userRepository.searchByTenantIdAndRoles(tenantId, "", Set.of("PRIEST"));

        return users.stream()
                .map(user -> new MarriagePriestLookupResponse(user.uuid(), user.fullName(), user.email()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarriageManualPaymentResponse> listPayments(UUID caseId) {
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        return marriageManualPaymentRepository.findByMarriageCaseId(marriageCase.getId())
                .stream()
                .map(this::toPaymentResponse)
                .toList();
    }

    @Override
    @Transactional
    public MarriageManualPaymentResponse recordManualPayment(UUID caseId, MarriageManualPaymentRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        ensureSecretaryLike();
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);

        if (marriageCase.getStatus() == MarriageCaseStatus.ADMIN_APPROVED_PENDING_PAYMENT) {
            updateCaseStatus(marriageCase, MarriageCaseStatus.WAITING_FOR_MANUAL_PAYMENT, "Waiting for manual payment", currentUser.getUuid());
        }

        MarriageManualPaymentEntity payment = marriageManualPaymentRepository.save(MarriageManualPaymentEntity.builder()
                .marriageCase(marriageCase)
                .paymentCategory(request.paymentCategory().trim())
                .amount(request.amount())
                .currency(request.currency().trim().toUpperCase(Locale.ROOT))
                .receiptReferenceNumber(trimToNull(request.receiptReferenceNumber()))
                .receivedByUserId(currentUser.getUuid())
                .receivedDate(request.receivedDate() == null ? LocalDate.now() : request.receivedDate())
                .verificationStatus(MarriageManualPaymentStatus.RECEIVED)
                .note(trimToNull(request.note()))
                .build());

        writeAuditEvent(marriageCase, MarriageCaseAuditEventType.MANUAL_PAYMENT_RECORDED, currentUser.getUuid(), null, "Manual payment recorded");
        return toPaymentResponse(payment);
    }

    @Override
    @Transactional
    public MarriageManualPaymentResponse verifyManualPayment(UUID caseId, UUID paymentId, MarriageManualPaymentVerificationRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        ensureSecretaryLike();
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        MarriageManualPaymentEntity payment = marriageManualPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalStateException("Marriage manual payment not found."));
        if (!marriageCase.getId().equals(payment.getMarriageCase().getId())) {
            throw new IllegalStateException("Marriage payment does not belong to the specified case.");
        }

        payment.setVerificationStatus(MarriageManualPaymentStatus.VERIFIED);
        payment.setNote(trimToNull(request.note()) == null ? payment.getNote() : request.note().trim());
        marriageManualPaymentRepository.save(payment);

        marriageCase.setManualPaymentSatisfied(true);
        marriageCaseRepository.save(marriageCase);
        updateCaseStatus(marriageCase, MarriageCaseStatus.PAYMENT_CONFIRMED, "Manual payment verified", currentUser.getUuid());
        updateCaseStatus(marriageCase, MarriageCaseStatus.READY_FOR_SCHEDULING, "Case ready for scheduling", currentUser.getUuid());
        marriageCase.setReadyForScheduling(true);
        marriageCaseRepository.save(marriageCase);
        return toPaymentResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarriageWitnessResponse> listWitnesses(UUID caseId) {
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        return marriageWitnessRepository.findByMarriageCaseIdOrderBySortOrderAsc(marriageCase.getId())
                .stream()
                .map(this::toWitnessResponse)
                .toList();
    }

    @Override
    @Transactional
    public MarriageWitnessResponse addWitness(UUID caseId, MarriageWitnessUpsertRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        ensureSecretaryLike();
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        MarriageWitnessEntity witness = applyWitnessRequest(
                MarriageWitnessEntity.builder().marriageCase(marriageCase).build(),
                marriageCase,
                request,
                currentUser.getUuid()
        );
        witness = marriageWitnessRepository.save(witness);
        return toWitnessResponse(witness);
    }

    @Override
    @Transactional
    public MarriageWitnessResponse updateWitness(UUID caseId, UUID witnessId, MarriageWitnessUpsertRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        ensureSecretaryLike();
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        MarriageWitnessEntity witness = marriageWitnessRepository.findById(witnessId)
                .orElseThrow(() -> new IllegalStateException("Marriage witness not found."));
        if (!marriageCase.getId().equals(witness.getMarriageCase().getId())) {
            throw new IllegalStateException("Marriage witness does not belong to the specified case.");
        }
        witness = applyWitnessRequest(witness, marriageCase, request, currentUser.getUuid());
        witness = marriageWitnessRepository.save(witness);
        return toWitnessResponse(witness);
    }

    @Override
    @Transactional
    public void deleteWitness(UUID caseId, UUID witnessId) {
        ensureSecretaryLike();
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        MarriageWitnessEntity witness = marriageWitnessRepository.findById(witnessId)
                .orElseThrow(() -> new IllegalStateException("Marriage witness not found."));
        if (!marriageCase.getId().equals(witness.getMarriageCase().getId())) {
            throw new IllegalStateException("Marriage witness does not belong to the specified case.");
        }
        marriageWitnessRepository.delete(witness);
    }

    @Override
    @Transactional
    public MarriagePriestAssignmentResponse assignPriest(UUID caseId, MarriagePriestAssignmentRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        ensureAdminLike();
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        if (marriageCase.getStatus() != MarriageCaseStatus.READY_FOR_SCHEDULING
                && marriageCase.getStatus() != MarriageCaseStatus.PAYMENT_CONFIRMED
                && marriageCase.getStatus() != MarriageCaseStatus.PRIEST_ASSIGNED) {
            throw new IllegalStateException("Priest assignment requires the case to be payment-confirmed or ready for scheduling.");
        }

        UserEntity priest = userRepository.findById(request.priestUserId())
                .orElseThrow(() -> new IllegalStateException("Priest user not found."));
        marriagePriestAssignmentRepository.findFirstByMarriageCaseIdAndActiveTrue(marriageCase.getId())
                .ifPresent(existing -> {
                    existing.setActive(false);
                    marriagePriestAssignmentRepository.save(existing);
                });

        MarriagePriestAssignmentEntity assignment = marriagePriestAssignmentRepository.save(MarriagePriestAssignmentEntity.builder()
                .marriageCase(marriageCase)
                .priestUserId(priest.getUuid())
                .priestNameSnapshot(priest.getFullName())
                .assignedAt(Instant.now())
                .assignedByUserId(currentUser.getUuid())
                .active(true)
                .assignmentNote(trimToNull(request.assignmentNote()))
                .build());

        if (marriageCase.getStatus() == MarriageCaseStatus.PAYMENT_CONFIRMED) {
            updateCaseStatus(marriageCase, MarriageCaseStatus.READY_FOR_SCHEDULING, "Case ready for scheduling", currentUser.getUuid());
        }
        if (marriageCase.getStatus() != MarriageCaseStatus.PRIEST_ASSIGNED) {
            updateCaseStatus(marriageCase, MarriageCaseStatus.PRIEST_ASSIGNED, "Priest assigned", currentUser.getUuid());
        }
        writeAuditEvent(marriageCase, MarriageCaseAuditEventType.PRIEST_ASSIGNED, currentUser.getUuid(), null, "Priest assigned to marriage case");
        return toPriestAssignmentResponse(assignment);
    }

    @Override
    @Transactional
    public MarriageScheduleResponse proposeSchedule(UUID caseId, MarriageScheduleRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        ensureAdminLike();
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        requireAssignedPriest(marriageCase);

        MarriageScheduleEntity schedule = findOrCreateSchedule(marriageCase);
        schedule.setProposedDateTime(request.dateTime());
        schedule.setPlaceLabel(request.placeLabel().trim());
        schedule.setSchedulingNote(trimToNull(request.schedulingNote()));
        schedule.setScheduleStatus(MarriageScheduleStatus.PROPOSED);
        schedule = marriageScheduleRepository.save(schedule);
        return toScheduleResponse(schedule);
    }

    @Override
    @Transactional
    public MarriageScheduleResponse confirmSchedule(UUID caseId, MarriageScheduleRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        ensureAdminLike();
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        MarriageScheduleEntity schedule = findOrCreateSchedule(marriageCase);
        UUID assignedPriestUserId = requireAssignedPriest(marriageCase);

        schedule.setProposedDateTime(request.dateTime());
        schedule.setApprovedDateTime(request.dateTime());
        schedule.setPlaceLabel(request.placeLabel().trim());
        schedule.setSchedulingNote(trimToNull(request.schedulingNote()));
        schedule.setAssignedPriestUserId(assignedPriestUserId);
        schedule.setScheduleStatus(MarriageScheduleStatus.CONFIRMED);
        marriageScheduleRepository.save(schedule);
        syncCalendarEntries(marriageCase, schedule, currentUser.getUuid(), request.timezone());
        updateCaseStatus(marriageCase, MarriageCaseStatus.SCHEDULED, "Wedding ceremony scheduled", currentUser.getUuid());
        writeAuditEvent(marriageCase, MarriageCaseAuditEventType.SCHEDULE_CREATED, currentUser.getUuid(), null, "Marriage schedule confirmed");
        return toScheduleResponse(schedule);
    }

    @Override
    @Transactional
    public MarriageScheduleResponse reschedule(UUID caseId, MarriageScheduleRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        ensureAdminLike();
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        MarriageScheduleEntity schedule = marriageScheduleRepository.findByMarriageCaseId(marriageCase.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Marriage schedule not found."));

        schedule.setProposedDateTime(request.dateTime());
        schedule.setApprovedDateTime(request.dateTime());
        schedule.setPlaceLabel(request.placeLabel().trim());
        schedule.setSchedulingNote(trimToNull(request.schedulingNote()));
        schedule.setRescheduleCount(schedule.getRescheduleCount() + 1);
        schedule.setScheduleStatus(MarriageScheduleStatus.RESCHEDULED);
        marriageScheduleRepository.save(schedule);
        syncCalendarEntries(marriageCase, schedule, currentUser.getUuid(), request.timezone());
        updateCaseStatus(marriageCase, MarriageCaseStatus.CEREMONY_POSTPONED, "Wedding ceremony rescheduled", currentUser.getUuid());
        updateCaseStatus(marriageCase, MarriageCaseStatus.SCHEDULED, "Wedding ceremony rescheduled and confirmed", currentUser.getUuid());
        return toScheduleResponse(schedule);
    }

    @Override
    @Transactional
    public MarriageScheduleResponse cancelSchedule(UUID caseId, MarriageScheduleCancellationRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        ensureAdminLike();
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        MarriageScheduleEntity schedule = marriageScheduleRepository.findByMarriageCaseId(marriageCase.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Marriage schedule not found."));

        schedule.setScheduleStatus(MarriageScheduleStatus.CANCELLED);
        schedule.setSchedulingNote(request.reason().trim());
        marriageScheduleRepository.save(schedule);
        cancelCalendarEntries(schedule);
        updateCaseStatus(marriageCase, MarriageCaseStatus.CEREMONY_CANCELLED, request.reason(), currentUser.getUuid());
        return toScheduleResponse(schedule);
    }

    @Override
    @Transactional
    public MarriageCaseResponse completeCeremony(UUID caseId, MarriageCeremonyCompletionRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        ensureAdminLike();
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        if (marriageCase.getStatus() != MarriageCaseStatus.SCHEDULED
                && marriageCase.getStatus() != MarriageCaseStatus.CEREMONY_CONFIRMED) {
            throw new IllegalStateException("Ceremony completion requires a scheduled or confirmed ceremony.");
        }

        MarriageScheduleEntity schedule = marriageScheduleRepository.findByMarriageCaseId(marriageCase.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Marriage schedule not found."));
        schedule.setScheduleStatus(MarriageScheduleStatus.COMPLETED);
        if (schedule.getApprovedDateTime() == null) {
            schedule.setApprovedDateTime(request.ceremonyCompletedAt());
        }
        schedule.setSchedulingNote(trimToNull(request.ceremonyNote()));
        marriageScheduleRepository.save(schedule);

        marriageCase.setCeremonyCompleted(true);
        marriageCaseRepository.save(marriageCase);
        updateCaseStatus(marriageCase, MarriageCaseStatus.CEREMONY_COMPLETED, "Ceremony completed", currentUser.getUuid());
        writeAuditEvent(marriageCase, MarriageCaseAuditEventType.CEREMONY_COMPLETED, currentUser.getUuid(), null, "Marriage ceremony completed");
        return toCaseResponse(marriageCase);
    }

    @Override
    @Transactional
    public MarriageCertificateSequenceConfigResponse configureCertificateSequence(MarriageCertificateSequenceConfigRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        ensureAdminLike();
        ChurchEntity church = resolveChurch(request.churchNumber());
        UUID tenantId = resolveTenantId(church);

        marriageCertificateSequenceConfigRepository.findFirstByChurch_ChurchIdAndActiveTrue(church.getChurchId())
                .ifPresent(existing -> {
                    existing.setActive(false);
                    marriageCertificateSequenceConfigRepository.save(existing);
                });

        MarriageCertificateSequenceConfigEntity config = MarriageCertificateSequenceConfigEntity.builder()
                .tenantId(tenantId)
                .church(church)
                .prefix(trimToNull(request.prefix()))
                .separator(trimToNull(request.separator()) == null ? "-" : request.separator().trim())
                .startingSeed(request.startingSeed())
                .currentNumber(request.startingSeed() - 1)
                .resetMode(trimToNull(request.resetMode()))
                .formatMask(request.formatMask().trim())
                .migrationReference(trimToNull(request.migrationReference()))
                .active(true)
                .build();
        config = marriageCertificateSequenceConfigRepository.save(config);
        return toSequenceConfigResponse(config);
    }

    @Override
    @Transactional(readOnly = true)
    public MarriageCertificateSequenceConfigResponse getCertificateSequenceConfig(String churchNumber) {
        ChurchEntity church = resolveChurch(churchNumber);
        MarriageCertificateSequenceConfigEntity config = marriageCertificateSequenceConfigRepository.findFirstByChurch_ChurchIdAndActiveTrue(church.getChurchId())
                .orElseThrow(() -> new IllegalStateException("Marriage certificate sequence config not found for church."));
        return toSequenceConfigResponse(config);
    }

    @Override
    @Transactional
    public MarriageCertificateResponse prepareCertificate(UUID caseId, MarriageCertificatePrepareRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        ensureAdminLike();
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        marriageCaseDomainValidator.requireCaseReadyForCertificate(marriageCase);
        if (marriageCase.getStatus() != MarriageCaseStatus.CEREMONY_COMPLETED
                && marriageCase.getStatus() != MarriageCaseStatus.CERTIFICATE_READY) {
            throw new IllegalStateException("Certificate preparation requires ceremony completion.");
        }

        String snapshotJson = buildCertificateSnapshot(marriageCase, request.registryReference());
        MarriageCertificateEntity certificate = marriageCertificateRepository.findFirstByMarriageCaseIdOrderByIssuedDateDesc(marriageCase.getId())
                .orElseGet(() -> MarriageCertificateEntity.builder()
                        .marriageCase(marriageCase)
                        .certificateNumber("PENDING")
                        .numberingFormatSnapshot("PENDING")
                        .issuedDate(Instant.EPOCH)
                        .issuedByUserId(currentUser.getUuid())
                        .status(MarriageCertificateStatus.DRAFT)
                        .build());
        certificate.setLockedSnapshotJson(snapshotJson);
        certificate.setRegistryReference(trimToNull(request.registryReference()));
        certificate.setStatus(MarriageCertificateStatus.READY);
        certificate = marriageCertificateRepository.save(certificate);

        if (marriageCase.getStatus() != MarriageCaseStatus.CERTIFICATE_READY) {
            updateCaseStatus(marriageCase, MarriageCaseStatus.CERTIFICATE_READY, "Certificate snapshot prepared", currentUser.getUuid());
        }
        writeAuditEvent(marriageCase, MarriageCaseAuditEventType.CERTIFICATE_PREPARED, currentUser.getUuid(), null, "Certificate snapshot prepared");
        return toCertificateResponse(certificate);
    }

    @Override
    @Transactional
    public MarriageCertificateResponse issueCertificate(UUID caseId, MarriageCertificateIssueRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        ensureAdminLike();
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        MarriageCertificateEntity certificate = marriageCertificateRepository.findFirstByMarriageCaseIdOrderByIssuedDateDesc(marriageCase.getId())
                .orElseThrow(() -> new IllegalStateException("Prepared marriage certificate not found."));

        if (certificate.getStatus() != MarriageCertificateStatus.READY) {
            throw new IllegalStateException("Marriage certificate must be prepared before issuance.");
        }
        if (!StringUtils.hasText(certificate.getLockedSnapshotJson())) {
            throw new IllegalStateException("Marriage certificate snapshot is missing.");
        }

        MarriageCertificateSequenceConfigEntity config = marriageCertificateSequenceConfigRepository
                .findFirstByChurch_ChurchIdAndActiveTrue(marriageCase.getChurchId())
                .orElseThrow(() -> new IllegalStateException("Active marriage certificate sequence config not found."));

        long nextNumber = config.getCurrentNumber() + 1;
        config.setCurrentNumber(nextNumber);
        marriageCertificateSequenceConfigRepository.save(config);

        String certificateNumber = formatCertificateNumber(config, nextNumber);
        if (marriageCertificateRepository.existsByCertificateNumber(certificateNumber)) {
            throw new IllegalStateException("Generated certificate number already exists: " + certificateNumber);
        }

        certificate.setCertificateNumber(certificateNumber);
        certificate.setNumberingFormatSnapshot(config.getFormatMask());
        certificate.setIssuedDate(Instant.now());
        certificate.setIssuedByUserId(currentUser.getUuid());
        certificate.setRegistryReference(trimToNull(request.registryReference()) == null ? certificate.getRegistryReference() : request.registryReference().trim());
        certificate.setStatus(MarriageCertificateStatus.ISSUED);
        certificate = marriageCertificateRepository.save(certificate);

        marriageCase.setCertificateIssued(true);
        marriageCaseRepository.save(marriageCase);
        updateCaseStatus(marriageCase, MarriageCaseStatus.CERTIFICATE_ISSUED, "Marriage certificate issued", currentUser.getUuid());
        writeAuditEvent(marriageCase, MarriageCaseAuditEventType.CERTIFICATE_ISSUED, currentUser.getUuid(), null, "Marriage certificate issued");
        return toCertificateResponse(certificate);
    }

    @Override
    @Transactional(readOnly = true)
    public MarriageCertificateResponse getCertificate(UUID caseId) {
        MarriageCaseEntity marriageCase = resolveAccessibleCase(caseId);
        MarriageCertificateEntity certificate = marriageCertificateRepository.findFirstByMarriageCaseIdOrderByIssuedDateDesc(marriageCase.getId())
                .orElseThrow(() -> new IllegalStateException("Marriage certificate not found."));
        return toCertificateResponse(certificate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarriageCertificateResponse> listCertificateRegistry() {
        marriageSecuritySupport.requireCurrentUser();
        return marriageCertificateRepository.findByStatusOrderByIssuedDateDesc(MarriageCertificateStatus.ISSUED)
                .stream()
                .map(this::toCertificateResponse)
                .toList();
    }

    @Override
    @Transactional
    public MarriageCertificateAmendmentResponse createCertificateAmendment(UUID certificateId, MarriageCertificateAmendmentRequest request) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        ensureAdminLike();
        MarriageCertificateEntity certificate = marriageCertificateRepository.findById(certificateId)
                .orElseThrow(() -> new IllegalStateException("Marriage certificate not found."));
        if (certificate.getStatus() != MarriageCertificateStatus.ISSUED && certificate.getStatus() != MarriageCertificateStatus.AMENDED) {
            throw new IllegalStateException("Only issued marriage certificates can be amended.");
        }

        MarriageCertificateAmendmentEntity amendment = marriageCertificateAmendmentRepository.save(MarriageCertificateAmendmentEntity.builder()
                .certificate(certificate)
                .amendmentReason(request.amendmentReason().trim())
                .amendmentSnapshotJson(request.amendmentSnapshotJson().trim())
                .amendedByUserId(currentUser.getUuid())
                .amendedAt(Instant.now())
                .build());

        certificate.setHasAmendment(true);
        certificate.setStatus(MarriageCertificateStatus.AMENDED);
        marriageCertificateRepository.save(certificate);
        return toCertificateAmendmentResponse(amendment);
    }

    private MarriagePartyEntity buildParty(MarriageCaseEntity marriageCase, MarriagePartyRole partyRole, MarriagePartyCreateRequest request) {
        MarriageContactInfo contactInfo = MarriageContactInfo.builder()
                .email(trimToNull(request.email()))
                .phone(trimToNull(request.phone()))
                .build();

        return MarriagePartyEntity.builder()
                .marriageCase(marriageCase)
                .partyRole(partyRole)
                .fullLegalNameEnglish(trimToNull(request.fullLegalNameEnglish()))
                .fullLegalNameLocal(trimToNull(request.fullLegalNameLocal()))
                .contactInfo(contactInfo)
                .externalApplicant(Boolean.TRUE.equals(request.externalApplicant()))
                .counterpartPlaceholder(Boolean.TRUE.equals(request.placeholder()))
                .editable(true)
                .build();
    }

    private void attachPartyIds(MarriageCaseEntity marriageCase, MarriagePartyEntity first, MarriagePartyEntity second) {
        MarriagePartyEntity bride = first.getPartyRole() == MarriagePartyRole.BRIDE ? first : second;
        MarriagePartyEntity groom = first.getPartyRole() == MarriagePartyRole.GROOM ? first : second;
        marriageCase.setBridePartyId(bride.getId());
        marriageCase.setGroomPartyId(groom.getId());
    }

    private void instantiateRequirementAssignments(MarriageCaseEntity marriageCase, MarriagePartyEntity bride, MarriagePartyEntity groom) {
        List<MarriageRequirementTemplateEntity> templates =
                marriageRequirementTemplateRepository.findByChurch_ChurchIdAndEnabledTrueOrderByOrderIndexAsc(marriageCase.getChurchId());

        List<MarriageRequirementAssignmentEntity> assignments = new ArrayList<>();
        for (MarriageRequirementTemplateEntity template : templates) {
            MarriagePartyEntity targetParty = switch (template.getAppliesTo()) {
                case BRIDE -> bride;
                case GROOM -> groom;
                case CASE, BOTH_PARTIES -> null;
            };

            assignments.add(MarriageRequirementAssignmentEntity.builder()
                    .requirementTemplate(template)
                    .marriageCase(marriageCase)
                    .party(targetParty)
                    .currentStatus(MarriageRequirementStatus.PENDING)
                    .blocking(template.isBlocking())
                    .build());
        }
        marriageRequirementAssignmentRepository.saveAll(assignments);
    }

    private MarriagePartySubmissionEntity upsertDraftSubmission(
            MarriageCaseEntity marriageCase,
            MarriagePartyEntity party,
            MarriagePartyDraftRequest request
    ) {
        MarriagePartySubmissionEntity draft = marriagePartySubmissionRepository
                .findFirstByPartyIdAndStatusOrderBySubmissionVersionDesc(party.getId(), MarriagePartySubmissionStatus.DRAFT)
                .orElseGet(() -> MarriagePartySubmissionEntity.builder()
                        .marriageCase(marriageCase)
                        .party(party)
                        .submissionVersion(nextSubmissionVersion(party.getId()))
                        .status(MarriagePartySubmissionStatus.DRAFT)
                        .build());

        draft.setApplicationSnapshotJson(toJson(request));
        return marriagePartySubmissionRepository.save(draft);
    }

    private int nextSubmissionVersion(UUID partyId) {
        return marriagePartySubmissionRepository.findByPartyIdOrderBySubmissionVersionDesc(partyId)
                .stream()
                .findFirst()
                .map(existing -> existing.getSubmissionVersion() + 1)
                .orElse(1);
    }

    private void applyPartySummary(MarriagePartyEntity party, MarriagePartyDraftRequest request) {
        party.setFullLegalNameEnglish(request.fullLegalNameEnglish().trim());
        party.setFullLegalNameLocal(trimToNull(request.fullLegalNameLocal()));
        party.setDateOfBirth(request.dateOfBirth());
        party.setMaritalStatus(request.maritalStatus().trim());
        party.setContactInfo(MarriageContactInfo.builder()
                .phone(trimToNull(request.phone()))
                .alternatePhone(trimToNull(request.alternatePhone()))
                .email(trimToNull(request.email()))
                .addressLine(trimToNull(request.addressLine()))
                .currentCountry(trimToNull(request.currentCountry()))
                .currentCity(trimToNull(request.currentCity()))
                .build());
        party.setIdentityInfo(MarriageIdentityInfo.builder()
                .governmentIdType(trimToNull(request.governmentIdType()))
                .governmentIdNumber(trimToNull(request.governmentIdNumber()))
                .passportNumber(trimToNull(request.passportNumber()))
                .build());
    }

    private MarriageCaseEntity resolveAccessibleCase(UUID caseId) {
        UserEntity currentUser = marriageSecuritySupport.requireCurrentUser();
        MarriageCaseEntity marriageCase = marriageCaseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalStateException("Marriage case not found."));

        boolean memberVisible = marriageCase.getParties().stream()
                .anyMatch(p -> currentUser.getUuid().equals(p.getLinkedUserId()));
        boolean staffVisible = marriageSecuritySupport.isSecretaryLike() || marriageSecuritySupport.hasAnyRole("PRIEST");
        if (!memberVisible && !marriageSecuritySupport.isAdminLike() && !staffVisible) {
            throw new IllegalStateException("You do not have access to this marriage case.");
        }
        return marriageCase;
    }

    private MarriagePartyEntity resolveParty(MarriageCaseEntity marriageCase, MarriagePartyRole partyRole) {
        return marriagePartyRepository.findByMarriageCaseIdAndPartyRole(marriageCase.getId(), partyRole)
                .orElseThrow(() -> new IllegalStateException("Marriage party not found for role " + partyRole));
    }

    private void ensurePartyEditableByCurrentUser(MarriagePartyEntity party, UserEntity currentUser) {
        boolean owner = currentUser.getUuid().equals(party.getLinkedUserId());
        if (!owner && !marriageSecuritySupport.isAdminLike()) {
            throw new IllegalStateException("You cannot edit this party.");
        }
        if (!party.isEditable() && !marriageSecuritySupport.isAdminLike()) {
            throw new IllegalStateException("This party is no longer editable.");
        }
    }

    private void ensureAdminLike() {
        if (!marriageSecuritySupport.isAdminLike()) {
            throw new IllegalStateException("Admin access is required for this operation.");
        }
    }

    private ChurchEntity resolveChurch(String churchNumber) {
        String normalized = churchNumber == null ? null : churchNumber.replace("\"", "").trim();
        return churchRepository.findByChurchNumber(normalized)
                .orElseThrow(() -> new IllegalStateException("Church not found for number: " + normalized));
    }

    private UUID resolveTenantId(ChurchEntity church) {
        UUID tenantId = church.getTenant() != null ? church.getTenant().getId() : TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is required for marriage case operations.");
        }
        return tenantId;
    }

    private void updateCaseStatus(MarriageCaseEntity marriageCase, MarriageCaseStatus toStatus, String reason, UUID actorUserId) {
        MarriageCaseStatus fromStatus = marriageCase.getStatus();
        marriageCaseDomainValidator.validateStatusTransition(fromStatus, toStatus);
        marriageCase.setStatus(toStatus);
        marriageCaseRepository.save(marriageCase);
        writeStatusHistory(marriageCase, fromStatus, toStatus, reason, actorUserId);
    }

    private void writeStatusHistory(MarriageCaseEntity marriageCase, MarriageCaseStatus from, MarriageCaseStatus to, String reason, UUID actorUserId) {
        marriageStatusHistoryRepository.save(MarriageStatusHistoryEntity.builder()
                .marriageCase(marriageCase)
                .fromStatus(from)
                .toStatus(to)
                .changeReason(reason)
                .changedByUserId(actorUserId)
                .changedAt(Instant.now())
                .build());
    }

    private void writeAuditEvent(MarriageCaseEntity marriageCase, MarriageCaseAuditEventType eventType, UUID actorUserId, UUID relatedPartyId, String summary) {
        marriageAuditEventRepository.save(MarriageAuditEventEntity.builder()
                .marriageCase(marriageCase)
                .eventType(eventType)
                .actorUserId(actorUserId)
                .relatedPartyId(relatedPartyId)
                .summary(summary)
                .occurredAt(Instant.now())
                .build());
    }

    private MarriagePartySubmissionEntity latestSubmission(MarriagePartyEntity party) {
        return marriagePartySubmissionRepository.findByPartyIdOrderBySubmissionVersionDesc(party.getId())
                .stream()
                .max(Comparator.comparingInt(MarriagePartySubmissionEntity::getSubmissionVersion))
                .orElse(null);
    }

    private void recordReview(
            MarriageCaseEntity marriageCase,
            MarriageReviewStage stage,
            MarriageReviewDecision decision,
            UUID actorUserId,
            String actorRole,
            String reason,
            String notes,
            MarriageNoteVisibility visibility
    ) {
        marriageReviewRepository.save(MarriageReviewEntity.builder()
                .marriageCase(marriageCase)
                .stage(stage)
                .decision(decision)
                .actorUserId(actorUserId)
                .actorRole(actorRole)
                .reason(reason)
                .notes(trimToNull(notes))
                .visibility(visibility)
                .reviewedAt(Instant.now())
                .build());
    }

    private void requireCurrentStatus(MarriageCaseEntity marriageCase, MarriageCaseStatus expectedStatus) {
        if (marriageCase.getStatus() != expectedStatus) {
            throw new IllegalStateException("Marriage case must be in status " + expectedStatus + " but was " + marriageCase.getStatus());
        }
    }

    private void ensureSecretaryLike() {
        if (!marriageSecuritySupport.isSecretaryLike()) {
            throw new IllegalStateException("Secretary or admin access is required for this operation.");
        }
    }

    private void ensureSecretaryViewer() {
        if (!marriageSecuritySupport.isSecretaryLike() && !marriageSecuritySupport.hasAnyRole("PRIEST")) {
            throw new IllegalStateException("Staff, priest, or admin access is required for this operation.");
        }
    }

    private boolean mayCreateNoteVisibility(MarriageNoteVisibility visibility) {
        if (marriageSecuritySupport.isSecretaryLike()) {
            return true;
        }
        if (marriageSecuritySupport.hasAnyRole("PRIEST")) {
            return visibility != MarriageNoteVisibility.INTERNAL_ADMIN;
        }
        return visibility == MarriageNoteVisibility.APPLICANT_VISIBLE;
    }

    private String resolveActorRole(UserEntity user) {
        return user.getRoles().stream()
                .findFirst()
                .map(role -> role.getRoleName())
                .orElse("USER");
    }

    private MarriageCaseResponse toCaseResponse(MarriageCaseEntity marriageCase) {
        List<MarriagePartySummaryResponse> parties = marriagePartyRepository.findByMarriageCaseId(marriageCase.getId())
                .stream()
                .map(this::toPartySummary)
                .toList();
        return new MarriageCaseResponse(
                marriageCase.getId(),
                marriageCase.getCaseReference(),
                marriageCase.getStatus(),
                marriageCase.getOriginType(),
                marriageCase.getPairingMode(),
                marriageCase.getPrimaryLanguage(),
                marriageCase.getTenantId(),
                marriageCase.getChurchId(),
                marriageCase.getChurch().getChurchNumber(),
                marriageCase.getChurch().getChurchNameLocal(),
                marriageCase.isBothSubmitted(),
                marriageCase.isSecretaryClearanceComplete(),
                marriageCase.isAdminApprovalGranted(),
                marriageCase.isConfessorGateSatisfied(),
                marriageCase.isManualPaymentSatisfied(),
                marriageCase.isReadyForScheduling(),
                marriageCase.isCeremonyCompleted(),
                marriageCase.isCertificateIssued(),
                marriageCase.getCreatedAt(),
                parties
        );
    }

    private MarriagePartySummaryResponse toPartySummary(MarriagePartyEntity party) {
        return new MarriagePartySummaryResponse(
                party.getId(),
                party.getPartyRole(),
                party.isCounterpartPlaceholder(),
                party.isExternalApplicant(),
                party.isSubmitted(),
                party.getLatestSubmissionStatus(),
                party.getLinkedUserId(),
                party.getMemberId(),
                party.getFullLegalNameEnglish(),
                party.getFullLegalNameLocal(),
                party.getDateOfBirth(),
                party.getMaritalStatus(),
                party.getSubmittedAt()
        );
    }

    private MarriagePartyApplicationResponse toPartyApplicationResponse(MarriagePartyEntity party, MarriagePartySubmissionEntity latest) {
        return new MarriagePartyApplicationResponse(
                party.getId(),
                party.getPartyRole(),
                party.isSubmitted(),
                party.isEditable(),
                party.getLatestSubmissionStatus(),
                latest == null ? null : latest.getSubmissionVersion(),
                party.getSubmittedAt(),
                latest == null ? null : parseJson(latest.getApplicationSnapshotJson())
        );
    }

    private MarriageDocumentResponse toDocumentResponse(MarriagePartyDocumentEntity document) {
        return new MarriageDocumentResponse(
                document.getId(),
                document.getParty() == null ? null : document.getParty().getId(),
                document.getDocumentCategory(),
                document.getOriginalFileName(),
                document.getStorageReference(),
                document.getContentType(),
                document.getVerificationStatus(),
                document.getExpiryDate(),
                document.getDocumentNumber(),
                document.getNotes(),
                document.getUploadedByUserId(),
                document.getUploadedAt()
        );
    }

    private MarriageCaseNoteResponse toCaseNoteResponse(MarriageCaseNoteEntity note) {
        return new MarriageCaseNoteResponse(
                note.getId(),
                note.getParty() == null ? null : note.getParty().getId(),
                note.getAuthorUserId(),
                note.getNoteType(),
                note.getVisibility(),
                note.getContent(),
                note.getCreatedAt()
        );
    }

    private MarriageStatusHistoryResponse toStatusHistoryResponse(MarriageStatusHistoryEntity history) {
        return new MarriageStatusHistoryResponse(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getChangeReason(),
                history.getChangedByUserId(),
                history.getChangedAt()
        );
    }

    private MarriageAuditEventResponse toAuditEventResponse(MarriageAuditEventEntity event) {
        return new MarriageAuditEventResponse(
                event.getId(),
                event.getEventType(),
                event.getActorUserId(),
                event.getRelatedPartyId(),
                event.getSummary(),
                event.getOccurredAt()
        );
    }

    private MarriageReviewResponse toReviewResponse(MarriageReviewEntity review) {
        return new MarriageReviewResponse(
                review.getId(),
                review.getStage(),
                review.getDecision(),
                review.getActorUserId(),
                review.getActorRole(),
                review.getReason(),
                review.getNotes(),
                review.getVisibility(),
                review.getReviewedAt()
        );
    }

    private MarriagePairingTokenResponse toPairingTokenResponse(MarriagePairingTokenEntity token) {
        return new MarriagePairingTokenResponse(
                token.getId(),
                token.getMarriageCase().getId(),
                token.getTargetParty().getPartyRole(),
                token.getTokenValue(),
                token.getInviteEmail(),
                token.getExpiresAt(),
                token.isActive()
        );
    }

    private MarriageConfessorApprovalResponse toConfessorApprovalResponse(MarriageConfessorApprovalEntity approval) {
        return new MarriageConfessorApprovalResponse(
                approval.getId(),
                approval.getParty() == null ? null : approval.getParty().getId(),
                approval.getApprovalStatus(),
                approval.getApprovalMode(),
                approval.getPriestUserId(),
                approval.getPriestPersonName(),
                approval.getChurchName(),
                approval.getDioceseName(),
                approval.getApprovalDate(),
                approval.getEvidenceDocumentId(),
                approval.getNotes(),
                approval.isBlocking(),
                approval.getOverrideReason(),
                approval.getOverrideDocumentId()
        );
    }

    private MarriageImpedimentResponse toImpedimentResponse(MarriageImpedimentEntity impediment) {
        return new MarriageImpedimentResponse(
                impediment.getId(),
                impediment.getParty() == null ? null : impediment.getParty().getId(),
                impediment.getImpedimentType(),
                impediment.getSeverity(),
                impediment.getSourceStage(),
                impediment.isBlocking(),
                impediment.getStatus(),
                impediment.getCreatedByUserId(),
                impediment.getResolvedByUserId(),
                impediment.getEvidenceNote()
        );
    }

    private MarriageManualPaymentResponse toPaymentResponse(MarriageManualPaymentEntity payment) {
        return new MarriageManualPaymentResponse(
                payment.getId(),
                payment.getPaymentCategory(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getReceiptReferenceNumber(),
                payment.getReceivedByUserId(),
                payment.getReceivedDate(),
                payment.getVerificationStatus(),
                payment.getNote()
        );
    }

    private MarriageWitnessResponse toWitnessResponse(MarriageWitnessEntity witness) {
        return new MarriageWitnessResponse(
                witness.getId(),
                witness.getParty() == null ? null : witness.getParty().getId(),
                witness.getWitnessType(),
                witness.getNameEnglish(),
                witness.getNameLocal(),
                witness.getRelationshipToParty(),
                witness.getPhone(),
                witness.getEmail(),
                witness.getAddressLine(),
                witness.getIdType(),
                witness.getIdNumber(),
                witness.getIdDocumentReference(),
                witness.isTestimonyCompleted(),
                witness.getTestimonyDate(),
                witness.getVerifiedByUserId(),
                witness.getNotes(),
                witness.getSortOrder()
        );
    }

    private MarriagePriestAssignmentResponse toPriestAssignmentResponse(MarriagePriestAssignmentEntity assignment) {
        return new MarriagePriestAssignmentResponse(
                assignment.getId(),
                assignment.getPriestUserId(),
                assignment.getPriestNameSnapshot(),
                assignment.getAssignedAt(),
                assignment.getAssignedByUserId(),
                assignment.isActive(),
                assignment.getAssignmentNote()
        );
    }

    private MarriageScheduleResponse toScheduleResponse(MarriageScheduleEntity schedule) {
        return new MarriageScheduleResponse(
                schedule.getId(),
                schedule.getProposedDateTime(),
                schedule.getApprovedDateTime(),
                schedule.getPlaceLabel(),
                schedule.getAdminCalendarEventId(),
                schedule.getPriestCalendarEventId(),
                schedule.getScheduleStatus(),
                schedule.getRescheduleCount(),
                schedule.getAssignedPriestUserId(),
                schedule.getSchedulingNote()
        );
    }

    private MarriageCertificateSequenceConfigResponse toSequenceConfigResponse(MarriageCertificateSequenceConfigEntity config) {
        return new MarriageCertificateSequenceConfigResponse(
                config.getId(),
                config.getChurch().getChurchNumber(),
                config.getPrefix(),
                config.getSeparator(),
                config.getCurrentNumber(),
                config.getStartingSeed(),
                config.getResetMode(),
                config.getFormatMask(),
                config.getMigrationReference(),
                config.isActive()
        );
    }

    private MarriageCertificateResponse toCertificateResponse(MarriageCertificateEntity certificate) {
        return new MarriageCertificateResponse(
                certificate.getId(),
                certificate.getMarriageCase().getId(),
                certificate.getCertificateNumber(),
                certificate.getNumberingFormatSnapshot(),
                certificate.getIssuedDate(),
                certificate.getIssuedByUserId(),
                parseJson(certificate.getLockedSnapshotJson()),
                certificate.getPrintCount(),
                certificate.getRegistryReference(),
                certificate.getStatus(),
                certificate.isHasAmendment()
        );
    }

    private MarriageCertificateAmendmentResponse toCertificateAmendmentResponse(MarriageCertificateAmendmentEntity amendment) {
        return new MarriageCertificateAmendmentResponse(
                amendment.getId(),
                amendment.getCertificate().getId(),
                amendment.getAmendmentReason(),
                amendment.getAmendmentSnapshotJson(),
                amendment.getAmendedByUserId(),
                amendment.getAmendedAt()
        );
    }

    private MarriageWitnessEntity applyWitnessRequest(
            MarriageWitnessEntity witness,
            MarriageCaseEntity marriageCase,
            MarriageWitnessUpsertRequest request,
            UUID verifierUserId
    ) {
        witness.setMarriageCase(marriageCase);
        witness.setParty(request.partyRole() == null ? null : resolveParty(marriageCase, request.partyRole()));
        witness.setWitnessType(request.witnessType());
        witness.setNameEnglish(request.nameEnglish().trim());
        witness.setNameLocal(trimToNull(request.nameLocal()));
        witness.setRelationshipToParty(trimToNull(request.relationshipToParty()));
        witness.setPhone(trimToNull(request.phone()));
        witness.setEmail(trimToNull(request.email()));
        witness.setAddressLine(trimToNull(request.addressLine()));
        witness.setIdType(trimToNull(request.idType()));
        witness.setIdNumber(trimToNull(request.idNumber()));
        witness.setIdDocumentReference(trimToNull(request.idDocumentReference()));
        witness.setTestimonyCompleted(request.testimonyCompleted());
        witness.setTestimonyDate(request.testimonyDate());
        witness.setVerifiedByUserId(request.testimonyCompleted() ? verifierUserId : null);
        witness.setNotes(trimToNull(request.notes()));
        witness.setSortOrder(request.sortOrder());
        return witness;
    }

    private UUID requireAssignedPriest(MarriageCaseEntity marriageCase) {
        MarriagePriestAssignmentEntity assignment = marriagePriestAssignmentRepository.findFirstByMarriageCaseIdAndActiveTrue(marriageCase.getId())
                .orElseThrow(() -> new IllegalStateException("A priest must be assigned before scheduling."));
        return assignment.getPriestUserId();
    }

    private MarriageScheduleEntity findOrCreateSchedule(MarriageCaseEntity marriageCase) {
        return marriageScheduleRepository.findByMarriageCaseId(marriageCase.getId())
                .stream()
                .findFirst()
                .orElseGet(() -> MarriageScheduleEntity.builder()
                        .marriageCase(marriageCase)
                        .scheduleStatus(MarriageScheduleStatus.DRAFT)
                        .build());
    }

    private void syncCalendarEntries(MarriageCaseEntity marriageCase, MarriageScheduleEntity schedule, UUID actorUserId, String timezone) {
        String effectiveTimezone = StringUtils.hasText(timezone) ? timezone.trim() : marriageCase.getChurch().getTimezone();
        String title = "Marriage Ceremony: " + displayPartyNames(marriageCase);
        String description = "Marriage case " + marriageCase.getCaseReference() + " at " + schedule.getPlaceLabel();

        CalendarEntryRequest adminRequest = new CalendarEntryRequest(
                CalendarEntryType.SACRAMENT,
                title,
                description,
                CalendarSystem.GREGORIAN,
                schedule.getApprovedDateTime(),
                schedule.getApprovedDateTime().plusSeconds(7200),
                effectiveTimezone,
                false,
                CalendarVisibility.STAFF,
                Set.of(CalendarCategory.SACRAMENTS),
                null,
                null,
                null
        );

        CalendarEntryResponse adminResponse = schedule.getAdminCalendarEventId() == null
                ? calendarEntryService.createEntry(adminRequest, actorUserId)
                : calendarEntryService.updateEntry(schedule.getAdminCalendarEventId(), adminRequest, actorUserId);
        schedule.setAdminCalendarEventId(adminResponse.entryId());

        if (schedule.getAssignedPriestUserId() != null) {
            CalendarEntryRequest priestRequest = new CalendarEntryRequest(
                    CalendarEntryType.SACRAMENT,
                    title,
                    description,
                    CalendarSystem.GREGORIAN,
                    schedule.getApprovedDateTime(),
                    schedule.getApprovedDateTime().plusSeconds(7200),
                    effectiveTimezone,
                    false,
                    CalendarVisibility.PRIEST_ONLY,
                    Set.of(CalendarCategory.SACRAMENTS),
                    null,
                    null,
                    null
            );

            CalendarEntryResponse priestResponse = schedule.getPriestCalendarEventId() == null
                    ? calendarEntryService.createEntry(priestRequest, schedule.getAssignedPriestUserId())
                    : calendarEntryService.updateEntry(schedule.getPriestCalendarEventId(), priestRequest, schedule.getAssignedPriestUserId());
            schedule.setPriestCalendarEventId(priestResponse.entryId());
        }
        marriageScheduleRepository.save(schedule);
    }

    private void cancelCalendarEntries(MarriageScheduleEntity schedule) {
        cancelCalendarEntry(schedule.getAdminCalendarEventId());
        cancelCalendarEntry(schedule.getPriestCalendarEventId());
    }

    private void cancelCalendarEntry(UUID entryId) {
        if (entryId == null) {
            return;
        }
        CalendarEntryEntity entry = calendarEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalStateException("Calendar entry not found for marriage schedule."));
        entry.setStatus(CalendarEntryStatus.CANCELED);
        entry.setCanceledAt(Instant.now());
        entry.setStatusChangedAt(Instant.now());
        calendarEntryRepository.save(entry);
    }

    private String buildCertificateSnapshot(MarriageCaseEntity marriageCase, String registryReference) {
        List<MarriagePartyEntity> parties = marriagePartyRepository.findByMarriageCaseId(marriageCase.getId());
        MarriageScheduleEntity schedule = marriageScheduleRepository.findByMarriageCaseId(marriageCase.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Marriage schedule not found."));
        MarriagePriestAssignmentEntity assignment = marriagePriestAssignmentRepository.findFirstByMarriageCaseIdAndActiveTrue(marriageCase.getId())
                .orElse(null);
        List<MarriageWitnessEntity> signatories = marriageWitnessRepository.findByMarriageCaseIdAndWitnessTypeOrderBySortOrderAsc(
                marriageCase.getId(),
                MarriageWitnessType.CEREMONY_SIGNATORY
        );

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("caseReference", marriageCase.getCaseReference());
        payload.put("churchName", marriageCase.getChurch().getChurchNameLocal());
        payload.put("primaryLanguage", marriageCase.getPrimaryLanguage().name());
        payload.put("bride", certificatePartyMap(parties, MarriagePartyRole.BRIDE));
        payload.put("groom", certificatePartyMap(parties, MarriagePartyRole.GROOM));
        payload.put("ceremonyDate", schedule.getApprovedDateTime() == null ? null : schedule.getApprovedDateTime().atOffset(ZoneOffset.UTC).toString());
        payload.put("placeLabel", schedule.getPlaceLabel());
        payload.put("officiatingPriest", assignment == null ? null : assignment.getPriestNameSnapshot());
        payload.put("registryReference", trimToNull(registryReference));
        payload.put("signatories", signatories.stream().map(witness -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("nameEnglish", witness.getNameEnglish());
            map.put("nameLocal", witness.getNameLocal());
            map.put("idNumber", witness.getIdNumber());
            return map;
        }).toList());

        return toJson(payload);
    }

    private Map<String, Object> certificatePartyMap(List<MarriagePartyEntity> parties, MarriagePartyRole role) {
        MarriagePartyEntity party = parties.stream()
                .filter(candidate -> candidate.getPartyRole() == role)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Marriage party not found for role " + role));

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("fullLegalNameEnglish", party.getFullLegalNameEnglish());
        map.put("fullLegalNameLocal", party.getFullLegalNameLocal());
        map.put("dateOfBirth", party.getDateOfBirth() == null ? null : party.getDateOfBirth().toString());
        map.put("maritalStatus", party.getMaritalStatus());
        return map;
    }

    private String formatCertificateNumber(MarriageCertificateSequenceConfigEntity config, long nextNumber) {
        String prefix = trimToNull(config.getPrefix());
        String separator = trimToNull(config.getSeparator()) == null ? "-" : config.getSeparator();
        String formatMask = config.getFormatMask();
        String numericPart = String.format(Locale.ROOT, "%03d", nextNumber);

        if (StringUtils.hasText(formatMask) && formatMask.contains("{number}")) {
            return formatMask
                    .replace("{prefix}", prefix == null ? "" : prefix)
                    .replace("{separator}", separator)
                    .replace("{number}", numericPart);
        }

        if (prefix == null) {
            return numericPart;
        }
        return prefix + separator + numericPart;
    }

    private String displayPartyNames(MarriageCaseEntity marriageCase) {
        List<MarriagePartyEntity> parties = marriagePartyRepository.findByMarriageCaseId(marriageCase.getId());
        String brideName = parties.stream()
                .filter(party -> party.getPartyRole() == MarriagePartyRole.BRIDE)
                .map(MarriagePartyEntity::getFullLegalNameEnglish)
                .findFirst()
                .orElse("Bride");
        String groomName = parties.stream()
                .filter(party -> party.getPartyRole() == MarriagePartyRole.GROOM)
                .map(MarriagePartyEntity::getFullLegalNameEnglish)
                .findFirst()
                .orElse("Groom");
        return brideName + " & " + groomName;
    }

    private JsonNode parseJson(String raw) {
        try {
            return objectMapper.readTree(raw);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to read party application snapshot.", ex);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize party application snapshot.", ex);
        }
    }

    private MarriagePartyRole counterpartRoleOf(MarriagePartyRole role) {
        return role == MarriagePartyRole.BRIDE ? MarriagePartyRole.GROOM : MarriagePartyRole.BRIDE;
    }

    private boolean isDirectPairing(MarriageMemberInitiationRequest request) {
        return StringUtils.hasText(request.counterpartEmail());
    }

    private String generatePairingToken() {
        return "MARRIAGE-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
    }

    private String normalizeCode(String value) {
        return value.trim().replace(' ', '_').toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
