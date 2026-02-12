package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.events.MemberBirthdayEvent;
import com.anastasia.Anastasia_BackEnd.core.outbox.OutboxPublisher;
import com.anastasia.Anastasia_BackEnd.modules.registration.mappers.MemberMapper;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.*;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserType;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import com.anastasia.Anastasia_BackEnd.common.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final ChurchRepository churchRepository;
    private final UserRepository userRepository;
    private final MemberMapper memberMapper;
    private final SecurityUtils securityUtils;
    private final ApplicationEventPublisher publisher;
    private final CacheManager cacheManager;

    private final OutboxPublisher outboxPublisher;



    @Override
    public Adult_MemberEntity convertToEntity(Adult_MemberDTO adultMemberDTO) {
        return memberMapper.memberDTOToEntity(adultMemberDTO);
    }

    @Override
    public Adult_MemberDTO convertToDTO(Adult_MemberEntity adultMemberEntity) {
        return memberMapper.memberEntityToDTO(adultMemberEntity);
    }

    @Override
    public Adult_MemberResponse convertToResponse(Adult_MemberEntity adultMemberEntity) {
        return memberMapper.memberEntityToResponse(adultMemberEntity);
    }

   @CacheEvict(
           value = "members_all",
           keyGenerator = "tenantAwareKeyGenerator",
           allEntries = true)
    @Override
    public MemberResponse registerMember(Adult_MemberEntity adultMemberEntity) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)){
            throw new IllegalStateException("User not authenticated");
        }
        UserEntity user = userRepository.findById(userPrincipal.getUserUuid())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));


        ChurchEntity church = churchRepository.findByChurchNumber(adultMemberEntity.getChurchNumber())
                .orElseThrow(() -> new IllegalStateException("No valid church number provided"));

        // alternative
        // Set user reference **without fetching from DB**
//        UserEntity userReference = new UserEntity();
//        userReference.setId(userPrincipal.getId());  // Only setting the ID, no need to load from DB
//        memberEntity.setUser(userReference);

        adultMemberEntity.setMembershipNumber(generateUniqueMembershipNumber(6, adultMemberEntity.isDeacon()));
        adultMemberEntity.setUser(user);
        adultMemberEntity.setChurch(church);
        Optional.ofNullable(church.getTenant())
                .ifPresent(tenant -> adultMemberEntity.setTenantId(tenant.getId()));
        adultMemberEntity.setApprovedByChurch(false);
        adultMemberEntity.setApprovedByPriest(false);
        adultMemberEntity.setStatus(MemberStatus.PENDING.name());
        Adult_MemberEntity membership = memberRepository.save(adultMemberEntity);

        user.assignMembership(membership);
        user.assignTenant(church.getTenant());
        user.setUserType(UserType.MEMBER);
        userRepository.save(user);

       outboxPublisher.publish(
               RegistrationEventType.MEMBER_REGISTERED,
               membership.getTenantId(),
               membership.getId().toString(),
               new RegistrationCompletedEvent(
                       TenantContext.getTenantId(),
                       membership.getId(),
                       membership.getEmail(),
                       membership.getFirstName() + " " + membership.getFatherName(),
                       Instant.now()
               )
       );




       return MemberResponse.builder()
                .name(membership.getFirstName() + " " + membership.getFatherName() + " " + membership.getGrandFatherName())
                .membershipNumber(membership.getMembershipNumber())
                .fatherOfConfession(membership.getFatherOfConfession())
                .build();
    }


    @Override
    public Page<Adult_MemberResponse> findAll(Pageable pageable) {
        return memberRepository.findByStatusNotAndTenantId(
                        MemberStatus.PENDING.name(),
                        requireTenantId(),
                        pageable)
                .map(memberMapper::memberEntityToResponse);
    }

    @Override
    public long countNonPending() {
        return memberRepository.countByStatusNotAndTenantId(
                MemberStatus.PENDING.name(),
                requireTenantId());
    }

    @Override
    public Page<Adult_MemberResponse> findByTenantAndPriestNumber(UUID tenantId, String priestNumber, Pageable pageable) {
        UUID effectiveTenantId = tenantId != null ? tenantId : requireTenantId();
        return memberRepository.findByTenantIdAndPriestNumber(effectiveTenantId, priestNumber, pageable)
                .map(memberMapper::memberEntityToResponse);
    }

    @Override
    public Page<Adult_MemberResponse> findByTenantAndPriestNumberAndStatus(UUID tenantId, String priestNumber, String status, Pageable pageable) {
        UUID effectiveTenantId = tenantId != null ? tenantId : requireTenantId();
        return memberRepository.findByTenantIdAndPriestNumberAndStatus(effectiveTenantId, priestNumber, status, pageable)
                .map(memberMapper::memberEntityToResponse);
    }

    @Override
    public Page<Adult_MemberEntity> findPending(Pageable pageable) {
        return memberRepository.findByStatusAndTenantId(
                MemberStatus.PENDING.name(),
                requireTenantId(),
                pageable);
    }

    @Override
    public Page<Adult_MemberResponse> findPendingByTenantAndPriestNumber(UUID tenantId, String priestNumber, Pageable pageable) {
        UUID effectiveTenantId = tenantId != null ? tenantId : requireTenantId();
        return memberRepository.findByTenantIdAndPriestNumberAndStatus(
                        effectiveTenantId,
                        priestNumber,
                        MemberStatus.PENDING.name(),
                        pageable)
                .map(memberMapper::memberEntityToResponse);
    }

    @Override
    public Page<Adult_MemberEntity> searchNonPending(Pageable pageable, String query) {
        if (query == null || query.isBlank()) {
            return memberRepository.findByStatusNotAndTenantId(
                    MemberStatus.PENDING.name(),
                    requireTenantId(),
                    pageable);
        }
        return memberRepository.searchNonPending(
                query.trim(),
                MemberStatus.PENDING.name(),
                requireTenantId(),
                pageable);
    }

    @Cacheable(value = "members", keyGenerator = "tenantAwareKeyGenerator")
    @Override
    public Optional<Adult_MemberEntity> findMemberById(Long memberId) {
        return memberRepository.findByIdAndTenantId(memberId, requireTenantId());
    }

    @Caching(
//            put = {@CachePut(value = "members",
//                    key = "#memberId",
//                    keyGenerator = "tenantAwareKeyGenerator")},
            evict = {@CacheEvict( value = "members_all",
                    key = "#memberId",
//                    keyGenerator = "tenantAwareKeyGenerator",
                    allEntries = true)}
    )
    @Override
    public void updateMembershipDetails(Long memberId, Adult_MemberDTO request) {
        memberRepository.findByIdAndTenantId(memberId, requireTenantId()).ifPresent(memberEntity -> {

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
            Optional.ofNullable(request.getPriestNumber()).ifPresent(memberEntity::setPriestNumber);
            Optional.ofNullable(request.getSpouseIdNumber()).ifPresent(memberEntity::setSpouseIdNumber);

            Optional.ofNullable(request.getAddress()).ifPresent(memberEntity::setAddress);
            Optional.ofNullable(request.getTermsVersion()).ifPresent(memberEntity::setTermsVersion);
            Optional.ofNullable(request.getTermsAcceptedAt()).ifPresent(memberEntity::setTermsAcceptedAt);
            if (request.isTermsAccepted()) {
                memberEntity.setTermsAccepted(true);
            }

            memberRepository.save(memberEntity);
        });
    }

    @Caching(
            evict = {
                    @CacheEvict(value = "members",
                            key = "#memberId"
//                            keyGenerator = "tenantAwareKeyGenerator"
                    ),
                    @CacheEvict(value = "members_all",
                            keyGenerator = "tenantAwareKeyGenerator",
                            allEntries = true
                    )
            }
    )
    @Override
    public void deleteMembership(Long memberId) {
        memberRepository.findByIdAndTenantId(memberId, requireTenantId())
                .ifPresent(memberRepository::delete);
    }

    @CachePut(value = "members", key = "#memberId"
//            , keyGenerator = "tenantAwareKeyGenerator"
    )
    @Override
    public Adult_MemberResponse approveByChurch(Long memberId) {
        Adult_MemberEntity member = memberRepository.findByIdAndTenantId(memberId, requireTenantId())
                .orElseThrow(() -> new UsernameNotFoundException("Not valid member"));

        member.setApprovedByChurch(true);
        updateApprovalStatus(member);

        Adult_MemberEntity saved = memberRepository.save(member);
        return convertToResponse(saved);
    }

    @CachePut(value = "members", key = "#memberId"
//            , keyGenerator = "tenantAwareKeyGenerator"
    )
    @Override
    public Adult_MemberResponse approveByPriest(Long memberId) {
        Adult_MemberEntity member = memberRepository.findByIdAndTenantId(memberId, requireTenantId())
                .orElseThrow(() -> new UsernameNotFoundException("Not valid member"));
        member.setApprovedByPriest(true);

        updateApprovalStatus(member);
        Adult_MemberEntity saved = memberRepository.save(member);
        return convertToResponse(saved);
    }

    @Override
    public Page<Adult_MemberEntity> findAllBySpecification(Specification<Adult_MemberEntity> spec, Pageable pageable) {
        Specification<Adult_MemberEntity> tenantSpec = (root, query, cb) ->
                cb.equal(root.get("tenantId"), requireTenantId());
        Specification<Adult_MemberEntity> combinedSpec = Specification.where(tenantSpec).and(spec);
        return memberRepository.findAll(combinedSpec, pageable);
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

    public void checkBirthdays() {
        List<Adult_MemberEntity> birthdaysToday = memberRepository.findByBirthday(LocalDate.now());
        for (Adult_MemberEntity member : birthdaysToday) {
            publisher.publishEvent(new MemberBirthdayEvent(this, member));
        }
    }

    @CacheEvict(
            value = "members_all",
            allEntries = true)
    public void clearAllCache() {
        System.out.println("Clearing members_all cache...");
    }

    public void evictMemberCachesForTenant(String tenantId) {
        cacheManager.getCache("members").invalidate(); // clear all if needed
        cacheManager.getCache("members_all").invalidate();
    }

    private void updateApprovalStatus(Adult_MemberEntity member) {
        boolean requiresPriestApproval = org.springframework.util.StringUtils.hasText(member.getPriestNumber());
        if (!requiresPriestApproval && member.isApprovedByChurch()) {
            member.setStatus(MemberStatus.APPROVED.name());
            return;
        }
        if (requiresPriestApproval && member.isApprovedByChurch() && member.isApprovedByPriest()) {
            member.setStatus(MemberStatus.APPROVED.name());
        }
    }

    private UUID requireTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is missing for member access");
        }
        return tenantId;
    }

}
