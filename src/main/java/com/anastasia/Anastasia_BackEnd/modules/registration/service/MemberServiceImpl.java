package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.core.notification.service.TenantAdminNotificationService;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.events.MemberBirthdayEvent;
import com.anastasia.Anastasia_BackEnd.core.outbox.OutboxPublisher;
import com.anastasia.Anastasia_BackEnd.modules.registration.mappers.MemberMapper;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.family.UpdateFamilyRelationshipRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.family.UpsertFamilyRelationshipRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.MemberLifecycleStatus;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetType;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.*;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.family.FamilyMemberSourceType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.family.FamilyMemberSummaryResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.family.FamilyRelationshipEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.family.FamilyRelationshipType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.family.MyFamilyResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserType;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChildRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.FamilyRelationshipRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.PriestRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.ActiveMemberLimitPolicy;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.card.MembershipCardService;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final ChildRepository childRepository;
    private final FamilyRelationshipRepository familyRelationshipRepository;
    private final ChurchRepository churchRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MemberMapper memberMapper;
    private final SecurityUtils securityUtils;
    private final ApplicationEventPublisher publisher;
    private final CacheManager cacheManager;
    private final PriestRepository priestRepository;
    private final ActiveMemberLimitPolicy activeMemberLimitPolicy;
    private final MembershipCardService membershipCardService;

    private final OutboxPublisher outboxPublisher;
    private final TenantAdminNotificationService tenantAdminNotificationService;
    private final LocalizedMessageService messageService;



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
        if (adultMemberEntity == null) {
            return null;
        }
        enrichChildMetadata(List.of(adultMemberEntity), adultMemberEntity.getTenantId());
        return memberMapper.memberEntityToResponse(adultMemberEntity);
    }

    private Page<Adult_MemberResponse> mapMembersToResponse(Page<Adult_MemberEntity> members, UUID tenantId) {
        enrichChildMetadata(members.getContent(), tenantId);
        return members.map(memberMapper::memberEntityToResponse);
    }

    private void enrichChildMetadata(Collection<? extends Adult_MemberEntity> members, UUID tenantId) {
        if (members == null || members.isEmpty()) {
            return;
        }
        Set<Long> ownerIds = members.stream()
                .map(Adult_MemberEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ownerIds.isEmpty()) {
            members.forEach(member -> {
                if (member != null) {
                    member.setChildrenAsFatherIds(Collections.emptySet());
                    member.setChildrenAsMotherIds(Collections.emptySet());
                    member.setNumberOfChildren(0);
                }
            });
            return;
        }
        UUID effectiveTenantId = tenantId != null ? tenantId : requireTenantId();
        List<FamilyRelationshipRepository.OwnerChildRelationship> relationships =
                familyRelationshipRepository.findChildRelationshipsByOwnerIdsAndTenantIdAndRelationshipType(
                        ownerIds,
                        effectiveTenantId,
                        FamilyRelationshipType.CHILD);

        Map<Long, Set<Long>> fatherIndex = new HashMap<>();
        Map<Long, Set<Long>> motherIndex = new HashMap<>();
        for (FamilyRelationshipRepository.OwnerChildRelationship relationship : relationships) {
            Long ownerId = relationship.getOwnerMemberId();
            Long childId = relationship.getChildId();
            if (ownerId == null || childId == null) {
                continue;
            }
            if (Boolean.TRUE.equals(relationship.getFather())) {
                fatherIndex.computeIfAbsent(ownerId, id -> new HashSet<>()).add(childId);
            }
            if (Boolean.TRUE.equals(relationship.getMother())) {
                motherIndex.computeIfAbsent(ownerId, id -> new HashSet<>()).add(childId);
            }
        }

        for (Adult_MemberEntity member : members) {
            if (member == null) {
                continue;
            }
            Long memberId = member.getId();
            Set<Long> fatherChildren = memberId != null ? fatherIndex.getOrDefault(memberId, Collections.emptySet()) : Collections.emptySet();
            Set<Long> motherChildren = memberId != null ? motherIndex.getOrDefault(memberId, Collections.emptySet()) : Collections.emptySet();
            member.setChildrenAsFatherIds(fatherChildren.isEmpty() ? Collections.emptySet() : Set.copyOf(fatherChildren));
            member.setChildrenAsMotherIds(motherChildren.isEmpty() ? Collections.emptySet() : Set.copyOf(motherChildren));
            member.setNumberOfChildren(Math.max(fatherChildren.size(), motherChildren.size()));
        }
    }

   @CacheEvict(
           value = "members_all",
           keyGenerator = "tenantAwareKeyGenerator",
           allEntries = true)
    @Override
    public Adult_MemberResponse registerMember(Adult_MemberEntity adultMemberEntity) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)){
            throw new IllegalStateException(messageService.get(
                    "auth.user.notAuthenticated",
                    "User not authenticated"
            ));
        }
        UserEntity user = userRepository.findById(userPrincipal.getUserUuid())
                .orElseThrow(() -> new IllegalStateException(messageService.get(
                        "auth.user.notAuthenticated",
                        "Authenticated user not found"
                )));


        ChurchEntity church = resolveChurch(adultMemberEntity.getChurchNumber());

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
        stampAvatar(adultMemberEntity, user.getUuid());
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

        tenantAdminNotificationService.notifyMemberRegistrationSubmitted(membership, user.getUuid());

       return convertToResponse(membership);
    }


    @Override
    public Page<Adult_MemberResponse> findAll(Pageable pageable) {
        UUID tenantId = requireTenantId();
        Page<Adult_MemberEntity> members = memberRepository.findByStatusValueNotAndTenantId(
                MemberLifecycleStatus.PENDING,
                tenantId,
                pageable);
        return mapMembersToResponse(members, tenantId);
    }

    @Override
    public Page<Adult_MemberSummaryResponse> findAllSummary(Pageable pageable, String language) {
        return memberRepository.findByStatusValueNotAndTenantId(
                        MemberLifecycleStatus.PENDING,
                        requireTenantId(),
                        pageable)
                .map(member -> memberMapper.memberEntityToSummaryResponse(member, language));
    }

    @Override
    public long countNonPending() {
        return memberRepository.countByStatusValueNotAndTenantId(
                MemberLifecycleStatus.PENDING,
                requireTenantId());
    }

    @Override
    public Page<Adult_MemberResponse> findByTenantAndPriestNumber(UUID tenantId, String priestNumber, Pageable pageable) {
        UUID effectiveTenantId = tenantId != null ? tenantId : requireTenantId();
        Page<Adult_MemberEntity> members = memberRepository.findByTenantIdAndPriestNumber(effectiveTenantId, priestNumber, pageable);
        return mapMembersToResponse(members, effectiveTenantId);
    }

    @Override
    public Page<Adult_MemberSummaryResponse> findByTenantAndPriestNumberSummary(UUID tenantId, String priestNumber, Pageable pageable, String language) {
        UUID effectiveTenantId = tenantId != null ? tenantId : requireTenantId();
        return memberRepository.findByTenantIdAndPriestNumber(effectiveTenantId, priestNumber, pageable)
                .map(member -> memberMapper.memberEntityToSummaryResponse(member, language));
    }

    @Override
    public Page<Adult_MemberResponse> findByTenantAndPriestNumberAndStatus(UUID tenantId, String priestNumber, String status, Pageable pageable) {
        UUID effectiveTenantId = tenantId != null ? tenantId : requireTenantId();
        Page<Adult_MemberEntity> members = memberRepository.findByTenantIdAndPriestNumberAndStatusValue(
                effectiveTenantId,
                priestNumber,
                MemberLifecycleStatus.from(status),
                pageable);
        return mapMembersToResponse(members, effectiveTenantId);
    }

    @Override
    public Page<Adult_MemberSummaryResponse> findByTenantAndPriestNumberAndStatusSummary(UUID tenantId, String priestNumber, String status, Pageable pageable, String language) {
        UUID effectiveTenantId = tenantId != null ? tenantId : requireTenantId();
        return memberRepository.findByTenantIdAndPriestNumberAndStatusValue(
                        effectiveTenantId,
                        priestNumber,
                        MemberLifecycleStatus.from(status),
                        pageable)
                .map(member -> memberMapper.memberEntityToSummaryResponse(member, language));
    }

    @Override
    public Page<Adult_MemberResponse> findPending(Pageable pageable) {
        UUID tenantId = requireTenantId();
        Page<Adult_MemberEntity> members = memberRepository.findByStatusValueAndTenantId(
                MemberLifecycleStatus.PENDING,
                tenantId,
                pageable);
        return mapMembersToResponse(members, tenantId);
    }

    @Override
    public Page<Adult_MemberResponse> findPendingByTenantAndPriestNumber(UUID tenantId, String priestNumber, Pageable pageable) {
        UUID effectiveTenantId = tenantId != null ? tenantId : requireTenantId();
        Page<Adult_MemberEntity> members = memberRepository.findByTenantIdAndPriestNumberAndStatusValue(
                effectiveTenantId,
                priestNumber,
                MemberLifecycleStatus.PENDING,
                pageable);
        return mapMembersToResponse(members, effectiveTenantId);
    }

    @Override
    public Page<Adult_MemberResponse> searchNonPending(Pageable pageable, String query) {
        UUID tenantId = requireTenantId();
        Long churchId = resolveCurrentChurchId();
        String search = query == null ? null : query.trim();

        Specification<Adult_MemberEntity> scopeSpec = (root, cq, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            predicates.add(cb.notEqual(root.get("statusValue"), MemberLifecycleStatus.PENDING));
            if (churchId != null) {
                predicates.add(cb.equal(root.get("churchId"), churchId));
            }
            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(root.get("firstName"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("fatherName"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("grandFatherName"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("firstNameT"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("fatherNameT"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("grandFatherNameT"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("membershipNumber"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("email"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("profession"), "")), like)
                ));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<Adult_MemberEntity> members = memberRepository.findAll(scopeSpec, pageable);
        return mapMembersToResponse(members, tenantId);
    }

    @Override
    public Page<Adult_MemberSummaryResponse> searchNonPendingSummary(Pageable pageable, String query, String language) {
        UUID tenantId = requireTenantId();
        Long churchId = resolveCurrentChurchId();
        String search = query == null ? null : query.trim();

        Specification<Adult_MemberEntity> scopeSpec = (root, cq, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            predicates.add(cb.notEqual(root.get("statusValue"), MemberLifecycleStatus.PENDING));
            if (churchId != null) {
                predicates.add(cb.equal(root.get("churchId"), churchId));
            }
            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(root.get("firstName"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("fatherName"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("grandFatherName"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("firstNameT"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("fatherNameT"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("grandFatherNameT"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("membershipNumber"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("email"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("profession"), "")), like)
                ));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return memberRepository.findAll(scopeSpec, pageable)
                .map(member -> memberMapper.memberEntityToSummaryResponse(member, language));
    }

    @Cacheable(value = "members", keyGenerator = "tenantAwareKeyGenerator", unless = "#result == null")
    @Override
    public Optional<Adult_MemberResponse> findMemberById(Long memberId) {
        return memberRepository.findByIdAndTenantId(memberId, requireTenantId())
                .map(this::convertToResponse);
    }

    @Caching(
            put = {@CachePut(value = "members", key = "#root.target.memberCacheKey(#memberId)")},
            evict = {@CacheEvict(value = "members_all",
                    keyGenerator = "tenantAwareKeyGenerator",
                    allEntries = true)}
    )
    @Override
    public Adult_MemberResponse updateMembershipDetails(Long memberId, Adult_MemberDTO request) {
        Adult_MemberEntity memberEntity = memberRepository.findByIdAndTenantId(memberId, requireTenantId())
                .orElseThrow(() -> new UsernameNotFoundException(messageService.get(
                        "registration.member.invalid",
                        "Not valid member"
                )));

            Optional.ofNullable(request.getChurchNumber()).ifPresent(churchNumber -> {
                ChurchEntity church = resolveChurch(churchNumber);
                memberEntity.setChurchNumber(church.getChurchNumber());
                memberEntity.setChurch(church);
                if (church.getTenant() != null) {
                    memberEntity.setTenantId(church.getTenant().getId());
                }
            });
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
            Optional.ofNullable(request.getVillage()).ifPresent(memberEntity::setVillage);
            Optional.ofNullable(request.getEmail()).ifPresent(memberEntity::setEmail);
            Optional.ofNullable(request.getPhone()).ifPresent(memberEntity::setPhone);
            Optional.ofNullable(request.getWhatsApp()).ifPresent(memberEntity::setWhatsApp);
            Optional.ofNullable(request.getEmergencyContactNumber()).ifPresent(memberEntity::setEmergencyContactNumber);
            Optional.ofNullable(request.getContactRelation()).ifPresent(memberEntity::setContactRelation);
            Optional.ofNullable(request.getEritreaContact()).ifPresent(memberEntity::setEritreaContact);
            Optional.ofNullable(request.getMaritalStatus())
                    .map(MaritalStatus::from)
                    .ifPresent(memberEntity::setMaritalStatus);
            Optional.of(request.getNumberOfChildren()).ifPresent(memberEntity::setNumberOfChildren); // primitive int

            Optional.ofNullable(request.getFirstLanguage()).ifPresent(memberEntity::setFirstLanguage);
            Optional.ofNullable(request.getSecondLanguage()).ifPresent(memberEntity::setSecondLanguage);
            Optional.ofNullable(request.getProfession()).ifPresent(memberEntity::setProfession);
            Optional.ofNullable(request.getLevelOfEducation()).ifPresent(memberEntity::setLevelOfEducation);
            Optional.ofNullable(request.getFatherOfConfession()).ifPresent(memberEntity::setFatherOfConfession);
            Optional.ofNullable(request.getChurchOfBaptism()).ifPresent(memberEntity::setChurchOfBaptism);
            Optional.ofNullable(request.getBaptismName()).ifPresent(memberEntity::setBaptismName);
            Optional.ofNullable(request.getPriestNumber()).ifPresent(memberEntity::setPriestNumber);
            Optional.ofNullable(request.getSpouseIdNumber()).ifPresent(memberEntity::setSpouseIdNumber);

            Optional.ofNullable(request.getAddress()).ifPresent(memberEntity::setAddress);
            Optional.ofNullable(request.getAvatar()).ifPresent(avatar -> {
                memberEntity.setAvatar(mapAvatar(avatar));
                stampAvatar(memberEntity, memberEntity.getUserId());
            });
            Optional.ofNullable(request.getTermsVersion()).ifPresent(memberEntity::setTermsVersion);
            Optional.ofNullable(request.getTermsAcceptedAt()).ifPresent(memberEntity::setTermsAcceptedAt);
            if (request.isTermsAccepted()) {
                memberEntity.setTermsAccepted(true);
            }

            Adult_MemberEntity saved = memberRepository.save(memberEntity);
            return convertToResponse(saved);
    }

    @Caching(
            evict = {
                    @CacheEvict(value = "members",
                            key = "#root.target.memberCacheKey(#memberId)"
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

    public String memberCacheKey(Long memberId) {
        return "tenant:" + requireTenantId() + ":" + memberId;
    }

    @Caching(
            put = {@CachePut(value = "members", keyGenerator = "tenantAwareKeyGenerator")},
            evict = {@CacheEvict(value = "members_all", keyGenerator = "tenantAwareKeyGenerator", allEntries = true)}
    )
    @Override
    public Adult_MemberResponse approveByChurch(Long memberId) {
        UUID tenantId = requireTenantId();
        Adult_MemberEntity member = memberRepository.findByIdAndTenantId(memberId, tenantId)
                .orElseThrow(() -> new UsernameNotFoundException(messageService.get(
                        "registration.member.invalid",
                        "Not valid member"
                )));

        String previousStatus = member.getStatus();
        boolean wasActive = MemberStatus.ACTIVE.name().equals(member.getStatus());
        member.setApprovedByChurch(true);
        updateApprovalStatus(member);
        if (!wasActive && MemberStatus.ACTIVE.name().equals(member.getStatus())) {
            activeMemberLimitPolicy.assertCanActivateMembers(tenantId, 1);
        }
        assignMemberRoleIfApproved(member);

        Adult_MemberEntity saved = memberRepository.save(member);
        if (!MemberStatus.ACTIVE.name().equals(previousStatus)
                && MemberStatus.ACTIVE.name().equals(saved.getStatus())) {
            membershipCardService.issueOrRefreshForApprovedMember(saved);
        }
        return convertToResponse(saved);
    }

    @Caching(
            put = {@CachePut(value = "members", keyGenerator = "tenantAwareKeyGenerator")},
            evict = {@CacheEvict(value = "members_all", keyGenerator = "tenantAwareKeyGenerator", allEntries = true)}
    )
    @Override
    public Adult_MemberResponse approveByPriest(Long memberId) {
        UUID tenantId = requireTenantId();
        Adult_MemberEntity member = memberRepository.findByIdAndTenantId(memberId, tenantId)
                .orElseThrow(() -> new UsernameNotFoundException(messageService.get(
                        "registration.member.invalid",
                        "Not valid member"
                )));
        String previousStatus = member.getStatus();
        boolean wasApproved = member.isApprovedByPriest();
        boolean wasActive = MemberStatus.ACTIVE.name().equals(member.getStatus());
        member.setApprovedByPriest(true);

        updateApprovalStatus(member);
        if (!wasActive && MemberStatus.ACTIVE.name().equals(member.getStatus())) {
            activeMemberLimitPolicy.assertCanActivateMembers(tenantId, 1);
        }
        assignMemberRoleIfApproved(member);
        if (!wasApproved && member.getPriestNumber() != null) {
            priestRepository.findByPriestNumber(member.getPriestNumber())
                    .ifPresent(priest -> {
                        priest.setSpiritualChildren(priest.getSpiritualChildren() + 1);
                        priestRepository.save(priest);
                    });
        }
        Adult_MemberEntity saved = memberRepository.save(member);
        if (!MemberStatus.ACTIVE.name().equals(previousStatus)
                && MemberStatus.ACTIVE.name().equals(saved.getStatus())) {
            membershipCardService.issueOrRefreshForApprovedMember(saved);
        }
        return convertToResponse(saved);
    }

    @Override
    public Page<Adult_MemberResponse> findAllBySpecification(Specification<Adult_MemberEntity> spec, Pageable pageable) {
        Specification<Adult_MemberEntity> tenantSpec = (root, query, cb) ->
                cb.equal(root.get("tenantId"), requireTenantId());
        Specification<Adult_MemberEntity> combinedSpec = Specification.where(tenantSpec).and(spec);
        Page<Adult_MemberEntity> members = memberRepository.findAll(combinedSpec, pageable);
        return mapMembersToResponse(members, requireTenantId());
    }

    @Override
    @Transactional
    public MyFamilyResponse getCurrentUserFamily() {
        UUID tenantId = requireTenantId();
        UUID userId = requireCurrentUserId();

        Adult_MemberEntity self = memberRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new UsernameNotFoundException("Current user does not have a member profile"));

        ensureFamilyRelationshipsSeeded(self, tenantId);

        List<FamilyRelationshipEntity> relationships =
                familyRelationshipRepository.findByOwnerMemberIdAndTenantIdOrderByRelationshipTypeAscSortOrderAscIdAsc(
                        self.getId(),
                        tenantId
                );

        return MyFamilyResponse.builder()
                .self(toAdultSummary(self, FamilyRelationshipType.SELF, true, false, true))
                .spouse(relationships.stream()
                        .filter(relationship -> relationship.getRelationshipType() == FamilyRelationshipType.SPOUSE)
                        .map(this::toSummary)
                        .findFirst()
                        .orElse(null))
                .children(relationships.stream()
                        .filter(relationship -> relationship.getRelationshipType() == FamilyRelationshipType.CHILD)
                        .map(this::toSummary)
                        .toList())
                .parents(relationships.stream()
                        .filter(relationship -> relationship.getRelationshipType() == FamilyRelationshipType.PARENT)
                        .map(this::toSummary)
                        .toList())
                .inLaws(relationships.stream()
                        .filter(relationship -> relationship.getRelationshipType() == FamilyRelationshipType.IN_LAW)
                        .map(this::toSummary)
                        .toList())
                .build();
    }

    @Override
    @Transactional
    public FamilyMemberSummaryResponse createFamilyRelationship(UpsertFamilyRelationshipRequest request) {
        UUID tenantId = requireTenantId();
        Adult_MemberEntity owner = requireCurrentMember();
        ensureFamilyRelationshipsSeeded(owner, tenantId);
        assertMemberSelfServiceCreateAllowed(request);

        FamilyRelationshipEntity entity = buildRelationshipEntity(owner, tenantId, request);
        validateNoDuplicate(owner, tenantId, entity, null);
        return toSummary(familyRelationshipRepository.save(entity));
    }

    @Override
    @Transactional
    public FamilyMemberSummaryResponse updateFamilyRelationship(Long relationshipId, UpdateFamilyRelationshipRequest request) {
        UUID tenantId = requireTenantId();
        Adult_MemberEntity owner = requireCurrentMember();
        ensureFamilyRelationshipsSeeded(owner, tenantId);

        FamilyRelationshipEntity existing = familyRelationshipRepository
                .findByIdAndOwnerMemberIdAndTenantId(relationshipId, owner.getId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException(messageService.get(
                        "registration.familyRelationship.notFound",
                        "Family relationship not found"
                )));

        assertMemberSelfServiceUpdateAllowed(existing, request);
        FamilyRelationshipEntity updated = mergeRelationship(existing, request, tenantId, owner);
        validateNoDuplicate(owner, tenantId, updated, existing.getId());
        return toSummary(familyRelationshipRepository.save(updated));
    }

    @Override
    @Transactional
    public void deleteFamilyRelationship(Long relationshipId) {
        UUID tenantId = requireTenantId();
        Adult_MemberEntity owner = requireCurrentMember();
        FamilyRelationshipEntity existing = familyRelationshipRepository
                .findByIdAndOwnerMemberIdAndTenantId(relationshipId, owner.getId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException(messageService.get(
                        "registration.familyRelationship.notFound",
                        "Family relationship not found"
                )));
        familyRelationshipRepository.delete(existing);
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

    private Long resolveCurrentChurchId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return userRepository.findById(principal.getUserUuid())
                .map(user -> {
                    if (user.getMembership() != null && user.getMembership().getChurchId() != null) {
                        return user.getMembership().getChurchId();
                    }
                    if (user.getTenant() != null && user.getTenant().getChurch() != null) {
                        return user.getTenant().getChurch().getChurchId();
                    }
                    return null;
                })
                .orElse(null);
    }

    public void evictMemberCachesForTenant(String tenantId) {
        cacheManager.getCache("members").invalidate(); // clear all if needed
        cacheManager.getCache("members_all").invalidate();
    }

    private void updateApprovalStatus(Adult_MemberEntity member) {
        boolean requiresPriestApproval = org.springframework.util.StringUtils.hasText(member.getPriestNumber());
        if (!requiresPriestApproval && member.isApprovedByChurch()) {
            member.setStatus(MemberStatus.ACTIVE.name());
            if (member.getApprovedAt() == null) {
                member.setApprovedAt(LocalDateTime.now());
            }
            return;
        }
        if (requiresPriestApproval && member.isApprovedByChurch() && member.isApprovedByPriest()) {
            member.setStatus(MemberStatus.ACTIVE.name());
            if (member.getApprovedAt() == null) {
                member.setApprovedAt(LocalDateTime.now());
            }
        }
    }

    private ChurchEntity resolveChurch(String churchNumber) {
        if (!StringUtils.hasText(churchNumber)) {
            throw new IllegalStateException(messageService.get(
                    "registration.churchNumber.invalid",
                    "No valid church number provided"
            ));
        }
        return churchRepository.findByChurchNumber(churchNumber.trim())
                .orElseThrow(() -> new IllegalStateException(messageService.get(
                        "registration.churchNumber.invalid",
                        "No valid church number provided"
                )));
    }

    private void stampAvatar(Adult_MemberEntity member, UUID uploadedByUserId) {
        if (member.getAvatar() == null) {
            return;
        }
        member.getAvatar().setImageAssetType(ImageAssetType.MEMBER);
        member.getAvatar().setTenantId(member.getTenantId());
        member.getAvatar().setUploadedByUserId(uploadedByUserId);
        if (member.getUserId() != null) {
            member.getAvatar().setOwnerId(member.getUserId());
        } else if (member.getTenantId() != null) {
            member.getAvatar().setOwnerId(member.getTenantId());
        }
    }

    private ImageAssetEntity mapAvatar(ImageAssetDTO avatar) {
        if (avatar == null) {
            return null;
        }
        return ImageAssetEntity.builder()
                .imageUrl(avatar.getImageUrl())
                .imageSize(avatar.getImageSize())
                .build();
    }

    private void assignMemberRoleIfApproved(Adult_MemberEntity member) {
        if (!MemberStatus.ACTIVE.name().equals(member.getStatus())) {
            return;
        }
        UserEntity user = member.getUser();
        if (user == null && member.getUserId() != null) {
            user = userRepository.findById(member.getUserId()).orElse(null);
        }
        if (user == null) {
            return;
        }

        Role memberRole = roleRepository.findByRoleName("MEMBER").orElse(null);
        if (memberRole == null) {
            return;
        }
        if (user.getRoles().contains(memberRole)) {
            return;
        }
        user.getRoles().add(memberRole);
        userRepository.save(user);
    }

    private UUID requireCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalStateException(messageService.get(
                    "auth.user.notAuthenticated",
                    "User not authenticated"
            ));
        }
        return principal.getUserUuid();
    }

    private Adult_MemberEntity requireCurrentMember() {
        UUID tenantId = requireTenantId();
        UUID userId = requireCurrentUserId();
        return memberRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new UsernameNotFoundException(messageService.get(
                        "registration.member.currentProfile.missing",
                        "Current user does not have a member profile"
                )));
    }

    private Optional<Adult_MemberEntity> resolveSpouse(Adult_MemberEntity self, UUID tenantId) {
        if (self.getMembershipNumber() != null) {
            Optional<Adult_MemberEntity> reverseMatch = memberRepository
                    .findFirstBySpouseIdNumberAndTenantId(self.getMembershipNumber(), tenantId)
                    .filter(candidate -> !candidate.getId().equals(self.getId()));
            if (reverseMatch.isPresent()) {
                return reverseMatch;
            }
        }

        if (self.getSpouseIdNumber() == null || self.getSpouseIdNumber().isBlank()) {
            return Optional.empty();
        }

        return memberRepository.findByMembershipNumberAndTenantId(self.getSpouseIdNumber().trim(), tenantId)
                .filter(candidate -> !candidate.getId().equals(self.getId()));
    }

    private FamilyMemberSummaryResponse toAdultSummary(Adult_MemberEntity member,
                                                       FamilyRelationshipType relationshipType,
                                                       boolean canManage,
                                                       boolean dependent,
                                                       boolean inHousehold) {
        return FamilyMemberSummaryResponse.builder()
                .relationshipId(null)
                .id("adult-" + member.getId())
                .sourceType(FamilyMemberSourceType.ADULT_MEMBER)
                .sourceId(member.getId())
                .fullName(buildAdultFullName(member))
                .relationship(relationshipType.name())
                .membershipStatus(member.getStatus())
                .canManage(canManage)
                .primaryGuardian(false)
                .accountHolder(member.getUserId() != null)
                .dependent(dependent)
                .inHousehold(inHousehold)
                .linkedToMemberProfile(true)
                .membershipNumber(member.getMembershipNumber())
                .sortOrder(0)
                .maritalStatus(member.getMaritalStatus() != null ? member.getMaritalStatus().toApiValue() : null)
                .active(true)
                .build();
    }

    private String buildAdultFullName(Adult_MemberEntity member) {
        return joinNameParts(member.getFirstName(), member.getFatherName(), member.getGrandFatherName());
    }

    private String buildChildFullName(Child_MemberEntity child) {
        return joinNameParts(child.getFirstName(), child.getFatherName(), child.getGrandFatherName());
    }

    private String buildExternalParentName(String firstPart, String secondPart) {
        return joinNameParts(firstPart, secondPart);
    }

    private FamilyMemberSummaryResponse toSummary(FamilyRelationshipEntity relationship) {
        if (relationship.getSourceType() == FamilyMemberSourceType.ADULT_MEMBER && relationship.getRelatedMember() != null) {
            Adult_MemberEntity member = relationship.getRelatedMember();
            return FamilyMemberSummaryResponse.builder()
                    .relationshipId(relationship.getId())
                    .id("adult-" + member.getId())
                    .sourceType(FamilyMemberSourceType.ADULT_MEMBER)
                    .sourceId(member.getId())
                    .fullName(buildAdultFullName(member))
                    .relationship(relationship.getRelationshipType().name())
                    .membershipStatus(member.getStatus())
                    .canManage(relationship.isCanManage())
                    .primaryGuardian(relationship.isPrimaryGuardian())
                    .accountHolder(member.getUserId() != null)
                    .dependent(relationship.isDependent())
                    .inHousehold(relationship.isInHousehold())
                    .linkedToMemberProfile(true)
                    .membershipNumber(member.getMembershipNumber())
                    .sortOrder(relationship.getSortOrder())
                    .maritalStatus(member.getMaritalStatus() != null ? member.getMaritalStatus().toApiValue() : null)
                    .active(relationship.isActive())
                    .build();
        }

        if (relationship.getSourceType() == FamilyMemberSourceType.CHILD_MEMBER && relationship.getRelatedChild() != null) {
            Child_MemberEntity child = relationship.getRelatedChild();
            return FamilyMemberSummaryResponse.builder()
                    .relationshipId(relationship.getId())
                    .id("child-" + child.getId())
                    .sourceType(FamilyMemberSourceType.CHILD_MEMBER)
                    .sourceId(child.getId())
                    .fullName(buildChildFullName(child))
                    .relationship(relationship.getRelationshipType().name())
                    .membershipStatus(child.getStatus())
                    .canManage(relationship.isCanManage())
                    .primaryGuardian(relationship.isPrimaryGuardian())
                    .accountHolder(false)
                    .dependent(relationship.isDependent())
                    .inHousehold(relationship.isInHousehold())
                    .linkedToMemberProfile(true)
                    .membershipNumber(child.getMembershipNumber())
                    .sortOrder(relationship.getSortOrder())
                    .active(relationship.isActive())
                    .build();
        }

        return FamilyMemberSummaryResponse.builder()
                .relationshipId(relationship.getId())
                .id("external-" + relationship.getId())
                .sourceType(FamilyMemberSourceType.EXTERNAL)
                .fullName(relationship.getDisplayName())
                .relationship(relationship.getRelationshipType().name())
                .canManage(relationship.isCanManage())
                .primaryGuardian(relationship.isPrimaryGuardian())
                .accountHolder(false)
                .dependent(relationship.isDependent())
                .inHousehold(relationship.isInHousehold())
                .linkedToMemberProfile(false)
                .sortOrder(relationship.getSortOrder())
                .active(relationship.isActive())
                .build();
    }

    private FamilyRelationshipEntity buildRelationshipEntity(Adult_MemberEntity owner,
                                                             UUID tenantId,
                                                             UpsertFamilyRelationshipRequest request) {
        FamilyRelationshipEntity entity = FamilyRelationshipEntity.builder()
                .tenantId(tenantId)
                .ownerMember(owner)
                .relationshipType(request.relationshipType())
                .sourceType(request.sourceType())
                .dependent(Boolean.TRUE.equals(request.dependent()))
                .inHousehold(Boolean.TRUE.equals(request.inHousehold()))
                .canManage(Boolean.TRUE.equals(request.canManage()))
                .primaryGuardian(Boolean.TRUE.equals(request.primaryGuardian()))
                .active(request.active() == null || request.active())
                .effectiveFrom(request.effectiveFrom())
                .effectiveTo(request.effectiveTo())
                .endReason(request.endReason())
                .sortOrder(request.sortOrder() != null ? request.sortOrder() : defaultSortOrder(request.relationshipType()))
                .build();
        applyRelationshipTarget(entity, request.sourceType(), request.relatedMemberId(), request.relatedChildId(), request.displayName(), tenantId, owner);
        return entity;
    }

    private FamilyRelationshipEntity mergeRelationship(FamilyRelationshipEntity entity,
                                                       UpdateFamilyRelationshipRequest request,
                                                       UUID tenantId,
                                                       Adult_MemberEntity owner) {
        FamilyMemberSourceType sourceType = entity.getSourceType() == FamilyMemberSourceType.EXTERNAL
                ? (request.sourceType() != null ? request.sourceType() : entity.getSourceType())
                : entity.getSourceType();
        FamilyRelationshipType relationshipType = request.relationshipType() != null ? request.relationshipType() : entity.getRelationshipType();

        entity.setRelationshipType(relationshipType);
        entity.setSourceType(sourceType);
        if (request.dependent() != null) {
            entity.setDependent(request.dependent());
        }
        if (request.inHousehold() != null) {
            entity.setInHousehold(request.inHousehold());
        }
        if (request.canManage() != null) {
            entity.setCanManage(request.canManage());
        }
        if (request.primaryGuardian() != null) {
            entity.setPrimaryGuardian(request.primaryGuardian());
        }
        if (request.active() != null) {
            entity.setActive(request.active());
        }
        if (request.effectiveFrom() != null) {
            entity.setEffectiveFrom(request.effectiveFrom());
        }
        if (request.effectiveTo() != null) {
            entity.setEffectiveTo(request.effectiveTo());
        }
        if (request.endReason() != null) {
            entity.setEndReason(request.endReason());
        }
        if (request.sortOrder() != null) {
            entity.setSortOrder(request.sortOrder());
        }

        applyRelationshipTarget(
                entity,
                sourceType,
                request.relatedMemberId() != null ? request.relatedMemberId() : entity.getRelatedMember() != null ? entity.getRelatedMember().getId() : null,
                request.relatedChildId() != null ? request.relatedChildId() : entity.getRelatedChild() != null ? entity.getRelatedChild().getId() : null,
                request.displayName() != null ? request.displayName() : entity.getDisplayName(),
                tenantId,
                owner
        );
        return entity;
    }

    private void applyRelationshipTarget(FamilyRelationshipEntity entity,
                                         FamilyMemberSourceType sourceType,
                                         Long relatedMemberId,
                                         Long relatedChildId,
                                         String displayName,
                                         UUID tenantId,
                                         Adult_MemberEntity owner) {
        entity.setRelatedMember(null);
        entity.setRelatedChild(null);
        entity.setDisplayName(null);

        switch (sourceType) {
            case ADULT_MEMBER -> {
                if (relatedMemberId == null) {
                    throw new IllegalArgumentException(messageService.get(
                            "registration.familyRelationship.relatedMemberId.required",
                            "relatedMemberId is required for adult member relationships"
                    ));
                }
                Adult_MemberEntity relatedMember = memberRepository.findByIdAndTenantId(relatedMemberId, tenantId)
                        .orElseThrow(() -> new IllegalArgumentException(messageService.get(
                                "registration.familyRelationship.relatedMember.notFound",
                                "Related member not found"
                        )));
                if (owner.getId().equals(relatedMember.getId())) {
                    throw new IllegalArgumentException(messageService.get(
                            "registration.familyRelationship.selfLink.forbidden",
                            "A member cannot link themselves as a family relationship"
                    ));
                }
                entity.setRelatedMember(relatedMember);
            }
            case CHILD_MEMBER -> {
                if (relatedChildId == null) {
                    throw new IllegalArgumentException(messageService.get(
                            "registration.familyRelationship.relatedChildId.required",
                            "relatedChildId is required for child relationships"
                    ));
                }
                Child_MemberEntity relatedChild = childRepository.findByIdAndTenantId(relatedChildId, tenantId)
                        .orElseThrow(() -> new IllegalArgumentException(messageService.get(
                                "registration.familyRelationship.relatedChild.notFound",
                                "Related child not found"
                        )));
                entity.setRelatedChild(relatedChild);
            }
            case EXTERNAL -> {
                if (displayName == null || displayName.isBlank()) {
                    throw new IllegalArgumentException(messageService.get(
                            "registration.familyRelationship.displayName.required",
                            "displayName is required for external relationships"
                    ));
                }
                entity.setDisplayName(displayName.trim());
            }
        }
    }

    private void validateNoDuplicate(Adult_MemberEntity owner,
                                     UUID tenantId,
                                     FamilyRelationshipEntity relationship,
                                     Long ignoreRelationshipId) {
        if (relationship.getSourceType() == FamilyMemberSourceType.ADULT_MEMBER && relationship.getRelatedMember() != null) {
            boolean exists = familyRelationshipRepository.existsByOwnerMemberIdAndTenantIdAndRelationshipTypeAndRelatedMemberId(
                    owner.getId(),
                    tenantId,
                    relationship.getRelationshipType(),
                    relationship.getRelatedMember().getId()
            );
            if (exists && !sameRelationship(ignoreRelationshipId, relationship)) {
                throw new IllegalArgumentException(messageService.get(
                        "registration.familyRelationship.member.duplicate",
                        "This member is already linked with the same relationship type"
                ));
            }
        }

        if (relationship.getSourceType() == FamilyMemberSourceType.CHILD_MEMBER && relationship.getRelatedChild() != null) {
            boolean exists = familyRelationshipRepository.existsByOwnerMemberIdAndTenantIdAndRelationshipTypeAndRelatedChildId(
                    owner.getId(),
                    tenantId,
                    relationship.getRelationshipType(),
                    relationship.getRelatedChild().getId()
            );
            if (exists && !sameRelationship(ignoreRelationshipId, relationship)) {
                throw new IllegalArgumentException(messageService.get(
                        "registration.familyRelationship.child.duplicate",
                        "This child is already linked with the same relationship type"
                ));
            }
        }
    }

    private boolean sameRelationship(Long ignoreRelationshipId, FamilyRelationshipEntity relationship) {
        return ignoreRelationshipId != null && ignoreRelationshipId.equals(relationship.getId());
    }

    private void assertMemberSelfServiceCreateAllowed(UpsertFamilyRelationshipRequest request) {
        if (request.sourceType() != FamilyMemberSourceType.EXTERNAL) {
            throw new IllegalArgumentException(messageService.get(
                    "registration.familyRelationship.selfService.create.externalOnly",
                    "Member self-service can only create external family records"
            ));
        }
    }

    private void assertMemberSelfServiceUpdateAllowed(FamilyRelationshipEntity existing,
                                                      UpdateFamilyRelationshipRequest request) {
        if (existing.getSourceType() != FamilyMemberSourceType.EXTERNAL) {
            if (request.sourceType() != null && request.sourceType() != existing.getSourceType()) {
                throw new IllegalArgumentException(messageService.get(
                        "registration.familyRelationship.selfService.retarget.forbidden",
                        "Linked member relationships cannot be retargeted in self-service"
                ));
            }
            if (request.relatedMemberId() != null || request.relatedChildId() != null) {
                throw new IllegalArgumentException(messageService.get(
                        "registration.familyRelationship.selfService.change.forbidden",
                        "Linked member relationships cannot be changed in self-service"
                ));
            }
            if (request.displayName() != null && !request.displayName().isBlank()) {
                throw new IllegalArgumentException(messageService.get(
                        "registration.familyRelationship.selfService.rename.forbidden",
                        "Linked member relationships cannot be renamed in self-service"
                ));
            }
        }
    }

    private int defaultSortOrder(FamilyRelationshipType relationshipType) {
        return switch (relationshipType) {
            case SPOUSE -> 10;
            case CHILD -> 100;
            case PARENT -> 200;
            case IN_LAW -> 300;
            case GUARDIAN -> 120;
            case CAREGIVER -> 130;
            case SIBLING -> 140;
            case OTHER -> 400;
            case SELF -> 0;
        };
    }

    private void ensureFamilyRelationshipsSeeded(Adult_MemberEntity owner, UUID tenantId) {
        if (owner.getId() == null) {
            return;
        }
        if (familyRelationshipRepository.countByOwnerMemberIdAndTenantId(owner.getId(), tenantId) > 0) {
            return;
        }

        List<FamilyRelationshipEntity> seedRows = new ArrayList<>();
        Adult_MemberEntity spouse = resolveSpouse(owner, tenantId).orElse(null);

        if (spouse != null) {
            seedRows.add(FamilyRelationshipEntity.builder()
                    .tenantId(tenantId)
                    .ownerMember(owner)
                    .relationshipType(FamilyRelationshipType.SPOUSE)
                    .sourceType(FamilyMemberSourceType.ADULT_MEMBER)
                    .relatedMember(spouse)
                    .dependent(false)
                    .inHousehold(true)
                    .canManage(false)
                    .primaryGuardian(false)
                    .active(true)
                    .sortOrder(10)
                    .build());
        }

        List<Child_MemberEntity> children = childRepository.findFamilyChildren(tenantId, owner.getId());
        int childSort = 100;
        for (Child_MemberEntity child : children) {
            seedRows.add(FamilyRelationshipEntity.builder()
                    .tenantId(tenantId)
                    .ownerMember(owner)
                    .relationshipType(FamilyRelationshipType.CHILD)
                    .sourceType(FamilyMemberSourceType.CHILD_MEMBER)
                    .relatedChild(child)
                    .dependent(true)
                    .inHousehold(true)
                    .canManage(true)
                    .primaryGuardian(false)
                    .active(true)
                    .sortOrder(childSort++)
                    .build());
        }

        addExternalSeed(seedRows, owner, tenantId, buildExternalParentName(owner.getFatherName(), owner.getGrandFatherName()),
                FamilyRelationshipType.PARENT, 200);
        addExternalSeed(seedRows, owner, tenantId, buildExternalParentName(owner.getMotherName(), owner.getMothersFather()),
                FamilyRelationshipType.PARENT, 210);

        if (spouse != null) {
            addExternalSeed(seedRows, owner, tenantId, buildExternalParentName(spouse.getFatherName(), spouse.getGrandFatherName()),
                    FamilyRelationshipType.IN_LAW, 300);
            addExternalSeed(seedRows, owner, tenantId, buildExternalParentName(spouse.getMotherName(), spouse.getMothersFather()),
                    FamilyRelationshipType.IN_LAW, 310);
        }

        if (!seedRows.isEmpty()) {
            familyRelationshipRepository.saveAll(seedRows);
        }
    }

    private void addExternalSeed(List<FamilyRelationshipEntity> target,
                                 Adult_MemberEntity owner,
                                 UUID tenantId,
                                 String displayName,
                                 FamilyRelationshipType relationshipType,
                                 int sortOrder) {
        if (displayName == null || displayName.isBlank()) {
            return;
        }

        target.add(FamilyRelationshipEntity.builder()
                .tenantId(tenantId)
                .ownerMember(owner)
                .relationshipType(relationshipType)
                .sourceType(FamilyMemberSourceType.EXTERNAL)
                .displayName(displayName)
                .dependent(false)
                .inHousehold(false)
                .canManage(false)
                .primaryGuardian(false)
                .active(true)
                .sortOrder(sortOrder)
                .build());
    }

    private String joinNameParts(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(part.trim());
        }
        return builder.toString();
    }

    private UUID requireTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException(messageService.get(
                    "registration.member.tenantContext.missing",
                    "Tenant context is missing for member access"
            ));
        }
        return tenantId;
    }

}
