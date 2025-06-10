package com.anastasia.Anastasia_BackEnd.service.registration;

import com.anastasia.Anastasia_BackEnd.mappers.PriestMapper;
import com.anastasia.Anastasia_BackEnd.model.priest.PriestDTO;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.priest.PriestEntity;
import com.anastasia.Anastasia_BackEnd.model.priest.PriestStatus;
import com.anastasia.Anastasia_BackEnd.model.role.Role;
import com.anastasia.Anastasia_BackEnd.model.role.RoleType;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.model.user.UserType;
import com.anastasia.Anastasia_BackEnd.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.repository.PriestRepository;
import com.anastasia.Anastasia_BackEnd.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.RoleRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.UserRepository;
import com.anastasia.Anastasia_BackEnd.service.auth.AuthServiceImpl;
import com.anastasia.Anastasia_BackEnd.util.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PriestServiceImpl implements PriestService{

    private final PriestMapper priestMapper;
    private final PriestRepository priestRepository;
    private final ChurchRepository churchRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final AuthServiceImpl authService;
    private final RoleRepository roleRepository;
    private final SecurityUtils securityUtils;

    @Override
    public PriestEntity convertToEntity(PriestDTO priestDTO) {
        return priestMapper.priestDTOToEntity(priestDTO);
    }

    @Override
    public PriestDTO convertToDTO(PriestEntity priestEntity) {
        return priestMapper.priestEntityToDTO(priestEntity);
    }



    @Override
    public void registerPriest(PriestDTO priestDTO) {

        // Try to find an existing user by email
        UserEntity priestUser = userRepository.findByEmail(priestDTO.getPersonalEmail()).orElse(null);

        Role priestRole = roleRepository.findByRoleName(RoleType.PRIEST.name())
                .orElseThrow(() -> new RuntimeException("Priest role not found"));

        if (priestUser == null) {
            // If user does not exist, create a new one
            priestUser = UserEntity.builder()
                    .fullName(priestDTO.getFirstName() + " " + priestDTO.getFatherName() + " " + priestDTO.getGrandFatherName())
                    .email(priestDTO.getPersonalEmail())
                    .password(passwordEncoder.encode(priestDTO.getPassword()))
                    .roles(Set.of(priestRole))
                    .userType(UserType.PRIEST)
                    .build();

            // Save the newly created priest user
            try {
                var savedPriest = userRepository.save(priestUser);
                authService.sendValidationEmail(savedPriest);
            } catch (Exception e) {
                throw new RuntimeException("User creation failed: " + e.getMessage());
            }
        }


        if(priestDTO.getChurchNumber() == null && priestDTO.getTenantId() == null){
            throw new IllegalStateException("A priest should provide church number or be a tenant");
        }
        // Start building the PriestEntity
        PriestEntity.PriestEntityBuilder priestBuilder = PriestEntity.builder()
                .user(priestUser)
                .priestNumber(generateUniquePriestNumber(6))
                .churchNumber(priestDTO.getChurchNumber())
                .profilePicture(priestDTO.getProfilePicture())
                .prefixes(priestDTO.getPrefixes())
                .firstName(priestDTO.getFirstName())
                .fatherName(priestDTO.getFatherName())
                .grandFatherName(priestDTO.getGrandFatherName())
                .phoneNumber(priestDTO.getPhoneNumber())
                .churchEmail(priestDTO.getChurchEmail())
                .priesthoodCardId(priestDTO.getPriesthoodCardId())
                .priesthoodCardScan(priestDTO.getPriesthoodCardScan())
                .birthdate(priestDTO.getBirthdate())
                .languages(priestDTO.getLanguages())
                .levelOfEducation(priestDTO.getLevelOfEducation())
                .address(priestDTO.getAddress())
                .status(PriestStatus.PENDING)
                .isActive(false);

        boolean priestIsTenant = priestDTO.getTenantId() != null;
        boolean priestIsUnderChurch = priestDTO.getChurchNumber() != null;

        // Validation: A priest cannot be both a tenant and belong to a church
        if (priestIsTenant && priestIsUnderChurch) {
            throw new IllegalStateException("A priest cannot be both a tenant and belong to a church. Choose one.");
        }

        // If the priest is a tenant, associate with tenant
        if (priestIsTenant) {
            TenantEntity tenantFound = tenantRepository.findById(priestDTO.getTenantId())
                    .orElseThrow(() -> new EntityNotFoundException("Tenant with ID " + priestDTO.getTenantId() + " does not exist"));
            priestBuilder.tenant(tenantFound);
        }
        // If the priest is under a church, associate with church
        else if (priestIsUnderChurch) {
            ChurchEntity churchFound = churchRepository.findByChurchNumber(priestDTO.getChurchNumber())
                    .orElseThrow(() -> new EntityNotFoundException("Church with number " + priestDTO.getChurchNumber() + " not found"));
            priestBuilder.church(churchFound);
        }

        // Save the priest entity
        priestRepository.save(priestBuilder.build());
    }

    @Override
    public Page<PriestEntity> findAllPriests(Pageable pageable) {
        return priestRepository.findAll(pageable);
    }

    @Override
    public Optional<PriestEntity> findPriestById(Long priestId) {
        return priestRepository.findById(priestId);
    }

    @Override
    public PriestEntity updatePriestDetails(Long priestId, PriestEntity priestEntity) {

        return priestRepository.findById(priestId).map(foundPriest -> {
            Optional.ofNullable(priestEntity.getChurch()).ifPresent(foundPriest::setChurch);
            Optional.ofNullable(priestEntity.getProfilePicture()).ifPresent(foundPriest::setProfilePicture);

            Optional.ofNullable(priestEntity.getPrefixes()).ifPresent(foundPriest::setPrefixes);
            Optional.ofNullable(priestEntity.getFirstName()).ifPresent(foundPriest::setFirstName);
            Optional.ofNullable(priestEntity.getFatherName()).ifPresent(foundPriest::setFatherName);
            Optional.ofNullable(priestEntity.getGrandFatherName()).ifPresent(foundPriest::setGrandFatherName);

            Optional.ofNullable(priestEntity.getChurchEmail()).ifPresent(foundPriest::setChurchEmail);

            Optional.ofNullable(priestEntity.getBirthdate()).ifPresent(foundPriest::setBirthdate);

            Optional.ofNullable(priestEntity.getAddress()).ifPresent(foundPriest::setAddress);
            Optional.ofNullable(priestEntity.getLanguages()).ifPresent(foundPriest::setLanguages);
            Optional.ofNullable(priestEntity.getLevelOfEducation()).ifPresent(foundPriest::setLevelOfEducation);
            Optional.ofNullable(priestEntity.getPriesthoodCardId()).ifPresent(foundPriest::setPriesthoodCardId);
            Optional.ofNullable(priestEntity.getPriesthoodCardScan()).ifPresent(foundPriest::setPriesthoodCardScan);

            return priestRepository.save(foundPriest);
        }).orElseThrow(() -> new UsernameNotFoundException("Priest not found"));
    }

    @Override
    public void deletePriest(Long priestId) {
        priestRepository.deleteById(priestId);
    }

    private String generateUniquePriestNumber(int length) {
        String baseLetter = "K";

        String priestNumber;
        do {
            priestNumber = securityUtils.generateUniqueIDNumber(length, baseLetter);
        } while (priestRepository.existsByPriestNumber(priestNumber)); // Keep generating if it already exists
        return priestNumber;
    }

}
