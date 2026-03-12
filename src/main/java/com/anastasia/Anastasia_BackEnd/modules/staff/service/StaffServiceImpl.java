package com.anastasia.Anastasia_BackEnd.modules.staff.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.common.utils.SecurityUtils;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.core.auth.role.RoleType;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.EmailNotificationService;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplateName;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.staff.dto.CreateStaffRequest;
import com.anastasia.Anastasia_BackEnd.modules.staff.dto.StaffResponse;
import com.anastasia.Anastasia_BackEnd.modules.staff.dto.UpdateStaffRequest;
import com.anastasia.Anastasia_BackEnd.modules.staff.model.StaffEmploymentStatus;
import com.anastasia.Anastasia_BackEnd.modules.staff.model.StaffEntity;
import com.anastasia.Anastasia_BackEnd.modules.staff.repository.StaffRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserStatus;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {

    private static final String TEMP_PASSWORD_SPECIALS = "!@#$%^&*";
    private static final String TEMP_PASSWORD_UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String TEMP_PASSWORD_LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String TEMP_PASSWORD_DIGITS = "23456789";
    private static final String TEMP_PASSWORD_ALL = TEMP_PASSWORD_UPPER + TEMP_PASSWORD_LOWER + TEMP_PASSWORD_DIGITS + TEMP_PASSWORD_SPECIALS;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;
    private final ChurchRepository churchRepository;
    private final SecurityUtils securityUtils;
    private final PasswordEncoder passwordEncoder;
    private final StaffRepository staffRepository;
    private final EmailNotificationService emailNotificationService;
    private final LocalizedMessageService messageService;

    @Transactional
    @Override
    public StaffResponse create(CreateStaffRequest request) {
        UUID tenantId = requireTenantId();
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get("tenant.notFound", "Tenant not found")));

        String normalizedEmail = normalizeEmail(request.email());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException(messageService.get("auth.email.exists", "Email is already in use"));
        }

        ChurchEntity church = resolveChurch(tenant, request.churchId());
        validateDates(request.hireDate(), request.endDate());
        StaffEntity manager = resolveManager(tenantId, request.reportsToStaffId(), null);

        String staffNumber = generateUniqueStaffNumber();
        String temporaryPassword = generateTemporaryPassword();
        Role staffRole = roleRepository.findByRoleName(RoleType.STAFF.name())
                .orElseThrow(() -> new EntityNotFoundException(messageService.get("role.staff.notFound", "Staff role not found")));

        UserEntity user = UserEntity.builder()
                .fullName(normalizeText(request.fullName()))
                .email(normalizedEmail)
                .phoneNumber(normalizeText(request.phoneNumber()))
                .password(passwordEncoder.encode(temporaryPassword))
                .roles(new HashSet<>(java.util.Set.of(staffRole)))
                .userType(UserType.STAFF)
                .status(UserStatus.ACTIVE)
                .verified(true)
                .tenant(tenant)
                .mustChangePassword(true)
                .temporaryPasswordIssuedAt(Instant.now())
                .lastPasswordChangedAt(null)
                .build();
        user = userRepository.save(user);

        StaffEntity staff = StaffEntity.builder()
                .staffNumber(staffNumber)
                .tenant(tenant)
                .church(church)
                .churchNumber(church.getChurchNumber())
                .user(user)
                .positionType(request.positionType())
                .employmentStatus(request.employmentStatus() == null ? StaffEmploymentStatus.ACTIVE : request.employmentStatus())
                .department(normalizeText(request.department()))
                .primaryPhone(normalizeText(request.primaryPhone()))
                .alternatePhone(normalizeText(request.alternatePhone()))
                .hireDate(request.hireDate())
                .endDate(request.endDate())
                .reportsTo(manager)
                .notes(normalizeText(request.notes()))
                .invitedAt(Instant.now())
                .lastCredentialResetAt(Instant.now())
                .build();
        staff = staffRepository.save(staff);

        sendTemporaryCredentialEmail(user, tenant, staff, temporaryPassword);
        return toResponse(staff);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<StaffResponse> list(String query, StaffEmploymentStatus status, Pageable pageable) {
        UUID tenantId = requireTenantId();
        return staffRepository.searchTenantStaff(tenantId, normalizeQuery(query), status, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    @Override
    public StaffResponse getById(Long staffId) {
        return toResponse(requireStaffInTenant(staffId));
    }

    @Transactional
    @Override
    public StaffResponse update(Long staffId, UpdateStaffRequest request) {
        StaffEntity staff = requireStaffInTenant(staffId);
        validateDates(request.hireDate() != null ? request.hireDate() : staff.getHireDate(),
                request.endDate() != null ? request.endDate() : staff.getEndDate());

        if (request.fullName() != null) {
            String fullName = normalizeText(request.fullName());
            if (fullName == null) {
                throw new IllegalArgumentException(messageService.get("validation.staff.fullName.required", "Full name cannot be blank"));
            }
            staff.getUser().setFullName(fullName);
        }

        if (request.email() != null) {
            String email = normalizeEmail(request.email());
            if (!email.equalsIgnoreCase(staff.getUser().getEmail()) && userRepository.existsByEmail(email)) {
                throw new IllegalArgumentException(messageService.get("auth.email.exists", "Email is already in use"));
            }
            staff.getUser().setEmail(email);
        }

        if (request.phoneNumber() != null) {
            staff.getUser().setPhoneNumber(normalizeText(request.phoneNumber()));
        }
        if (request.churchId() != null) {
            ChurchEntity church = resolveChurch(staff.getTenant(), request.churchId());
            staff.setChurch(church);
            staff.setChurchNumber(church.getChurchNumber());
        }
        if (request.positionType() != null) {
            staff.setPositionType(request.positionType());
        }
        if (request.employmentStatus() != null) {
            staff.setEmploymentStatus(request.employmentStatus());
            if (request.employmentStatus() == StaffEmploymentStatus.TERMINATED
                    || request.employmentStatus() == StaffEmploymentStatus.INACTIVE) {
                staff.setDeactivatedAt(Instant.now());
                staff.getUser().setStatus(UserStatus.DISABLED);
            } else {
                staff.setDeactivatedAt(null);
                if (staff.getUser().getStatus() == UserStatus.DISABLED) {
                    staff.getUser().setStatus(UserStatus.ACTIVE);
                }
            }
        }
        if (request.department() != null) {
            staff.setDepartment(normalizeText(request.department()));
        }
        if (request.primaryPhone() != null) {
            staff.setPrimaryPhone(normalizeText(request.primaryPhone()));
        }
        if (request.alternatePhone() != null) {
            staff.setAlternatePhone(normalizeText(request.alternatePhone()));
        }
        if (request.hireDate() != null) {
            staff.setHireDate(request.hireDate());
        }
        if (request.endDate() != null) {
            staff.setEndDate(request.endDate());
        }
        if (request.notes() != null) {
            staff.setNotes(normalizeText(request.notes()));
        }
        if (request.reportsToStaffId() != null) {
            staff.setReportsTo(resolveManager(staff.getTenant().getId(), request.reportsToStaffId(), staff.getId()));
        }

        userRepository.save(staff.getUser());
        staffRepository.save(staff);
        return toResponse(staff);
    }

    @Transactional
    @Override
    public StaffResponse deactivate(Long staffId) {
        StaffEntity staff = requireStaffInTenant(staffId);
        staff.setEmploymentStatus(StaffEmploymentStatus.TERMINATED);
        staff.setDeactivatedAt(Instant.now());
        staff.getUser().setStatus(UserStatus.DISABLED);
        userRepository.save(staff.getUser());
        staffRepository.save(staff);
        return toResponse(staff);
    }

    @Transactional
    @Override
    public void resetCredentials(Long staffId) {
        StaffEntity staff = requireStaffInTenant(staffId);
        String temporaryPassword = generateTemporaryPassword();
        UserEntity user = staff.getUser();
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);
        user.setTemporaryPasswordIssuedAt(Instant.now());
        user.setLastPasswordChangedAt(null);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        staff.setLastCredentialResetAt(Instant.now());
        staffRepository.save(staff);

        sendTemporaryCredentialEmail(user, staff.getTenant(), staff, temporaryPassword);
    }

    private StaffResponse toResponse(StaffEntity staff) {
        return new StaffResponse(
                staff.getId(),
                staff.getStaffNumber(),
                staff.getUser().getUuid(),
                staff.getUser().getFullName(),
                staff.getUser().getEmail(),
                staff.getUser().getPhoneNumber(),
                staff.getChurch() == null ? null : staff.getChurch().getChurchId(),
                staff.getChurchNumber(),
                staff.getChurch() == null ? null : staff.getChurch().getChurchName(),
                staff.getPositionType(),
                staff.getEmploymentStatus(),
                staff.getDepartment(),
                staff.getPrimaryPhone(),
                staff.getAlternatePhone(),
                staff.getHireDate(),
                staff.getEndDate(),
                staff.getReportsTo() == null ? null : staff.getReportsTo().getId(),
                staff.getReportsTo() == null ? null : staff.getReportsTo().getUser().getFullName(),
                staff.getNotes(),
                staff.getUser().isMustChangePassword(),
                toLocalDateTime(staff.getInvitedAt()),
                toLocalDateTime(staff.getInviteAcceptedAt()),
                toLocalDateTime(staff.getFirstLoginAt()),
                toLocalDateTime(staff.getLastCredentialResetAt()),
                toLocalDateTime(staff.getDeactivatedAt()),
                staff.getCreatedDate(),
                staff.getLastModifiedDate()
        );
    }

    private UUID requireTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException(messageService.get("tenant.context.missing", "Tenant ID is not set in the context"));
        }
        return tenantId;
    }

    private StaffEntity requireStaffInTenant(Long staffId) {
        UUID tenantId = requireTenantId();
        StaffEntity staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get("staff.notFound", "Staff not found")));
        if (!tenantId.equals(staff.getTenant().getId())) {
            throw new EntityNotFoundException(messageService.get("staff.notFound", "Staff not found"));
        }
        return staff;
    }

    private ChurchEntity resolveChurch(TenantEntity tenant, Long churchId) {
        Long effectiveChurchId = churchId;
        if (effectiveChurchId == null) {
            if (tenant.getChurch() == null) {
                throw new IllegalStateException(messageService.get("staff.church.required", "A church must be selected for staff"));
            }
            return tenant.getChurch();
        }

        ChurchEntity church = churchRepository.findById(effectiveChurchId)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get("church.notFound", "Church not found")));
        if (church.getTenant() == null || !tenant.getId().equals(church.getTenant().getId())) {
            throw new IllegalArgumentException(messageService.get("staff.church.tenantMismatch", "Staff church must belong to the current tenant"));
        }
        return church;
    }

    private StaffEntity resolveManager(UUID tenantId, Long managerStaffId, Long currentStaffId) {
        if (managerStaffId == null) {
            return null;
        }
        StaffEntity manager = staffRepository.findById(managerStaffId)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get("staff.manager.notFound", "Reporting manager not found")));
        if (!tenantId.equals(manager.getTenant().getId())) {
            throw new IllegalArgumentException(messageService.get("staff.manager.tenantMismatch", "Reporting manager must belong to the current tenant"));
        }
        if (currentStaffId != null && currentStaffId.equals(managerStaffId)) {
            throw new IllegalArgumentException(messageService.get("staff.manager.selfReference", "Staff cannot report to itself"));
        }
        return manager;
    }

    private void validateDates(java.time.LocalDate hireDate, java.time.LocalDate endDate) {
        if (hireDate != null && endDate != null && endDate.isBefore(hireDate)) {
            throw new IllegalArgumentException(messageService.get("staff.dates.invalid", "End date cannot be earlier than hire date"));
        }
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
    }

    private String generateUniqueStaffNumber() {
        String staffNumber;
        do {
            staffNumber = securityUtils.generateUniqueIDNumber(6, "S");
        } while (staffRepository.existsByStaffNumber(staffNumber));
        return staffNumber;
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(12);
        password.append(randomChar(TEMP_PASSWORD_UPPER));
        password.append(randomChar(TEMP_PASSWORD_LOWER));
        password.append(randomChar(TEMP_PASSWORD_DIGITS));
        password.append(randomChar(TEMP_PASSWORD_SPECIALS));
        while (password.length() < 12) {
            password.append(randomChar(TEMP_PASSWORD_ALL));
        }
        return shuffle(password.toString());
    }

    private char randomChar(String alphabet) {
        return alphabet.charAt(RANDOM.nextInt(alphabet.length()));
    }

    private String shuffle(String value) {
        char[] chars = value.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }

    private void sendTemporaryCredentialEmail(UserEntity user, TenantEntity tenant, StaffEntity staff, String temporaryPassword) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("username", user.getFullName());
        properties.put(
                "message_content",
                "Your staff account for " + tenant.getOwnerName()
                        + " has been created. Use email " + user.getEmail()
                        + " and temporary password " + temporaryPassword
                        + ". You will be required to change the password immediately after login. Staff number: "
                        + staff.getStaffNumber() + "."
        );

        emailNotificationService.sendEmail(
                user.getEmail(),
                "Anastasia staff account credentials",
                EmailTemplateName.NOTIFICATION,
                properties
        );
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(messageService.get("validation.email.required", "Email is required"));
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String normalizeQuery(String query) {
        String normalized = normalizeText(query);
        return normalized == null ? null : normalized;
    }
}
