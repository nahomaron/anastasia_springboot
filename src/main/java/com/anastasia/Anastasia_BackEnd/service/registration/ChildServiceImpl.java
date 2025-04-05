package com.anastasia.Anastasia_BackEnd.service.registration;

import com.anastasia.Anastasia_BackEnd.mappers.ChildMapper;
import com.anastasia.Anastasia_BackEnd.model.child.ChildDTO;
import com.anastasia.Anastasia_BackEnd.model.child.ChildEntity;
import com.anastasia.Anastasia_BackEnd.model.child.ChildResponse;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.UserRepository;
import com.anastasia.Anastasia_BackEnd.repository.registration.ChildRepository;
import com.anastasia.Anastasia_BackEnd.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChildServiceImpl implements ChildService{

    private final ChildRepository childRepository;
    private final ChurchRepository churchRepository;
    private final UserRepository userRepository;
    private final ChildMapper childMapper;
    private final SecurityUtils securityUtils;

    @Override
    public ChildEntity convertToEntity(ChildDTO childDTO) {
        return childMapper.childDTOToEntity(childDTO);
    }

    @Override
    public ChildDTO convertToDTO(ChildEntity childEntity) {
        return childMapper.childEntityToDTO(childEntity);
    }

    @Override
    public ChildResponse registerChild(ChildEntity childEntity) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)){
            throw new IllegalStateException("User not authenticated");
        }
        UserEntity user = userRepository.findById(userPrincipal.getUserUuid())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));


        ChurchEntity church = churchRepository.findByChurchNumber(childEntity.getChurchNumber())
                .orElseThrow(() -> new IllegalStateException("No valid church number provided"));


        childEntity.setMembershipNumber(generateUniqueChildMembershipNumber(6, childEntity.isDeacon()));
        childEntity.setUser(user);
        childEntity.setChurch(church);
        ChildEntity membership = childRepository.save(childEntity);

        return ChildResponse.builder()
                .name(membership.getFirstName() + " " + membership.getFatherName() + " " + membership.getGrandFatherName())
                .membershipNumber(membership.getMembershipNumber())
                .fatherOfConfession(membership.getFatherOfConfession())
                .build();
    }

    @Override
    public Page<ChildEntity> findAll(Pageable pageable) {
        return childRepository.findAll(pageable);
    }

    @Override
    public Optional<ChildEntity> findChildById(Long memberId) {
        return childRepository.findById(memberId);
    }

    @Override
    public void updateChildDetails(Long memberId, ChildDTO request) {
        childRepository.findById(memberId).ifPresent(memberEntity -> {

            Optional.ofNullable(request.getChurchNumber()).ifPresent(memberEntity::setChurchNumber);
            Optional.ofNullable(request.getTitle()).ifPresent(memberEntity::setTitle);
            Optional.ofNullable(request.getFirstName()).ifPresent(memberEntity::setFirstName);
            Optional.ofNullable(request.getFatherName()).ifPresent(memberEntity::setFatherName);
            Optional.ofNullable(request.getGrandFatherName()).ifPresent(memberEntity::setGrandFatherName);
            Optional.ofNullable(request.getMotherName()).ifPresent(memberEntity::setMotherName);
            Optional.ofNullable(request.getMothersFather()).ifPresent(memberEntity::setMothersFather);
            Optional.ofNullable(request.getFirstNameT()).ifPresent(memberEntity::setFirstNameT);
            Optional.ofNullable(request.getFatherNameT()).ifPresent(memberEntity::setFatherNameT);
            Optional.ofNullable(request.getGrandFatherNameT()).ifPresent(memberEntity::setGrandFatherNameT);
            Optional.ofNullable(request.getMotherFullNameT()).ifPresent(memberEntity::setMotherFullNameT);
            Optional.ofNullable(request.getGender()).ifPresent(memberEntity::setGender);
            Optional.ofNullable(request.getBirthday()).ifPresent(memberEntity::setBirthday);
            Optional.ofNullable(request.getNationality()).ifPresent(memberEntity::setNationality);
            Optional.ofNullable(request.getPlaceOfBirth()).ifPresent(memberEntity::setPlaceOfBirth);
            Optional.ofNullable(request.getEmail()).ifPresent(memberEntity::setEmail);
            Optional.ofNullable(request.getPhone()).ifPresent(memberEntity::setPhone);
            Optional.ofNullable(request.getWhatsApp()).ifPresent(memberEntity::setWhatsApp);
            Optional.ofNullable(request.getEmergencyContactNumber()).ifPresent(memberEntity::setEmergencyContactNumber);
            Optional.ofNullable(request.getContactRelation()).ifPresent(memberEntity::setContactRelation);

            Optional.ofNullable(request.getFirstLanguage()).ifPresent(memberEntity::setFirstLanguage);
            Optional.ofNullable(request.getSecondLanguage()).ifPresent(memberEntity::setSecondLanguage);
            Optional.ofNullable(request.getLevelOfEducation()).ifPresent(memberEntity::setLevelOfEducation);
            Optional.ofNullable(request.getFatherOfConfession()).ifPresent(memberEntity::setFatherOfConfession);

            Optional.ofNullable(request.getAddress()).ifPresent(memberEntity::setAddress);

            childRepository.save(memberEntity);
        });
    }

    @Override
    public void deleteChildMembership(Long memberId) {
        childRepository.deleteById(memberId);

    }

    @Override
    public Page<ChildEntity> findAllBySpecification(Specification<ChildEntity> spec, Pageable pageable) {
        return childRepository.findAll(spec, pageable);
    }

    private String generateUniqueChildMembershipNumber(int length, boolean isDeacon) {

        String baseLetter = "C";

        if(isDeacon){
            baseLetter = "D";
        }

        String membershipNumber;
        do {
            membershipNumber = securityUtils.generateUniqueIDNumber(length, baseLetter);
        } while (childRepository.existsByMembershipNumber(membershipNumber)); // Keep generating if it already exists
        return membershipNumber;
    }

}
