package com.anastasia.Anastasia_BackEnd.service.registration;

import com.anastasia.Anastasia_BackEnd.mappers.MemberMapper;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.member.MemberDTO;
import com.anastasia.Anastasia_BackEnd.model.member.MemberEntity;
import com.anastasia.Anastasia_BackEnd.model.member.MemberResponse;
import com.anastasia.Anastasia_BackEnd.model.member.MemberStatus;
import com.anastasia.Anastasia_BackEnd.model.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.model.user.UserType;
import com.anastasia.Anastasia_BackEnd.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.UserRepository;
import com.anastasia.Anastasia_BackEnd.repository.registration.MemberRepository;
import com.anastasia.Anastasia_BackEnd.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final ChurchRepository churchRepository;
    private final UserRepository userRepository;
    private final MemberMapper memberMapper;
    private final SecurityUtils securityUtils;

    @Override
    public MemberEntity convertToEntity(MemberDTO memberDTO) {
        return memberMapper.memberDTOToEntity(memberDTO);
    }

    @Override
    public MemberDTO convertToDTO(MemberEntity memberEntity) {
        return memberMapper.memberEntityToDTO(memberEntity);
    }

    @Override
    public MemberResponse registerMember(MemberEntity memberEntity) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)){
            throw new IllegalStateException("User not authenticated");
        }
        UserEntity user = userRepository.findById(userPrincipal.getUserUuid())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));


        ChurchEntity church = churchRepository.findByChurchNumber(memberEntity.getChurchNumber())
                .orElseThrow(() -> new IllegalStateException("No valid church number provided"));

        // alternative
        // Set user reference **without fetching from DB**
//        UserEntity userReference = new UserEntity();
//        userReference.setId(userPrincipal.getId());  // Only setting the ID, no need to load from DB
//        memberEntity.setUser(userReference);

        memberEntity.setMembershipNumber(generateUniqueMembershipNumber(6, memberEntity.isDeacon()));
        memberEntity.setUser(user);
        memberEntity.setChurch(church);
        memberEntity.setApprovedByChurch(false);
        memberEntity.setApprovedByPriest(false);
        MemberEntity membership = memberRepository.save(memberEntity);

        user.assignMembership(membership);
        user.assignTenant(church.getTenant());
        user.setUserType(UserType.MEMBER);
        userRepository.save(user);


        return MemberResponse.builder()
                .name(membership.getFirstName() + " " + membership.getFatherName() + " " + membership.getGrandFatherName())
                .membershipNumber(membership.getMembershipNumber())
                .fatherOfConfession(membership.getFatherOfConfession())
                .build();
    }

    @Override
    public Page<MemberEntity> findAll(Pageable pageable) {
        return memberRepository.findAll(pageable);
    }

    @Override
    public Optional<MemberEntity> findMemberById(Long memberId) {
        return memberRepository.findById(memberId);
    }

    @Override
    public void updateMembershipDetails(Long memberId, MemberDTO request) {
        memberRepository.findById(memberId).ifPresent(memberEntity -> {

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
            Optional.ofNullable(request.getEritreaContact()).ifPresent(memberEntity::setEritreaContact);
            Optional.ofNullable(request.getMaritalStatus()).ifPresent(memberEntity::setMaritalStatus);
            Optional.of(request.getNumberOfChildren()).ifPresent(memberEntity::setNumberOfChildren); // primitive int

            Optional.ofNullable(request.getFirstLanguage()).ifPresent(memberEntity::setFirstLanguage);
            Optional.ofNullable(request.getSecondLanguage()).ifPresent(memberEntity::setSecondLanguage);
            Optional.ofNullable(request.getProfession()).ifPresent(memberEntity::setProfession);
            Optional.ofNullable(request.getLevelOfEducation()).ifPresent(memberEntity::setLevelOfEducation);
            Optional.ofNullable(request.getFatherOfConfession()).ifPresent(memberEntity::setFatherOfConfession);
            Optional.ofNullable(request.getSpouseIdNumber()).ifPresent(memberEntity::setSpouseIdNumber);

            Optional.ofNullable(request.getAddress()).ifPresent(memberEntity::setAddress);

            memberRepository.save(memberEntity);
        });
    }

    @Override
    public void deleteMembership(Long memberId) {
        memberRepository.deleteById(memberId);
    }

    @Override
    public void approveByChurch(Long memberId) {
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(()-> new UsernameNotFoundException("Not valid member"));

        member.setApprovedByPriest(true);

        if(member.isApprovedByPriest() && member.isApprovedByChurch()){
            member.setStatus(MemberStatus.APPROVED.name());
        }

        memberRepository.save(member);
    }

    @Override
    public void approveByPriest(Long memberId) {
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(()-> new UsernameNotFoundException("Not valid member"));
        member.setApprovedByChurch(true);

        if(member.isApprovedByPriest() && member.isApprovedByChurch()){
            member.setStatus(MemberStatus.APPROVED.name());
        }
        memberRepository.save(member);
    }

    @Override
    public Page<MemberEntity> findAllBySpecification(Specification<MemberEntity> spec, Pageable pageable) {
        return memberRepository.findAll(spec, pageable);
    }

    private String generateUniqueMembershipNumber(int length, boolean isDeacon) {

        String baseLetter = "M";

        if(isDeacon){
            baseLetter = "D";
        }

        String membershipNumber;
        do {
            membershipNumber = securityUtils.generateUniqueIDNumber(length, baseLetter);
        } while (memberRepository.existsByMembershipNumber(membershipNumber)); // Keep generating if it already exists
        return membershipNumber;
    }

}
