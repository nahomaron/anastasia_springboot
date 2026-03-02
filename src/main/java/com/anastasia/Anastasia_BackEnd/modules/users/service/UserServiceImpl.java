package com.anastasia.Anastasia_BackEnd.modules.users.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.core.auth.service.AuthService;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.EmailNotificationService;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplateName;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.MemberStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.MemberTransferRequestEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.mappers.UsersMapper;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.MemberTransferService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.card.MembershipCardService;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.ChangePasswordRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarType;
import com.anastasia.Anastasia_BackEnd.core.auth.role.AssignRolesRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserDTO;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserResponseIDs;
import com.anastasia.Anastasia_BackEnd.modules.users.model.SimpleUserDTO;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserType;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.AvatarRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.BaseMember;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChildRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.MembershipSummary;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.MemberTransferResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.TenantInviteResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.TenantMembershipAction;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.TenantUserRowResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.TenantUsersMetricsResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.TenantUsersPageResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.TenantUserStatus;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.UserMembershipsResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UsersMapper usersMapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final AvatarRepository avatarRepository;
    private final ChildRepository childRepository;
    private final TenantRepository tenantRepository;
    private final MemberTransferService memberTransferService;
    private final MembershipCardService membershipCardService;
    private final AuthService authService;
    private final EmailNotificationService emailNotificationService;


    @Override
    public UserEntity convertToEntity(UserDTO userDTO) {
        return usersMapper.userDTOToUserEntity(userDTO);
    }

    @Override
    public UserDTO convertToDTO(UserEntity userEntity) {
//        System.out.println("Converting UserEntity to DTO: " + userEntity);
        return usersMapper.userEntityToUserDTO(userEntity);
    }


    @Cacheable(value = "users_all", keyGenerator = "tenantAwareKeyGenerator")
    @Override
    public Page<UserResponseIDs> findAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toIdResponse);
    }

    @Cacheable(value = "users", key = "#userId")
    @Override
    public Optional<SimpleUserDTO> findOne(UUID userId) {
        return userRepository.findById(userId).map(this::toSimpleUserDTO);
    }

    @Override
    public Optional<UserEntity> findEntity(UUID userId) {
        return userRepository.findById(userId);
    }

    @Caching(evict = {
            @CacheEvict(value = "users_all", keyGenerator = "tenantAwareKeyGenerator", allEntries = true),
            @CacheEvict(value = "users_all_list", keyGenerator = "tenantAwareKeyGenerator", allEntries = true)
    })
    @Override
    public SimpleUserDTO updateUserDetails(UserEntity userEntity, Principal connectedUser) {
//        var currentUser = (UserEntity) ((UsernamePasswordAuthenticationToken) connectedUser).getPrincipal();

        if (!(connectedUser instanceof Authentication)){
            throw new IllegalStateException("Invalid user authorization");
        }

        Authentication authentication = (Authentication) connectedUser;
        Object principal = authentication.getPrincipal();

        if(!(principal instanceof UserPrincipal)){
            throw new IllegalStateException("Invalid user principal");
        }

        UserPrincipal userPrincipal = (UserPrincipal) principal;

        return userRepository.findById(userPrincipal.getUserUuid()).map(existingUser -> {
            Optional.ofNullable(userEntity.getFullName()).ifPresent(existingUser::setFullName);
            Optional.ofNullable(userEntity.getEmail()).ifPresent(existingUser::setEmail);
            return toSimpleUserDTO(userRepository.save(existingUser));
        }).orElseThrow(() -> new RuntimeException("User doesn't exist"));
    }


    @Caching(
            evict = {
                    // Assuming the UserPrincipal object has a consistent key with the cache, or fetch the user first:
                    @CacheEvict(value = "users",
                            key = "#user.uuid" // Needs SpEL access to the 'user' variable.
//                            keyGenerator = "tenantAwareKeyGenerator"
                    )
            }
    )
    @Override
    public void changePassword(ChangePasswordRequest request, Principal connectedUser) {
        if (!(connectedUser instanceof Authentication)) {
            throw new IllegalStateException("Invalid user authentication");
        }

        Authentication authentication = (Authentication) connectedUser;
        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UserPrincipal)) {
            throw new IllegalStateException("Invalid user principal");
        }

        UserPrincipal userPrincipal = (UserPrincipal) principal;

        // Fetch the UserEntity from the database using the UserPrincipal's ID or email
        UserEntity user = userRepository.findByEmail(userPrincipal.getUsername()) // or findById(userPrincipal.getId())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        // Validate current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Incorrect password provided");
        }

        // Check if the new password matches confirmation
        if (!request.isPasswordMatch()) {
            throw new BadCredentialsException("Passwords do not match");
        }

        // Update the password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Caching(
            evict = {
                    @CacheEvict(value = "users", key = "#userId"
//                            keyGenerator = "tenantAwareKeyGenerator"
                    ),
                    @CacheEvict(value = "users_all",
                            keyGenerator = "tenantAwareKeyGenerator", allEntries = true)
            }
    )
    @Override
    public void assignRolesToUser(UUID userId, AssignRolesRequest request) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UUID tenantId = TenantContext.getTenantId();

        if (tenantId == null) {
            throw new IllegalStateException("Tenant ID is not set in the context");
        }


        Set<Role> roles = roleRepository.findAll()
                .stream()
                .filter(role -> request.roleIds().contains(role.getId()))
                .collect(Collectors.toSet());

        System.out.println("Roles "+ roles);

        user.setRoles(roles);

        userRepository.save(user);
    }

    @Cacheable(value = "users_all_list", keyGenerator = "tenantAwareKeyGenerator")
    @Override
    public List<UserResponseIDs> findAll() {
        return userRepository.findAll().stream().map(this::toIdResponse).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<SimpleUserDTO> searchUsers(String query, Set<String> roles) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant ID is not set in the context");
        }
        String q = query == null ? "" : query.trim();
        if (q.isBlank()) {
            return List.of();
        }
        if (roles == null || roles.isEmpty()) {
            return userRepository.searchByTenantId(tenantId, q);
        }
        return userRepository.searchByTenantIdAndRoles(tenantId, q, roles);
    }

    @Transactional(readOnly = true)
    @Override
    public TenantUsersPageResponse listTenantUsers(String query, String status, String role, int page, int size) {
        UUID tenantId = requireTenantId();
        int safeSize = Math.min(Math.max(size, 1), 200);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "fullName"));

        Specification<UserEntity> spec = byTenant(tenantId)
                .and(searchByQuery(query))
                .and(filterByStatus(status))
                .and(filterByRole(role));

        Page<UserEntity> userPage = userRepository.findAll(spec, pageable);
        List<TenantUserRowResponse> items = userPage.getContent().stream()
                .map(this::toTenantUserRow)
                .toList();

        TenantUsersMetricsResponse metrics = computeTenantMetrics(tenantId);
        List<String> roles = userRepository.findByTenantId(tenantId).stream()
                .flatMap(user -> user.getRoles().stream())
                .map(Role::getRoleName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .sorted()
                .toList();

        return TenantUsersPageResponse.builder()
                .items(items)
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalPages(userPage.getTotalPages())
                .totalElements(userPage.getTotalElements())
                .sizeOptions(List.of(20, 50, 100, 200))
                .roles(roles)
                .metrics(metrics)
                .build();
    }

    @Transactional
    @Override
    public TenantInviteResponse inviteUserToTenant(String email) {
        UUID tenantId = requireTenantId();
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        Optional<UserEntity> existingInTenant = userRepository.findByTenantIdAndEmailIgnoreCase(tenantId, normalizedEmail);
        if (existingInTenant.isPresent()) {
            UserEntity user = existingInTenant.get();
            if (!user.isVerified()) {
                try {
                    authService.resendActivationEmail(user.getEmail());
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to resend activation email");
                }
            } else {
                sendTenantInviteEmail(normalizedEmail);
            }
            return TenantInviteResponse.builder()
                    .email(normalizedEmail)
                    .existingUser(true)
                    .message("Invitation email sent.")
                    .build();
        }

        // Prevent leaking or hijacking users from other tenants by email.
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("Email is already associated with another tenant.");
        }

        sendTenantInviteEmail(normalizedEmail);
        return TenantInviteResponse.builder()
                .email(normalizedEmail)
                .existingUser(false)
                .message("Invitation email sent.")
                .build();
    }

    @Transactional
    @Override
    public TenantUserRowResponse applyMembershipAction(UUID userId, TenantMembershipAction action) {
        UUID tenantId = requireTenantId();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!tenantId.equals(user.getTenantId())) {
            throw new EntityNotFoundException("User not found in current tenant");
        }

        if (isProtectedTenantAccount(user)) {
            throw new IllegalArgumentException("Protected tenant account cannot be modified by membership actions.");
        }

        switch (action) {
            case APPROVE, RESTORE -> {
                if (user.getMembership() != null) {
                    user.getMembership().setStatus(MemberStatus.ACTIVE.name());
                }
            }
            case DENY, SUSPEND -> {
                if (user.getMembership() != null) {
                    user.getMembership().setStatus(MemberStatus.NON_ACTIVE.name());
                    membershipCardService.revokeCardByMembershipNumber(
                            tenantId,
                            user.getMembership().getMembershipNumber(),
                            "Membership status changed to " + action.name());
                }
            }
        }

        UserEntity saved = userRepository.save(user);
        return toTenantUserRow(saved);
    }

    @Transactional
    @Override
    public MemberTransferResponse createMemberTransferRequest(UUID userId, UUID targetTenantId, String reason) {
        UUID actorTenantId = requireTenantId();
        UUID actorUserId = getCurrentUserId();
        MemberTransferRequestEntity request = memberTransferService.createTransferRequest(
                actorTenantId,
                userId,
                targetTenantId,
                actorUserId,
                reason
        );
        return toMemberTransferResponse(request);
    }

    @Transactional
    @Override
    public MemberTransferResponse approveMemberTransferRequest(UUID transferRequestId, String decisionNote) {
        UUID actorTenantId = requireTenantId();
        UUID actorUserId = getCurrentUserId();
        MemberTransferRequestEntity request = memberTransferService.approveTransferRequest(
                actorTenantId,
                transferRequestId,
                actorUserId,
                decisionNote
        );
        return toMemberTransferResponse(request);
    }

    @Transactional
    @Override
    public MemberTransferResponse rejectMemberTransferRequest(UUID transferRequestId, String decisionNote) {
        UUID actorTenantId = requireTenantId();
        UUID actorUserId = getCurrentUserId();
        MemberTransferRequestEntity request = memberTransferService.rejectTransferRequest(
                actorTenantId,
                transferRequestId,
                actorUserId,
                decisionNote
        );
        return toMemberTransferResponse(request);
    }

    @Caching(
            evict = {
                    @CacheEvict(value = "users", key = "#userId"
//                            keyGenerator = "tenantAwareKeyGenerator"
                    ),
                    @CacheEvict(value = "users_all", keyGenerator = "tenantAwareKeyGenerator", allEntries = true),
                    @CacheEvict(value = "users_all_list", keyGenerator = "tenantAwareKeyGenerator", allEntries = true)
            }
    )
    @Override
    public void deleteUser(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        userRepository.delete(user);
    }

    public UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getUserUuid(); // or userPrincipal.getId();
        }
        throw new RuntimeException("No authenticated user found.");
    }

    @CachePut(value = "users",
            key = "#result.uuid" // Use the ID of the returned (saved) entity
//            keyGenerator = "tenantAwareKeyGenerator"
    )
    @Override
    public void updateProfileAvatar(AvatarDTO avatarDTO) {
        UUID userId = getCurrentUserId();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        AvatarEntity avatar = AvatarEntity.builder()
                .imageUrl(avatarDTO.getImageUrl())
                .imageSize(avatarDTO.getImageSize())
                .avatarType(AvatarType.USER)
                .ownerId(user.getUuid())
                .build();

        avatar = avatarRepository.save(avatar);
        user.setProfileAvatar(avatar);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    @Override
    public UserMembershipsResponse getCurrentUserMemberships() {
        UUID userId = getCurrentUserId();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Adult_MemberEntity adultMembership = user.getMembership();
        MembershipSummary selfMembership = null;
        List<MembershipSummary> managedMemberships = new ArrayList<>();

        if (adultMembership != null) {
            selfMembership = toSummary(adultMembership, "SELF", true);

            Long parentId = adultMembership.getId();
            List<Child_MemberEntity> children = childRepository.findByFatherIdOrMotherId(parentId, parentId);
            for (Child_MemberEntity child : children) {
                boolean primaryGuardian = isPrimaryGuardian(child, parentId);
                managedMemberships.add(toSummary(child, "CHILD", primaryGuardian));
            }
        }

        return UserMembershipsResponse.builder()
                .selfMembership(selfMembership)
                .managedMemberships(managedMemberships)
                .build();
    }

    private boolean isPrimaryGuardian(Child_MemberEntity child, Long parentId) {
        if (child == null || parentId == null) {
            return false;
        }
        if (child.getFather() != null && parentId.equals(child.getFather().getId())) {
            return true;
        }
        return child.getMother() != null && parentId.equals(child.getMother().getId());
    }

    private MembershipSummary toSummary(BaseMember member, String relationshipToUser, boolean isPrimaryGuardian) {
        if (member == null) {
            return null;
        }

        String fullName = String.join(" ",
                nullToEmpty(member.getFirstName()),
                nullToEmpty(member.getFatherName()),
                nullToEmpty(member.getGrandFatherName())
        ).trim();

        String churchName = member.getChurch() != null ? member.getChurch().getChurchName() : null;

        return MembershipSummary.builder()
                .memberId(member.getMembershipNumber())
                .fullName(fullName.isBlank() ? null : fullName)
                .relationshipToUser(relationshipToUser)
                .status(mapMembershipStatus(member.getStatus()))
                .churchName(churchName)
                .isPrimaryGuardian(isPrimaryGuardian)
                .build();
    }

    private String mapMembershipStatus(String status) {
        if (status == null) {
            return null;
        }
        return switch (status.toUpperCase()) {
            case "PENDING" -> "PENDING";
            case "APPROVED", "ACTIVE" -> "ACTIVE";
            case "NON_ACTIVE", "DECEASED" -> "TERMINATED";
            default -> status.toUpperCase();
        };
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private UserResponseIDs toIdResponse(UserEntity user) {
        return UserResponseIDs.builder()
                .uuid(user.getUuid())
                .build();
    }

    private UUID requireTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant ID is not set in the context");
        }
        return tenantId;
    }

    private Specification<UserEntity> byTenant(UUID tenantId) {
        return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
    }

    private Specification<UserEntity> searchByQuery(String queryText) {
        return (root, query, cb) -> {
            String q = queryText == null ? "" : queryText.trim().toLowerCase(Locale.ROOT);
            if (q.isBlank()) {
                return cb.conjunction();
            }

            var membershipJoin = root.join("membership", jakarta.persistence.criteria.JoinType.LEFT);
            String pattern = "%" + q + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(cb.coalesce(membershipJoin.get("membershipNumber"), "")), pattern)
            );
        };
    }

    private Specification<UserEntity> filterByStatus(String status) {
        return (root, query, cb) -> {
            if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
                return cb.conjunction();
            }
            String normalized = status.trim().toUpperCase(Locale.ROOT);
            var membershipJoin = root.join("membership", jakarta.persistence.criteria.JoinType.LEFT);
            return switch (normalized) {
                case "ACTIVE" -> cb.and(
                        cb.isFalse(root.get("accountLocked")),
                        membershipJoin.get("status").in(MemberStatus.ACTIVE.name(), MemberStatus.APPROVED.name())
                );
                case "INVITED" -> cb.or(
                        cb.isNull(root.get("membershipId")),
                        cb.equal(membershipJoin.get("status"), MemberStatus.PENDING.name())
                );
                case "DISABLED" -> membershipJoin.get("status").in(MemberStatus.NON_ACTIVE.name(), MemberStatus.DECEASED.name());
                case "LOCKED" -> cb.isTrue(root.get("accountLocked"));
                default -> cb.conjunction();
            };
        };
    }

    private Specification<UserEntity> filterByRole(String role) {
        return (root, query, cb) -> {
            if (role == null || role.isBlank() || "ALL".equalsIgnoreCase(role)) {
                return cb.conjunction();
            }
            query.distinct(true);
            var rolesJoin = root.join("roles", jakarta.persistence.criteria.JoinType.LEFT);
            return cb.equal(rolesJoin.get("roleName"), role);
        };
    }

    private TenantUsersMetricsResponse computeTenantMetrics(UUID tenantId) {
        List<UserEntity> users = userRepository.findByTenantId(tenantId);
        Map<TenantUserStatus, Long> counters = new HashMap<>();
        counters.put(TenantUserStatus.ACTIVE, 0L);
        counters.put(TenantUserStatus.INVITED, 0L);
        counters.put(TenantUserStatus.DISABLED, 0L);
        counters.put(TenantUserStatus.LOCKED, 0L);

        for (UserEntity user : users) {
            TenantUserStatus status = resolveTenantUserStatus(user);
            counters.put(status, counters.get(status) + 1L);
        }

        return TenantUsersMetricsResponse.builder()
                .total(users.size())
                .active(counters.get(TenantUserStatus.ACTIVE))
                .invited(counters.get(TenantUserStatus.INVITED))
                .disabled(counters.get(TenantUserStatus.DISABLED))
                .locked(counters.get(TenantUserStatus.LOCKED))
                .build();
    }

    private TenantUserRowResponse toTenantUserRow(UserEntity user) {
        List<String> roles = user.getRoles().stream()
                .map(Role::getRoleName)
                .filter(name -> name != null && !name.isBlank())
                .sorted()
                .toList();

        List<String> groups = user.getGroups().stream()
                .map(group -> group.getGroupName())
                .filter(name -> name != null && !name.isBlank())
                .sorted()
                .toList();

        String membershipId = user.getMembership() != null ? user.getMembership().getMembershipNumber() : null;

        return TenantUserRowResponse.builder()
                .id(user.getUuid())
                .tenantId(user.getTenantId())
                .username(user.getFullName())
                .email(user.getEmail())
                .roles(roles)
                .groups(groups)
                .membershipId(membershipId)
                .status(resolveTenantUserStatus(user))
                .createdAt(user.getCreatedDate())
                .protectedAccount(isProtectedTenantAccount(user))
                .protectedReason(protectedAccountReason(user))
                .build();
    }

    private TenantUserStatus resolveTenantUserStatus(UserEntity user) {
        if (user.isAccountLocked()) {
            return TenantUserStatus.LOCKED;
        }

        if (user.getMembership() == null || user.getMembership().getStatus() == null) {
            return TenantUserStatus.INVITED;
        }

        String memberStatus = user.getMembership().getStatus().toUpperCase(Locale.ROOT);
        return switch (memberStatus) {
            case "PENDING" -> TenantUserStatus.INVITED;
            case "NON_ACTIVE", "DECEASED" -> TenantUserStatus.DISABLED;
            case "APPROVED", "ACTIVE" -> TenantUserStatus.ACTIVE;
            default -> TenantUserStatus.ACTIVE;
        };
    }

    private void sendTenantInviteEmail(String email) {
        UUID tenantId = requireTenantId();
        String ownerName = tenantRepository.findById(tenantId)
                .map(t -> t.getOwnerName())
                .orElse("your church");

        Map<String, Object> properties = new HashMap<>();
        properties.put("username", "Member");
        properties.put("message_content",
                "You are invited to join " + ownerName + " on Anastasia. Complete your account registration at /auth/register.");

        emailNotificationService.sendEmail(
                email,
                "Anastasia membership invitation",
                EmailTemplateName.NOTIFICATION,
                properties
        );
    }

    private boolean isProtectedTenantAccount(UserEntity user) {
        if (user == null) {
            return false;
        }

        if (UserType.TENANT.equals(user.getUserType())) {
            return true;
        }

        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getRoleName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toSet());

        return roleNames.contains("OWNER") || roleNames.contains("ADMIN");
    }

    private String protectedAccountReason(UserEntity user) {
        if (!isProtectedTenantAccount(user)) {
            return null;
        }

        if (UserType.TENANT.equals(user.getUserType())) {
            return "Tenant governance account";
        }

        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getRoleName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toSet());

        if (roleNames.contains("OWNER") && roleNames.contains("ADMIN")) {
            return "Owner/Admin governance role";
        }
        if (roleNames.contains("OWNER")) {
            return "Owner governance role";
        }
        return "Admin governance role";
    }

    private SimpleUserDTO toSimpleUserDTO(UserEntity user) {
        return SimpleUserDTO.builder()
                .uuid(user.getUuid())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .build();
    }

    private MemberTransferResponse toMemberTransferResponse(MemberTransferRequestEntity request) {
        return MemberTransferResponse.builder()
                .id(request.getId())
                .userId(request.getUserId())
                .fromTenantId(request.getFromTenant().getId())
                .toTenantId(request.getToTenant().getId())
                .status(request.getStatus())
                .reason(request.getReason())
                .decisionNote(request.getDecisionNote())
                .requestedByUserId(request.getRequestedByUserId())
                .decidedByUserId(request.getDecidedByUserId())
                .requestedAt(request.getRequestedAt())
                .decidedAt(request.getDecidedAt())
                .executedAt(request.getExecutedAt())
                .build();
    }

}
