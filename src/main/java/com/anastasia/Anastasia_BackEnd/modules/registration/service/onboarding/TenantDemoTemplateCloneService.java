package com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding;

import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentAssignmentEntity;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentEntity;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentParticipantEntity;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentStatusHistoryEntity;
import com.anastasia.Anastasia_BackEnd.modules.appointments.repository.AppointmentRepository;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryAudienceEntity;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryEntity;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarOccurrenceOverrideEntity;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarRecurrenceEntity;
import com.anastasia.Anastasia_BackEnd.modules.calendar.repository.CalendarEntryRepository;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventManagerEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.repository.EventRepository;
import com.anastasia.Anastasia_BackEnd.modules.groups.GroupRepository;
import com.anastasia.Anastasia_BackEnd.modules.groups.model.GroupEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.common.Address;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChildRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.PriestRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.staff.model.StaffEntity;
import com.anastasia.Anastasia_BackEnd.modules.staff.repository.StaffRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantDemoTemplateCloneService {

    private static final String CLONE_SOURCE = "DEMO_TEMPLATE_CLONE";
    private static final String DEMO_PHONE_COUNTRY_CODE = "+291";

    private final TenantRepository tenantRepository;
    private final ChurchRepository churchRepository;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final ChildRepository childRepository;
    private final PriestRepository priestRepository;
    private final StaffRepository staffRepository;
    private final GroupRepository groupRepository;
    private final EventRepository eventRepository;
    private final CalendarEntryRepository calendarEntryRepository;
    private final AppointmentRepository appointmentRepository;
    private final LocalizedMessageService messageService;

    @Transactional(readOnly = true)
    public boolean hasConfiguredTemplate() {
        return tenantRepository.findFirstByDemoTemplateTrueAndDeletedAtIsNull().isPresent();
    }

    @Transactional(readOnly = true)
    public Optional<TenantEntity> findConfiguredTemplate() {
        return tenantRepository.findFirstByDemoTemplateTrueAndDeletedAtIsNull();
    }

    @Transactional
    public TenantEntity configureTemplateTenant(UUID tenantId) {
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException(messageService.get(
                        "tenant.demoTemplate.notFound",
                        "Tenant not found."
                )));
        if (tenant.getDeletedAt() != null) {
            throw new IllegalStateException(messageService.get(
                    "tenant.demoTemplate.deleted",
                    "Deleted tenants cannot be used as a demo template."
            ));
        }
        if (tenant.getStatus() == TenantStatus.DRAFT) {
            throw new IllegalStateException(messageService.get(
                    "tenant.demoTemplate.draft",
                    "Draft tenants cannot be used as a demo template."
            ));
        }
        if (churchRepository.findByTenantId(tenantId).isEmpty()) {
            throw new IllegalStateException(messageService.get(
                    "tenant.demoTemplate.churchMissing",
                    "Demo template tenant is missing a church."
            ));
        }

        tenantRepository.clearDemoTemplateFlags();
        tenant.setDemoTemplate(true);
        return tenantRepository.save(tenant);
    }

    @Transactional
    public void clearConfiguredTemplate(UUID tenantId) {
        tenantRepository.findById(tenantId)
                .filter(TenantEntity::isDemoTemplate)
                .ifPresent(tenant -> {
                    tenant.setDemoTemplate(false);
                    tenantRepository.save(tenant);
                });
    }

    @Transactional
    public boolean cloneWorkspaceFromConfiguredTemplate(TenantEntity targetTenant, UserEntity targetOwner) {
        Optional<TenantEntity> templateTenantOptional = tenantRepository.findFirstByDemoTemplateTrueAndDeletedAtIsNull();
        if (templateTenantOptional.isEmpty()) {
            return false;
        }

        TenantEntity templateTenant = templateTenantOptional.get();
        if (templateTenant.getId().equals(targetTenant.getId())) {
            throw new IllegalStateException(messageService.get(
                    "tenant.demoTemplate.clone.self",
                    "Cannot clone demo data from the same tenant."
            ));
        }

        ChurchEntity templateChurch = churchRepository.findByTenantId(templateTenant.getId())
                .orElseThrow(() -> new IllegalStateException(messageService.get(
                        "tenant.demoTemplate.churchMissing",
                        "Demo template tenant is missing a church."
                )));

        ChurchEntity targetChurch = churchRepository.findByTenantId(targetTenant.getId())
                .orElseThrow(() -> new IllegalStateException(messageService.get(
                        "tenant.demoTarget.churchMissing",
                        "Target tenant is missing a church."
                )));

        UserEntity templateOwner = userRepository.findByEmail(templateTenant.getOwnerEmail()).orElse(null);
        CloneContext context = new CloneContext(templateTenant, templateChurch, templateOwner, targetTenant, targetChurch, targetOwner);

        clonePriests(context);
        cloneStaff(context);
        cloneAdultMembers(context);
        cloneChildMembers(context);
        cloneGroups(context);
        cloneEvents(context);
        cloneCalendarEntries(context);
        cloneAppointments(context);

        log.info("Cloned demo template tenant {} into target tenant {}.", templateTenant.getId(), targetTenant.getId());
        return true;
    }

    private void clonePriests(CloneContext context) {
        List<PriestEntity> sourcePriests = priestRepository.findByChurch_ChurchId(context.templateChurch.getChurchId());
        for (int index = 0; index < sourcePriests.size(); index++) {
            PriestEntity source = sourcePriests.get(index);
            UserEntity clonedUser = cloneOrMapUser(source.getUser(), context, "priest", index);

            PriestEntity clone = PriestEntity.builder()
                    .priestNumber(buildGeneratedCode("P", context.targetTenant.getId(), index + 1))
                    .user(clonedUser)
                    .church(context.targetChurch)
                    .churchNumber(context.targetChurch.getChurchNumber())
                    .tenant(source.getTenant() != null ? context.targetTenant : null)
                    .status(source.getStatus())
                    .avatar(cloneImageAsset(source.getAvatar(), context.targetTenant.getId()))
                    .spiritualChildren(source.getSpiritualChildren())
                    .prefixes(source.getPrefixes())
                    .firstName(source.getFirstName())
                    .fatherName(source.getFatherName())
                    .grandFatherName(source.getGrandFatherName())
                    .phoneNumber(buildDemoPhoneNumber(context.targetTenant.getId(), "73", index + 20))
                    .churchEmail(buildScopedEmail(source.getChurchEmail(), context.targetTenant.getId(), "priest", index))
                    .priesthoodCardId(source.getPriesthoodCardId())
                    .priesthoodCardScan(source.getPriesthoodCardScan())
                    .birthdate(source.getBirthdate())
                    .languages(source.getLanguages() == null ? new HashSet<>() : new HashSet<>(source.getLanguages()))
                    .levelOfEducation(source.getLevelOfEducation())
                    .address(cloneAddress(source.getAddress()))
                    .isActive(source.isActive())
                    .build();
            applyAuditableDefaults(clone, context, context.targetTenant.getActivatedAt(), context.targetTenant.getActivatedAt());
            PriestEntity saved = priestRepository.save(clone);
            context.priestById.put(source.getId(), saved);
            context.priestNumberMap.put(source.getPriestNumber(), saved.getPriestNumber());
        }
    }

    private void cloneStaff(CloneContext context) {
        List<StaffEntity> sourceStaff = staffRepository.findByTenant_Id(context.templateTenant.getId());
        Map<Long, StaffEntity> pendingReportsTo = new HashMap<>();

        for (int index = 0; index < sourceStaff.size(); index++) {
            StaffEntity source = sourceStaff.get(index);
            UserEntity clonedUser = cloneOrMapUser(source.getUser(), context, "staff", index);

            StaffEntity clone = StaffEntity.builder()
                    .staffNumber(buildGeneratedCode("S", context.targetTenant.getId(), index + 1))
                    .tenant(context.targetTenant)
                    .church(context.targetChurch)
                    .churchNumber(context.targetChurch.getChurchNumber())
                    .user(clonedUser)
                    .positionType(source.getPositionType())
                    .employmentStatus(source.getEmploymentStatus())
                    .department(source.getDepartment())
                    .primaryPhone(buildDemoPhoneNumber(context.targetTenant.getId(), "74", index + 20))
                    .alternatePhone(source.getAlternatePhone())
                    .hireDate(source.getHireDate())
                    .endDate(source.getEndDate())
                    .notes(source.getNotes())
                    .invitedAt(shiftInstant(source.getInvitedAt(), context.timeOffset))
                    .inviteAcceptedAt(shiftInstant(source.getInviteAcceptedAt(), context.timeOffset))
                    .firstLoginAt(shiftInstant(source.getFirstLoginAt(), context.timeOffset))
                    .lastCredentialResetAt(shiftInstant(source.getLastCredentialResetAt(), context.timeOffset))
                    .deactivatedAt(shiftInstant(source.getDeactivatedAt(), context.timeOffset))
                    .build();
            applyAuditableDefaults(clone, context, source.getCreatedAt(), source.getUpdatedAt());
            StaffEntity saved = staffRepository.save(clone);
            context.staffById.put(source.getId(), saved);
            if (source.getReportsTo() != null) {
                pendingReportsTo.put(saved.getId(), source.getReportsTo());
            }
        }

        if (!pendingReportsTo.isEmpty()) {
            for (Map.Entry<Long, StaffEntity> entry : pendingReportsTo.entrySet()) {
                StaffEntity saved = staffRepository.findById(entry.getKey()).orElse(null);
                StaffEntity mappedManager = saved == null ? null : context.staffById.get(entry.getValue().getId());
                if (saved != null) {
                    saved.setReportsTo(mappedManager);
                    staffRepository.save(saved);
                }
            }
        }
    }

    private void cloneAdultMembers(CloneContext context) {
        List<Adult_MemberEntity> sourceAdults = memberRepository
                .findByTenantIdOrderByCreatedAtDesc(context.templateTenant.getId(), Pageable.unpaged())
                .getContent();
        for (int index = 0; index < sourceAdults.size(); index++) {
            Adult_MemberEntity source = sourceAdults.get(index);
            Adult_MemberEntity clone = Adult_MemberEntity.builder()
                    .tenantId(context.targetTenant.getId())
                    .membershipNumber(buildGeneratedCode("M", context.targetTenant.getId(), index + 1))
                    .churchNumber(context.targetChurch.getChurchNumber())
                    .statusValue(source.getStatusValue())
                    .deacon(source.isDeacon())
                    .avatar(cloneImageAsset(source.getAvatar(), context.targetTenant.getId()))
                    .title(source.getTitle())
                    .firstName(source.getFirstName())
                    .fatherName(source.getFatherName())
                    .grandFatherName(source.getGrandFatherName())
                    .motherName(source.getMotherName())
                    .mothersFather(source.getMothersFather())
                    .firstNameLocal(source.getFirstNameLocal())
                    .fatherNameLocal(source.getFatherNameLocal())
                    .grandFatherNameLocal(source.getGrandFatherNameLocal())
                    .motherFullNameLocal(source.getMotherFullNameLocal())
                    .genderValue(source.getGenderValue())
                    .birthday(source.getBirthday())
                    .nationality(source.getNationality())
                    .placeOfBirth(source.getPlaceOfBirth())
                    .village(source.getVillage())
                    .email(source.getEmail())
                    .phone(source.getPhone())
                    .whatsApp(source.getWhatsApp())
                    .emergencyContactNumber(source.getEmergencyContactNumber())
                    .contactRelation(source.getContactRelation())
                    .firstLanguage(source.getFirstLanguage())
                    .secondLanguage(source.getSecondLanguage())
                    .educationLevelValue(source.getEducationLevel())
                    .fatherOfConfession(source.getFatherOfConfession())
                    .churchOfBaptism(source.getChurchOfBaptism())
                    .baptismName(source.getBaptismName())
                    .priestNumber(mapPriestNumber(source.getPriestNumber(), context))
                    .address(cloneAddress(source.getAddress()))
                    .church(context.targetChurch)
                    .registeredAt(shiftLocalDateTime(source.getRegisteredAt(), context.timeOffset))
                    .approvedAt(shiftLocalDateTime(source.getApprovedAt(), context.timeOffset))
                    .inactiveAt(shiftLocalDateTime(source.getInactiveAt(), context.timeOffset))
                    .statusChangedAt(shiftLocalDateTime(source.getStatusChangedAt(), context.timeOffset))
                    .statusReason(source.getStatusReason())
                    .consentVersion(source.getConsentVersion())
                    .consentAcceptedAt(shiftLocalDateTime(source.getConsentAcceptedAt(), context.timeOffset))
                    .externalId(cloneExternalId(source.getExternalId(), context.targetTenant.getId(), index))
                    .sourceSystem(CLONE_SOURCE)
                    .preferredName(source.getPreferredName())
                    .deletedAt(shiftLocalDateTime(source.getDeletedAt(), context.timeOffset))
                    .churchApprovalStatus(source.getChurchApprovalStatus())
                    .priestApprovalStatus(source.getPriestApprovalStatus())
                    .churchApprovedAt(shiftLocalDateTime(source.getChurchApprovedAt(), context.timeOffset))
                    .churchApprovedBy(source.getChurchApprovedBy())
                    .priestApprovedAt(shiftLocalDateTime(source.getPriestApprovedAt(), context.timeOffset))
                    .priestApprovedBy(source.getPriestApprovedBy())
                    .termsAccepted(source.isTermsAccepted())
                    .termsVersion(source.getTermsVersion())
                    .termsAcceptedAt(shiftInstant(source.getTermsAcceptedAt(), context.timeOffset))
                    .eritreaContact(source.getEritreaContact())
                    .maritalStatus(source.getMaritalStatus())
                    .profession(source.getProfession())
                    .spouseIdNumber(source.getSpouseIdNumber())
                    .build();
            clone.setApprovedByChurch(source.isApprovedByChurch());
            clone.setApprovedByPriest(source.isApprovedByPriest());
            if (source.getUser() != null) {
                clone.setUser(cloneOrMapUser(source.getUser(), context, "member", index));
            }
            applyAuditableDefaults(clone, context, source.getCreatedAt(), source.getUpdatedAt());
            Adult_MemberEntity saved = memberRepository.save(clone);
            context.adultMemberById.put(source.getId(), saved);
        }
    }

    private void cloneChildMembers(CloneContext context) {
        List<Child_MemberEntity> sourceChildren = childRepository
                .findByTenantIdOrderByCreatedAtDesc(context.templateTenant.getId(), Pageable.unpaged())
                .getContent();
        for (int index = 0; index < sourceChildren.size(); index++) {
            Child_MemberEntity source = sourceChildren.get(index);
            Child_MemberEntity clone = Child_MemberEntity.builder()
                    .tenantId(context.targetTenant.getId())
                    .membershipNumber(buildGeneratedCode("C", context.targetTenant.getId(), index + 1))
                    .churchNumber(context.targetChurch.getChurchNumber())
                    .statusValue(source.getStatusValue())
                    .deacon(source.isDeacon())
                    .avatar(cloneImageAsset(source.getAvatar(), context.targetTenant.getId()))
                    .title(source.getTitle())
                    .firstName(source.getFirstName())
                    .fatherName(source.getFatherName())
                    .grandFatherName(source.getGrandFatherName())
                    .motherName(source.getMotherName())
                    .mothersFather(source.getMothersFather())
                    .firstNameLocal(source.getFirstNameLocal())
                    .fatherNameLocal(source.getFatherNameLocal())
                    .grandFatherNameLocal(source.getGrandFatherNameLocal())
                    .motherFullNameLocal(source.getMotherFullNameLocal())
                    .genderValue(source.getGenderValue())
                    .birthday(source.getBirthday())
                    .nationality(source.getNationality())
                    .placeOfBirth(source.getPlaceOfBirth())
                    .village(source.getVillage())
                    .email(source.getEmail())
                    .phone(source.getPhone())
                    .whatsApp(source.getWhatsApp())
                    .emergencyContactNumber(source.getEmergencyContactNumber())
                    .contactRelation(source.getContactRelation())
                    .firstLanguage(source.getFirstLanguage())
                    .secondLanguage(source.getSecondLanguage())
                    .educationLevelValue(source.getEducationLevel())
                    .fatherOfConfession(source.getFatherOfConfession())
                    .churchOfBaptism(source.getChurchOfBaptism())
                    .baptismName(source.getBaptismName())
                    .priestNumber(mapPriestNumber(source.getPriestNumber(), context))
                    .address(cloneAddress(source.getAddress()))
                    .church(context.targetChurch)
                    .registeredAt(shiftLocalDateTime(source.getRegisteredAt(), context.timeOffset))
                    .approvedAt(shiftLocalDateTime(source.getApprovedAt(), context.timeOffset))
                    .inactiveAt(shiftLocalDateTime(source.getInactiveAt(), context.timeOffset))
                    .statusChangedAt(shiftLocalDateTime(source.getStatusChangedAt(), context.timeOffset))
                    .statusReason(source.getStatusReason())
                    .consentVersion(source.getConsentVersion())
                    .consentAcceptedAt(shiftLocalDateTime(source.getConsentAcceptedAt(), context.timeOffset))
                    .externalId(cloneExternalId(source.getExternalId(), context.targetTenant.getId(), index))
                    .sourceSystem(CLONE_SOURCE)
                    .preferredName(source.getPreferredName())
                    .deletedAt(shiftLocalDateTime(source.getDeletedAt(), context.timeOffset))
                    .churchApprovalStatus(source.getChurchApprovalStatus())
                    .churchApprovedAt(shiftLocalDateTime(source.getChurchApprovedAt(), context.timeOffset))
                    .churchApprovedBy(source.getChurchApprovedBy())
                    .primaryGuardianPhone(source.getPrimaryGuardianPhone())
                    .guardianRelationship(source.getGuardianRelationship())
                    .father(source.getFather() == null ? null : context.adultMemberById.get(source.getFather().getId()))
                    .mother(source.getMother() == null ? null : context.adultMemberById.get(source.getMother().getId()))
                    .build();
            clone.setApprovedByChurch(source.isApprovedByChurch());
            if (source.getUser() != null) {
                clone.setUser(cloneOrMapUser(source.getUser(), context, "child-member", index));
            }
            applyAuditableDefaults(clone, context, source.getCreatedAt(), source.getUpdatedAt());
            Child_MemberEntity saved = childRepository.save(clone);
            context.childMemberById.put(source.getId(), saved);
        }
    }

    private void cloneGroups(CloneContext context) {
        List<GroupEntity> sourceGroups = groupRepository.findAllByTenantId(context.templateTenant.getId(), Pageable.unpaged()).getContent();
        for (int index = 0; index < sourceGroups.size(); index++) {
            GroupEntity source = sourceGroups.get(index);
            GroupEntity clone = GroupEntity.builder()
                    .tenantId(context.targetTenant.getId())
                    .church(context.targetChurch)
                    .groupName(source.getGroupName())
                    .description(source.getDescription())
                    .avatar(source.getAvatar())
                    .visibility(source.getVisibility())
                    .build();
            clone.setManagers(mapUsers(source.getManagers(), context));
            clone.setUsers(mapUsers(source.getUsers(), context));
            applyAuditableDefaults(clone, context, source.getCreatedAt(), source.getUpdatedAt());
            GroupEntity saved = groupRepository.save(clone);
            context.groupById.put(source.getGroupId(), saved);
        }
    }

    private void cloneEvents(CloneContext context) {
        List<EventEntity> sourceEvents = eventRepository.findByTenantId(context.templateTenant.getId());
        for (EventEntity source : sourceEvents) {
            EventEntity clone = EventEntity.builder()
                    .tenantId(context.targetTenant.getId())
                    .church(context.targetChurch)
                    .title(source.getTitle())
                    .description(source.getDescription())
                    .location(source.getLocation())
                    .gpsLocation(source.getGpsLocation())
                    .startAt(shiftInstant(source.getStartAt(), context.timeOffset))
                    .endAt(shiftInstant(source.getEndAt(), context.timeOffset))
                    .timezone(source.getTimezone())
                    .allDay(source.isAllDay())
                    .image(source.getImage())
                    .status(source.getStatus())
                    .canceledAt(shiftInstant(source.getCanceledAt(), context.timeOffset))
                    .statusChangedAt(shiftInstant(source.getStatusChangedAt(), context.timeOffset))
                    .type(source.getType())
                    .capacity(source.getCapacity())
                    .requiresRegistration(source.getRequiresRegistration())
                    .allowWaitlist(source.getAllowWaitlist())
                    .allowGeoCheckIn(source.getAllowGeoCheckIn())
                    .latitude(source.getLatitude())
                    .longitude(source.getLongitude())
                    .geofenceRadiusMeters(source.getGeofenceRadiusMeters())
                    .checkInOpensAt(shiftInstant(source.getCheckInOpensAt(), context.timeOffset))
                    .checkInClosesAt(shiftInstant(source.getCheckInClosesAt(), context.timeOffset))
                    .invitedGroups(mapGroups(source.getInvitedGroups(), context))
                    .invitedUsers(mapUsers(source.getInvitedUsers(), context))
                    .invitedEmails(source.getInvitedEmails() == null ? new HashSet<>() : new HashSet<>(source.getInvitedEmails()))
                    .visibility(source.getVisibility())
                    .repetition(source.getRepetition())
                    .build();
            applyAuditableDefaults(clone, context, source.getCreatedAt(), source.getUpdatedAt());
            if (source.getEventManagers() != null) {
                for (EventManagerEntity manager : source.getEventManagers()) {
                    UserEntity mappedUser = manager.getUser() == null ? null : cloneOrMapUser(manager.getUser(), context, "event-manager", context.userById.size());
                    if (mappedUser != null) {
                        clone.getEventManagers().add(EventManagerEntity.builder()
                                .event(clone)
                                .user(mappedUser)
                                .role(manager.getRole())
                                .build());
                    }
                }
            }
            EventEntity saved = eventRepository.save(clone);
            context.eventById.put(source.getEventId(), saved);
        }
    }

    private void cloneCalendarEntries(CloneContext context) {
        List<CalendarEntryEntity> sourceEntries = calendarEntryRepository.findByTenantIdOrderByStartAtUtcAsc(context.templateTenant.getId());
        for (CalendarEntryEntity source : sourceEntries) {
            CalendarEntryEntity clone = CalendarEntryEntity.builder()
                    .tenantId(context.targetTenant.getId())
                    .church(context.targetChurch)
                    .ownerUser(source.getOwnerUser() == null ? context.targetOwner : cloneOrMapUser(source.getOwnerUser(), context, "calendar-owner", context.userById.size()))
                    .type(source.getType())
                    .title(source.getTitle())
                    .description(source.getDescription())
                    .calendarSystem(source.getCalendarSystem())
                    .startAtUtc(shiftInstant(source.getStartAtUtc(), context.timeOffset))
                    .endAtUtc(shiftInstant(source.getEndAtUtc(), context.timeOffset))
                    .timezone(source.getTimezone())
                    .allDay(source.isAllDay())
                    .visibility(source.getVisibility())
                    .status(source.getStatus())
                    .canceledAt(shiftInstant(source.getCanceledAt(), context.timeOffset))
                    .statusChangedAt(shiftInstant(source.getStatusChangedAt(), context.timeOffset))
                    .sourceEntityType(source.getSourceEntityType())
                    .sourceEntityId(null)
                    .deletedAt(shiftInstant(source.getDeletedAt(), context.timeOffset))
                    .categories(source.getCategories() == null ? new HashSet<>() : new HashSet<>(source.getCategories()))
                    .build();
            applyAuditableDefaults(clone, context, source.getCreatedAt(), source.getUpdatedAt());

            if (source.getRecurrence() != null) {
                clone.setRecurrence(CalendarRecurrenceEntity.builder()
                        .entry(clone)
                        .frequency(source.getRecurrence().getFrequency())
                        .interval(source.getRecurrence().getInterval())
                        .byDay(source.getRecurrence().getByDay() == null ? new HashSet<>() : new HashSet<>(source.getRecurrence().getByDay()))
                        .byMonth(source.getRecurrence().getByMonth() == null ? new HashSet<>() : new HashSet<>(source.getRecurrence().getByMonth()))
                        .byMonthDay(source.getRecurrence().getByMonthDay() == null ? new HashSet<>() : new HashSet<>(source.getRecurrence().getByMonthDay()))
                        .until(shiftInstant(source.getRecurrence().getUntil(), context.timeOffset))
                        .count(source.getRecurrence().getCount())
                        .calendarSystem(source.getRecurrence().getCalendarSystem())
                        .geezMonth(source.getRecurrence().getGeezMonth())
                        .geezDay(source.getRecurrence().getGeezDay())
                        .build());
            }

            if (source.getOverrides() != null) {
                for (CalendarOccurrenceOverrideEntity override : source.getOverrides()) {
                    clone.getOverrides().add(CalendarOccurrenceOverrideEntity.builder()
                            .entry(clone)
                            .occurrenceDate(override.getOccurrenceDate())
                            .cancelled(override.isCancelled())
                            .titleOverride(override.getTitleOverride())
                            .startAtUtcOverride(shiftInstant(override.getStartAtUtcOverride(), context.timeOffset))
                            .endAtUtcOverride(shiftInstant(override.getEndAtUtcOverride(), context.timeOffset))
                            .notes(override.getNotes())
                            .build());
                }
            }

            if (source.getAudiences() != null) {
                for (CalendarEntryAudienceEntity audience : source.getAudiences()) {
                    clone.getAudiences().add(CalendarEntryAudienceEntity.builder()
                            .entry(clone)
                            .user(audience.getUser() == null ? null : cloneOrMapUser(audience.getUser(), context, "calendar-audience", context.userById.size()))
                            .group(audience.getGroup() == null ? null : context.groupById.get(audience.getGroup().getGroupId()))
                            .build());
                }
            }

            CalendarEntryEntity saved = calendarEntryRepository.save(clone);
            context.calendarEntryById.put(source.getId(), saved);
        }
    }

    private void cloneAppointments(CloneContext context) {
        List<AppointmentEntity> sourceAppointments = appointmentRepository.findByTenantIdOrderByStartAtUtcAsc(context.templateTenant.getId());
        for (AppointmentEntity source : sourceAppointments) {
            AppointmentEntity clone = AppointmentEntity.builder()
                    .tenantId(context.targetTenant.getId())
                    .church(context.targetChurch)
                    .calendarEntry(source.getCalendarEntry() == null ? null : context.calendarEntryById.get(source.getCalendarEntry().getId()))
                    .title(source.getTitle())
                    .description(source.getDescription())
                    .type(source.getType())
                    .status(source.getStatus())
                    .source(source.getSource())
                    .locationType(source.getLocationType())
                    .locationLabel(source.getLocationLabel())
                    .startAtUtc(shiftInstant(source.getStartAtUtc(), context.timeOffset))
                    .endAtUtc(shiftInstant(source.getEndAtUtc(), context.timeOffset))
                    .timezone(source.getTimezone())
                    .notesForMember(source.getNotesForMember())
                    .privateNotesExists(source.isPrivateNotesExists())
                    .contactPhone(source.getContactPhone())
                    .contactEmail(source.getContactEmail())
                    .contactPreference(source.getContactPreference())
                    .linkedRequestId(null)
                    .firstVisit(source.isFirstVisit())
                    .sacramentRelated(source.isSacramentRelated())
                    .requestedAt(shiftInstant(source.getRequestedAt(), context.timeOffset))
                    .confirmedAt(shiftInstant(source.getConfirmedAt(), context.timeOffset))
                    .completedAt(shiftInstant(source.getCompletedAt(), context.timeOffset))
                    .canceledAt(shiftInstant(source.getCanceledAt(), context.timeOffset))
                    .cancellationReason(source.getCancellationReason())
                    .outcomeNotes(source.getOutcomeNotes())
                    .deletedAt(shiftInstant(source.getDeletedAt(), context.timeOffset))
                    .build();
            applyAuditableDefaults(clone, context, source.getCreatedAt(), source.getUpdatedAt());

            if (source.getParticipants() != null) {
                for (AppointmentParticipantEntity participant : source.getParticipants()) {
                    clone.getParticipants().add(AppointmentParticipantEntity.builder()
                            .appointment(clone)
                            .memberId(mapMemberId(participant.getMemberId(), context))
                            .fullName(participant.getFullName())
                            .familyMember(participant.isFamilyMember())
                            .role(participant.getRole())
                            .build());
                }
            }

            if (source.getAssignments() != null) {
                for (AppointmentAssignmentEntity assignment : source.getAssignments()) {
                    clone.getAssignments().add(AppointmentAssignmentEntity.builder()
                            .appointment(clone)
                            .userId(mapUserId(assignment.getUserId(), context))
                            .role(assignment.getRole())
                            .build());
                }
            }

            if (source.getStatusHistory() != null) {
                for (AppointmentStatusHistoryEntity history : source.getStatusHistory()) {
                    clone.getStatusHistory().add(AppointmentStatusHistoryEntity.builder()
                            .appointment(clone)
                            .fromStatus(history.getFromStatus())
                            .toStatus(history.getToStatus())
                            .reason(history.getReason())
                            .changedByUserId(mapUserId(history.getChangedByUserId(), context))
                            .changedAt(shiftInstant(history.getChangedAt(), context.timeOffset))
                            .build());
                }
            }

            appointmentRepository.save(clone);
        }
    }

    private UserEntity cloneOrMapUser(UserEntity source, CloneContext context, String tag, int sequence) {
        if (source == null) {
            return null;
        }
        if (context.templateOwner != null && source.getUuid().equals(context.templateOwner.getUuid())) {
            return context.targetOwner;
        }
        UserEntity existing = context.userById.get(source.getUuid());
        if (existing != null) {
            return existing;
        }

        UserEntity clone = UserEntity.builder()
                .fullName(source.getFullName())
                .email(buildScopedEmail(source.getEmail(), context.targetTenant.getId(), tag, sequence))
                .password(source.getPassword())
                .googleId(null)
                .facebookId(null)
                .phoneNumber(buildDemoPhoneNumber(context.targetTenant.getId(), "79", context.userById.size() + sequence + 1))
                .emailVerifiedAt(shiftInstant(source.getEmailVerifiedAt(), context.timeOffset))
                .phoneVerifiedAt(shiftInstant(source.getPhoneVerifiedAt(), context.timeOffset))
                .lastLoginAt(shiftInstant(source.getLastLoginAt(), context.timeOffset))
                .lastPasswordChangedAt(shiftInstant(source.getLastPasswordChangedAt(), context.timeOffset))
                .mustChangePassword(source.isMustChangePassword())
                .temporaryPasswordIssuedAt(shiftInstant(source.getTemporaryPasswordIssuedAt(), context.timeOffset))
                .lockedAt(shiftInstant(source.getLockedAt(), context.timeOffset))
                .lockedUntil(shiftInstant(source.getLockedUntil(), context.timeOffset))
                .failedLoginAttempts(source.getFailedLoginAttempts())
                .deletedAt(shiftInstant(source.getDeletedAt(), context.timeOffset))
                .timezone(source.getTimezone())
                .priestNumber(null)
                .roles(source.getRoles() == null ? new HashSet<>() : new HashSet<>(source.getRoles()))
                .userType(source.getUserType())
                .status(source.getStatus())
                .affiliatedTenant(context.targetTenant)
                .createdBy(context.targetOwner.getUuid())
                .updatedBy(context.targetOwner.getUuid())
                .createdAt(resolveInstant(source.getCreatedAt(), context.targetTenant.getActivatedAt()))
                .updatedAt(resolveInstant(source.getUpdatedAt(), context.targetTenant.getActivatedAt()))
                .build();
        if (source.getProfileAvatar() != null) {
            clone.setProfileAvatar(cloneImageAsset(source.getProfileAvatar(), context.targetTenant.getId()));
        }
        UserEntity saved = userRepository.save(clone);
        context.userById.put(source.getUuid(), saved);
        return saved;
    }

    private Set<UserEntity> mapUsers(Set<UserEntity> sourceUsers, CloneContext context) {
        if (sourceUsers == null || sourceUsers.isEmpty()) {
            return new HashSet<>();
        }
        Set<UserEntity> result = new HashSet<>();
        int index = 0;
        for (UserEntity sourceUser : sourceUsers) {
            result.add(cloneOrMapUser(sourceUser, context, "group-user", index++));
        }
        return result;
    }

    private Set<GroupEntity> mapGroups(Set<GroupEntity> sourceGroups, CloneContext context) {
        if (sourceGroups == null || sourceGroups.isEmpty()) {
            return new HashSet<>();
        }
        Set<GroupEntity> result = new HashSet<>();
        for (GroupEntity group : sourceGroups) {
            GroupEntity mapped = context.groupById.get(group.getGroupId());
            if (mapped != null) {
                result.add(mapped);
            }
        }
        return result;
    }

    private Long mapMemberId(Long sourceMemberId, CloneContext context) {
        if (sourceMemberId == null) {
            return null;
        }
        Adult_MemberEntity adult = context.adultMemberById.get(sourceMemberId);
        if (adult != null) {
            return adult.getId();
        }
        Child_MemberEntity child = context.childMemberById.get(sourceMemberId);
        return child != null ? child.getId() : null;
    }

    private UUID mapUserId(UUID sourceUserId, CloneContext context) {
        if (sourceUserId == null) {
            return null;
        }
        if (context.templateOwner != null && sourceUserId.equals(context.templateOwner.getUuid())) {
            return context.targetOwner.getUuid();
        }
        UserEntity mapped = context.userById.get(sourceUserId);
        return mapped != null ? mapped.getUuid() : context.targetOwner.getUuid();
    }

    private String mapPriestNumber(String sourcePriestNumber, CloneContext context) {
        if (sourcePriestNumber == null || sourcePriestNumber.isBlank()) {
            return null;
        }
        return context.priestNumberMap.getOrDefault(sourcePriestNumber, sourcePriestNumber);
    }

    private Address cloneAddress(Address source) {
        if (source == null) {
            return null;
        }
        return Address.builder()
                .addressLine1(source.getAddressLine1())
                .addressLine2(source.getAddressLine2())
                .city(source.getCity())
                .stateProvince(source.getStateProvince())
                .country(source.getCountry())
                .postalCode(source.getPostalCode())
                .build();
    }

    private ImageAssetEntity cloneImageAsset(ImageAssetEntity source, UUID tenantId) {
        if (source == null) {
            return null;
        }
        return ImageAssetEntity.builder()
                .tenantId(tenantId)
                .ownerId(UUID.randomUUID())
                .imageAssetType(source.getImageAssetType())
                .storageProvider(source.getStorageProvider())
                .imageUrl(source.getImageUrl())
                .imageSize(source.getImageSize())
                .objectKey(source.getObjectKey())
                .originalFilename(source.getOriginalFilename())
                .contentType(source.getContentType())
                .fileSizeBytes(source.getFileSizeBytes())
                .width(source.getWidth())
                .height(source.getHeight())
                .checksum(source.getChecksum())
                .visibility(source.getVisibility())
                .uploadedByUserId(source.getUploadedByUserId())
                .uploadedAt(source.getUploadedAt())
                .deletedAt(source.getDeletedAt())
                .build();
    }

    private String buildScopedEmail(String sourceEmail, UUID tenantId, String tag, int sequence) {
        String normalized = (sourceEmail == null || sourceEmail.isBlank())
                ? tag + ".demo@anastasia.local"
                : sourceEmail.trim().toLowerCase(Locale.ROOT);
        int atIndex = normalized.indexOf('@');
        String localPart = atIndex > 0 ? normalized.substring(0, atIndex) : normalized;
        String sanitizedLocal = localPart.replaceAll("[^a-z0-9._+-]", "-");
        String tenantKey = tenantId.toString().substring(0, 8).toLowerCase(Locale.ROOT);
        return sanitizedLocal + "." + tenantKey + "." + Math.max(sequence, 0) + "@demo.anastasia.local";
    }

    private String buildGeneratedCode(String prefix, UUID tenantId, int sequence) {
        String tenantKey = tenantId.toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
        return prefix + tenantKey + String.format("%03d", sequence);
    }

    private String buildDemoPhoneNumber(UUID tenantId, String carrierPrefix, int sequence) {
        String tenantDigits = tenantId.toString().replace("-", "");
        int numeric = Math.floorMod(tenantDigits.hashCode(), 100_000);
        return DEMO_PHONE_COUNTRY_CODE + carrierPrefix + String.format("%05d%02d", numeric, Math.floorMod(sequence, 100));
    }

    private String cloneExternalId(String sourceExternalId, UUID tenantId, int index) {
        if (sourceExternalId == null || sourceExternalId.isBlank()) {
            return CLONE_SOURCE + ":" + tenantId + ":" + index;
        }
        return sourceExternalId + ":clone:" + tenantId.toString().substring(0, 8);
    }

    private Instant shiftInstant(Instant value, Duration offset) {
        return value == null ? null : value.plus(offset);
    }

    private LocalDateTime shiftLocalDateTime(LocalDateTime value, Duration offset) {
        if (value == null) {
            return null;
        }
        return LocalDateTime.ofInstant(value.toInstant(ZoneOffset.UTC).plus(offset), ZoneOffset.UTC);
    }

    private Instant resolveInstant(Instant preferred, Instant fallback) {
        return preferred != null ? preferred : fallback;
    }

    private void applyAuditableDefaults(Object entity, CloneContext context, Instant createdAt, Instant updatedAt) {
        if (entity instanceof com.anastasia.Anastasia_BackEnd.modules.common.Auditable auditable) {
            auditable.setCreatedBy(context.targetOwner.getUuid());
            auditable.setUpdatedBy(context.targetOwner.getUuid());
            auditable.setCreatedAt(resolveInstant(createdAt, context.targetTenant.getActivatedAt()));
            auditable.setUpdatedAt(resolveInstant(updatedAt, context.targetTenant.getActivatedAt()));
            return;
        }
        if (entity instanceof com.anastasia.Anastasia_BackEnd.modules.common.LocalDateTimeAuditMetadata metadata) {
            metadata.setCreatedAt(resolveInstant(createdAt, context.targetTenant.getActivatedAt()));
            metadata.setUpdatedAt(resolveInstant(updatedAt, context.targetTenant.getActivatedAt()));
        }
    }

    private static final class CloneContext {
        private final TenantEntity templateTenant;
        private final ChurchEntity templateChurch;
        private final UserEntity templateOwner;
        private final TenantEntity targetTenant;
        private final ChurchEntity targetChurch;
        private final UserEntity targetOwner;
        private final Duration timeOffset;
        private final Map<UUID, UserEntity> userById = new HashMap<>();
        private final Map<Long, PriestEntity> priestById = new HashMap<>();
        private final Map<String, String> priestNumberMap = new HashMap<>();
        private final Map<Long, StaffEntity> staffById = new HashMap<>();
        private final Map<Long, Adult_MemberEntity> adultMemberById = new HashMap<>();
        private final Map<Long, Child_MemberEntity> childMemberById = new HashMap<>();
        private final Map<Long, GroupEntity> groupById = new HashMap<>();
        private final Map<Long, EventEntity> eventById = new HashMap<>();
        private final Map<UUID, CalendarEntryEntity> calendarEntryById = new HashMap<>();

        private CloneContext(TenantEntity templateTenant,
                             ChurchEntity templateChurch,
                             UserEntity templateOwner,
                             TenantEntity targetTenant,
                             ChurchEntity targetChurch,
                             UserEntity targetOwner) {
            this.templateTenant = templateTenant;
            this.templateChurch = templateChurch;
            this.templateOwner = templateOwner;
            this.targetTenant = targetTenant;
            this.targetChurch = targetChurch;
            this.targetOwner = targetOwner;
            Instant templateBase = templateTenant.getActivatedAt() != null ? templateTenant.getActivatedAt() : Instant.now();
            Instant targetBase = targetTenant.getActivatedAt() != null ? targetTenant.getActivatedAt() : Instant.now();
            this.timeOffset = Duration.between(templateBase, targetBase);
        }
    }
}
