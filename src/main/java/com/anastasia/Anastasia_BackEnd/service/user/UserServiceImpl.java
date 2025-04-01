package com.anastasia.Anastasia_BackEnd.service.user;

import com.anastasia.Anastasia_BackEnd.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.mappers.UsersMapper;
import com.anastasia.Anastasia_BackEnd.model.DTO.auth.ChangePasswordRequest;
import com.anastasia.Anastasia_BackEnd.model.DTO.auth.UserDTO;
import com.anastasia.Anastasia_BackEnd.model.entity.auth.Role;
import com.anastasia.Anastasia_BackEnd.model.entity.auth.UserEntity;
import com.anastasia.Anastasia_BackEnd.model.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.repository.auth.RoleRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.Principal;
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


    @Override
    public UserEntity convertToEntity(UserDTO userDTO) {
        return usersMapper.userDTOToUserEntity(userDTO);
    }

    @Override
    public UserDTO convertToDTO(UserEntity userEntity) {
//        System.out.println("Converting UserEntity to DTO: " + userEntity);
        return usersMapper.userEntityToUserDTO(userEntity);
    }


    @Override
    public Page<UserEntity> findAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    public Optional<UserEntity> findOne(UUID userId) {
        return userRepository.findById(userId);
    }


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

    @Override
    public void assignRolesToUser(UUID userId, Set<String> roleNames) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getTenant().getTenantId().equals(TenantContext.getTenantId())) {
            throw new IllegalStateException("No authorized tenant provided");
        }

        Set<Role> roles = roleRepository.findByTenantId(TenantContext.getTenantId())
                .stream()
                .filter(role -> roleNames.contains(role.getRoleName()))
                .collect(Collectors.toSet());

        user.setRoles(roles);

        userRepository.save(user);
    }

//    @Override
//    public void changePassword(ChangePasswordRequest request, Principal connectedUser) {
//        // Ensure that the Principal is of the expected type
//        if (!(connectedUser instanceof Authentication)) {
//            throw new IllegalStateException("Invalid user authentication");
//        }
//
//        var user = (UserEntity) ((UsernamePasswordAuthenticationToken) connectedUser).getPrincipal();
//
//
//        if(!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())){
//            throw new BadCredentialsException("Incorrect password provided");
//        }
//
//        if (!request.isPasswordMatch()){
//            throw new BadCredentialsException("Password are not matching");
//        }
//
//        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
//
//        userRepository.save(user);
//
//    }
}
