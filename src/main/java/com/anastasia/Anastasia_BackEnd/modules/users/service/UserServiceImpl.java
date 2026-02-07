package com.anastasia.Anastasia_BackEnd.modules.users.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.modules.registration.mappers.UsersMapper;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.ChangePasswordRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarType;
import com.anastasia.Anastasia_BackEnd.core.auth.role.AssignRolesRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserDTO;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.AvatarRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.BaseMember;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChildRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.MembershipSummary;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.UserMembershipsResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
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
    public Page<UserEntity> findAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Cacheable(value = "users", key = "#userId")
    @Override
    public Optional<UserEntity> findOne(UUID userId) {
        return userRepository.findById(userId);
    }


    @Caching(
            put = {
                    @CachePut(value = "users",
                            key = "#result.uuid" // Use the ID of the returned (saved) entity
//                            keyGenerator = "tenantAwareKeyGenerator"
                            )
            },
            evict = {
                    @CacheEvict(value = "users_all", keyGenerator = "tenantAwareKeyGenerator", allEntries = true),
                    @CacheEvict(value = "users_all_list", keyGenerator = "tenantAwareKeyGenerator", allEntries = true)
            }
    )
    @Override
    public UserEntity updateUserDetails(UserEntity userEntity, Principal connectedUser) {
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
            return userRepository.save(existingUser);
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
    public List<UserEntity> findAll() {
        return userRepository.findAll();
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

}
