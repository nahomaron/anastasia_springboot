package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.common.utils.SecurityUtils;
import com.anastasia.Anastasia_BackEnd.core.notification.service.TenantAdminNotificationService;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.mappers.ChildMapper;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.MemberLifecycleStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberSummaryResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.ChildStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.ParentSummary;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.ApprovalStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.MembershipStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantAdminAssignmentEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChildRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.PriestRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantAdminAssignmentRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.ActiveMemberLimitPolicy;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChildServiceImpl implements ChildService{

    private final ChildRepository childRepository;
    private final ChurchRepository churchRepository;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final PriestRepository priestRepository;
    private final TenantAdminAssignmentRepository tenantAdminAssignmentRepository;
    private final ChildMapper childMapper;
    private final SecurityUtils securityUtils;
    private final TenantAdminNotificationService tenantAdminNotificationService;
    private final ActiveMemberLimitPolicy activeMemberLimitPolicy;
    private final LocalizedMessageService messageService;

    @Override
    public Child_MemberEntity convertToEntity(Child_MemberDTO childMemberDTO) {
        Child_MemberEntity entity = childMapper.childDTOToEntity(childMemberDTO);
        if (entity == null || childMemberDTO == null) {
            return entity;
        }
        linkParent(entity, childMemberDTO.getFather(), true);
        linkParent(entity, childMemberDTO.getMother(), false);
        return entity;
    }

    @Override
    public Child_MemberDTO convertToDTO(Child_MemberEntity childMemberEntity) {
        return childMapper.childEntityToDTO(childMemberEntity);
    }

    @Override
    public Child_MemberResponse convertToResponse(Child_MemberEntity childMemberEntity) {
        return childMapper.childEntityToResponse(childMemberEntity);
    }

    @Caching(
            evict = {@CacheEvict(value = "children_all",
                    keyGenerator = "tenantAwareKeyGenerator",
                    allEntries = true)
            }
    )
    @Override
    public Child_MemberResponse registerChild(Child_MemberEntity childMemberEntity) {
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


        String churchNumber = normalizeChurchNumber(childMemberEntity.getChurchNumber());
        if (!StringUtils.hasText(churchNumber)) {
            throw new IllegalStateException(messageService.get(
                    "registration.churchNumber.required",
                    "Church number must be provided"
            ));
        }

        childMemberEntity.setChurchNumber(churchNumber);
        ChurchEntity church = resolveChurch(churchNumber);
        childMemberEntity.setChurch(church);
        childMemberEntity.setTenantId(church.getTenant().getId());

        childMemberEntity.setMembershipNumber(generateUniqueChildMembershipNumber(6, childMemberEntity.isDeacon()));
        childMemberEntity.setUser(user);
        childMemberEntity.setStatus(ChildStatus.PENDING.name());
        Child_MemberEntity membership = childRepository.save(childMemberEntity);
        tenantAdminNotificationService.notifyChildRegistrationSubmitted(membership, user.getUuid());

        return convertToResponse(membership);
    }

    @Caching(
            evict = {@CacheEvict(value = "children_all",
                    keyGenerator = "tenantAwareKeyGenerator",
                    allEntries = true)
            }
    )
    @Override
    public Child_MemberResponse registerChildAsAdmin(Child_MemberEntity childMemberEntity) {
        UUID tenantId = requireTenantId();
        requireActiveTenantAdmin(tenantId);

        ChurchEntity church = resolveAdminChurch(childMemberEntity.getChurchNumber(), tenantId);
        PriestEntity assignedPriest = resolveAdminPriestForChurch(childMemberEntity.getPriestNumber(), church);
        boolean requiresPriestApproval = assignedPriest != null;
        if (!requiresPriestApproval) {
            activeMemberLimitPolicy.assertCanActivateMembers(tenantId, 1);
        }

        childMemberEntity.setChurchNumber(church.getChurchNumber());
        childMemberEntity.setChurch(church);
        childMemberEntity.setTenantId(tenantId);
        childMemberEntity.setMembershipNumber(generateUniqueChildMembershipNumber(6, childMemberEntity.isDeacon()));
        childMemberEntity.setUser(null);
        childMemberEntity.setPriestNumber(requiresPriestApproval ? assignedPriest.getPriestNumber() : null);
        childMemberEntity.setApprovedByChurch(true);
        childMemberEntity.setChurchApprovalStatus(ApprovalStatus.APPROVED);
        childMemberEntity.setStatus(requiresPriestApproval ? ChildStatus.PENDING.name() : ChildStatus.ACTIVE.name());

        Child_MemberEntity membership = childRepository.save(childMemberEntity);
        return convertToResponse(membership);
    }

    private String normalizeChurchNumber(String rawChurchNumber) {
        if (!StringUtils.hasText(rawChurchNumber)) {
            return rawChurchNumber;
        }
        return rawChurchNumber.replace("\"", "").trim();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Child_MemberResponse> findAll(Pageable pageable) {
        return childRepository.findByStatusValueNotAndTenantId(
                MemberLifecycleStatus.PENDING,
                requireTenantId(),
                pageable)
                .map(childMapper::childEntityToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Child_MemberSummaryResponse> findAllSummary(Pageable pageable, String language) {
        return childRepository.findByStatusValueNotAndTenantId(
                MemberLifecycleStatus.PENDING,
                requireTenantId(),
                pageable)
                .map(child -> childMapper.childEntityToSummaryResponse(child, language));
    }

    @Override
    public long countNonPending() {
        return childRepository.countByStatusValueNotAndTenantId(
                MemberLifecycleStatus.PENDING,
                requireTenantId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Child_MemberResponse> findByTenantAndPriestNumber(UUID tenantId, String priestNumber, Pageable pageable) {
        UUID effectiveTenantId = tenantId != null ? tenantId : requireTenantId();
        return childRepository.findByTenantIdAndPriestNumber(effectiveTenantId, priestNumber, pageable)
                .map(childMapper::childEntityToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Child_MemberSummaryResponse> findByTenantAndPriestNumberSummary(UUID tenantId, String priestNumber, Pageable pageable, String language) {
        UUID effectiveTenantId = tenantId != null ? tenantId : requireTenantId();
        return childRepository.findByTenantIdAndPriestNumber(effectiveTenantId, priestNumber, pageable)
                .map(child -> childMapper.childEntityToSummaryResponse(child, language));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Child_MemberResponse> findPending(Pageable pageable) {
        return childRepository.findByStatusValueAndTenantId(
                MemberLifecycleStatus.PENDING,
                requireTenantId(),
                pageable)
                .map(childMapper::childEntityToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Child_MemberResponse> searchNonPending(Pageable pageable, String query) {
        if (query == null || query.isBlank()) {
            return childRepository.findByStatusValueNotAndTenantId(
                    MemberLifecycleStatus.PENDING,
                    requireTenantId(),
                    pageable)
                    .map(childMapper::childEntityToResponse);
        }
        return childRepository.searchNonPending(
                query.trim(),
                MemberLifecycleStatus.PENDING,
                requireTenantId(),
                pageable)
                .map(childMapper::childEntityToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Child_MemberSummaryResponse> searchNonPendingSummary(Pageable pageable, String query, String language) {
        if (query == null || query.isBlank()) {
            return childRepository.findByStatusValueNotAndTenantId(
                    MemberLifecycleStatus.PENDING,
                    requireTenantId(),
                    pageable)
                    .map(child -> childMapper.childEntityToSummaryResponse(child, language));
        }
        return childRepository.searchNonPending(
                query.trim(),
                MemberLifecycleStatus.PENDING,
                requireTenantId(),
                pageable)
                .map(child -> childMapper.childEntityToSummaryResponse(child, language));
    }

    @Cacheable(value = "children", keyGenerator = "tenantAwareKeyGenerator")
    @Override
    @Transactional(readOnly = true)
    public Optional<Child_MemberResponse> findChildById(Long childId) {
        return childRepository.findByIdAndTenantId(childId, requireTenantId())
                .map(childMapper::childEntityToResponse);
    }

    @Caching(
            evict = {@CacheEvict( value = "children_all",
                        keyGenerator = "tenantAwareKeyGenerator",  allEntries = true),
                    @CacheEvict(value = "children",
                            key = "#root.target.childCacheKey(#childId)")}
    )
    @Override
    public Child_MemberResponse updateChildDetails(Long childId, Child_MemberDTO request) {
        Child_MemberEntity memberEntity = childRepository.findByIdAndTenantId(childId, requireTenantId())
                .orElseThrow(() -> new IllegalArgumentException(messageService.get(
                        "registration.child.notFound",
                        "Child not found"
                )));

            Optional.ofNullable(request.getChurchNumber()).ifPresent(churchNumber -> {
                ChurchEntity church = resolveChurch(normalizeChurchNumber(churchNumber));
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
            Optional.ofNullable(request.getFirstNameLocal()).ifPresent(memberEntity::setFirstNameLocal);
            Optional.ofNullable(request.getFatherNameLocal()).ifPresent(memberEntity::setFatherNameLocal);
            Optional.ofNullable(request.getGrandFatherNameLocal()).ifPresent(memberEntity::setGrandFatherNameLocal);
            Optional.ofNullable(request.getMotherFullNameLocal()).ifPresent(memberEntity::setMotherFullNameLocal);
            Optional.ofNullable(request.getGender()).ifPresent(memberEntity::setGender);
            Optional.ofNullable(request.getBirthday()).ifPresent(memberEntity::setBirthday);
            Optional.ofNullable(request.getNationality()).ifPresent(memberEntity::setNationality);
            Optional.ofNullable(request.getPlaceOfBirth()).ifPresent(memberEntity::setPlaceOfBirth);
            Optional.ofNullable(request.getEmail()).ifPresent(memberEntity::setEmail);
            Optional.ofNullable(request.getPhone()).ifPresent(memberEntity::setPhone);
            Optional.ofNullable(request.getWhatsApp()).ifPresent(memberEntity::setWhatsApp);
            Optional.ofNullable(request.getEmergencyContactNumber()).ifPresent(memberEntity::setEmergencyContactNumber);
            Optional.ofNullable(request.getContactRelation()).ifPresent(memberEntity::setContactRelation);
            Optional.ofNullable(request.getPrimaryGuardianPhone()).ifPresent(memberEntity::setPrimaryGuardianPhone);
            Optional.ofNullable(request.getGuardianRelationship()).ifPresent(memberEntity::setGuardianRelationship);

            Optional.ofNullable(request.getFirstLanguage()).ifPresent(memberEntity::setFirstLanguage);
            Optional.ofNullable(request.getSecondLanguage()).ifPresent(memberEntity::setSecondLanguage);
            Optional.ofNullable(request.getLevelOfEducation()).ifPresent(memberEntity::setLevelOfEducation);
            Optional.ofNullable(request.getFatherOfConfession()).ifPresent(memberEntity::setFatherOfConfession);
            Optional.ofNullable(request.getPriestNumber()).ifPresent(memberEntity::setPriestNumber);

            Optional.ofNullable(request.getAddress()).ifPresent(memberEntity::setAddress);
            if (request.getFather() != null) {
                linkParent(memberEntity, request.getFather(), true);
            }
            if (request.getMother() != null) {
                linkParent(memberEntity, request.getMother(), false);
            }

            Child_MemberEntity saved = childRepository.save(memberEntity);
            return convertToResponse(saved);
    }

    @Caching(
            evict = {
                    @CacheEvict(value = "children",
                            key = "#root.target.childCacheKey(#childId)"
                    ),
                    @CacheEvict(value = "children_all",
                            keyGenerator = "tenantAwareKeyGenerator",
                            allEntries = true
                    )
            }
    )
    @Override
    public void deleteChildMembership(Long childId) {
        childRepository.findByIdAndTenantId(childId, requireTenantId())
                .ifPresent(childRepository::delete);

    }

    @Override
    @Transactional(readOnly = true)
    public Page<Child_MemberResponse> findAllBySpecification(Specification<Child_MemberEntity> spec, Pageable pageable) {
        Specification<Child_MemberEntity> tenantSpec = (root, query, cb) ->
                cb.equal(root.get("tenantId"), requireTenantId());
        Specification<Child_MemberEntity> combinedSpec = Specification.allOf(tenantSpec).and(spec);
        return childRepository.findAll(combinedSpec, pageable)
                .map(childMapper::childEntityToResponse);
    }

    @Caching(
            put = {@CachePut(value = "children", keyGenerator = "tenantAwareKeyGenerator")},
            evict = {@CacheEvict(value = "children_all",
                    keyGenerator = "tenantAwareKeyGenerator",
                    allEntries = true)}
    )
    @Override
    public Child_MemberResponse approveByChurch(Long childId) {
        UUID tenantId = requireTenantId();
        Child_MemberEntity child = childRepository.findByIdAndTenantId(childId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(messageService.get(
                        "registration.child.notFound",
                        "Child not found"
                )));
        if (!ChildStatus.APPROVED.name().equals(child.getStatus()) && !ChildStatus.ACTIVE.name().equals(child.getStatus())) {
            activeMemberLimitPolicy.assertCanActivateMembers(tenantId, 1);
        }
        child.setChurchApprovalStatus(ApprovalStatus.APPROVED);
        child.setApprovedByChurch(true);
        child.setStatus(ChildStatus.APPROVED.name());
        Child_MemberEntity saved = childRepository.save(child);
        return convertToResponse(saved);
    }

    @Caching(
            put = {@CachePut(value = "children", keyGenerator = "tenantAwareKeyGenerator")},
            evict = {@CacheEvict(value = "children_all",
                    keyGenerator = "tenantAwareKeyGenerator",
                    allEntries = true)}
    )
    @Override
    public Child_MemberResponse approveByPriest(Long childId) {
        UUID tenantId = requireTenantId();
        Child_MemberEntity child = childRepository.findByIdAndTenantId(childId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(messageService.get(
                        "registration.child.notFound",
                        "Child not found"
                )));
        if (!ChildStatus.APPROVED.name().equals(child.getStatus()) && !ChildStatus.ACTIVE.name().equals(child.getStatus())) {
            activeMemberLimitPolicy.assertCanActivateMembers(tenantId, 1);
        }
        child.setChurchApprovalStatus(ApprovalStatus.APPROVED);
        child.setApprovedByChurch(true);
        child.setStatus(ChildStatus.APPROVED.name());
        Child_MemberEntity saved = childRepository.save(child);
        return convertToResponse(saved);
    }

    public String childCacheKey(Long childId) {
        return "tenant:" + requireTenantId() + ":" + childId;
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

    private void linkParent(Child_MemberEntity child,
                            ParentSummary parentSummary,
                            boolean isFather) {
        if (parentSummary == null) {
            detachParent(child, isFather);
            return;
        }

        Long parentId = parentSummary.getId();
        if (parentId == null) {
            detachParent(child, isFather);
            return;
        }

        Adult_MemberEntity parent = memberRepository.findByIdAndTenantId(parentId, requireTenantId())
                .orElseThrow(() -> new IllegalArgumentException(messageService.get(
                        "registration.parent.notFound.withId",
                        "Parent not found for id: {0}",
                        parentId
                )));

        Adult_MemberEntity current = isFather ? child.getFather() : child.getMother();
        if (current != null && current.getId().equals(parentId)) {
            return;
        }

        detachParent(child, isFather);

        if (isFather) {
            child.setFather(parent);
        } else {
            child.setMother(parent);
        }
    }

    private void detachParent(Child_MemberEntity child, boolean isFather) {
        Adult_MemberEntity existing = isFather ? child.getFather() : child.getMother();
        if (isFather) {
            child.setFather(null);
        } else {
            child.setMother(null);
        }
    }

    private UUID requireTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException(messageService.get(
                    "registration.child.tenantContext.missing",
                    "Tenant context is missing for child member access"
            ));
        }
        return tenantId;
    }

    private UserEntity requireActiveTenantAdmin(UUID tenantId) {
        UserEntity user = requireCurrentUserEntity();
        TenantAdminAssignmentEntity assignment = tenantAdminAssignmentRepository
                .findByTenant_IdAndUserId(tenantId, user.getUuid())
                .orElseThrow(() -> new IllegalStateException(messageService.get(
                        "registration.admin.assignment.missing",
                        "Active tenant admin assignment is required"
                )));
        if (assignment.getStatus() != MembershipStatus.ACTIVE) {
            throw new IllegalStateException(messageService.get(
                    "registration.admin.assignment.inactive",
                    "Active tenant admin assignment is required"
            ));
        }
        return user;
    }

    private UserEntity requireCurrentUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)){
            throw new IllegalStateException(messageService.get(
                    "auth.user.notAuthenticated",
                    "User not authenticated"
            ));
        }
        return userRepository.findById(userPrincipal.getUserUuid())
                .orElseThrow(() -> new IllegalStateException(messageService.get(
                        "auth.user.notAuthenticated",
                        "Authenticated user not found"
                )));
    }

    private ChurchEntity resolveAdminChurch(String requestedChurchNumber, UUID tenantId) {
        ChurchEntity church = churchRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalStateException(messageService.get(
                        "registration.admin.churchContext.missing",
                        "Current tenant does not have a church context"
                )));
        if (!StringUtils.hasText(requestedChurchNumber)
                || !church.getChurchNumber().equalsIgnoreCase(requestedChurchNumber.trim())) {
            throw new IllegalStateException(messageService.get(
                    "registration.admin.churchContext.invalid",
                    "Registration church must match the current tenant church"
            ));
        }
        return church;
    }

    private PriestEntity resolveAdminPriestForChurch(String priestNumber, ChurchEntity church) {
        if (!StringUtils.hasText(priestNumber) || church == null || church.getChurchId() == null) {
            return null;
        }
        return priestRepository.findByPriestNumber(priestNumber.trim())
                .filter(priest -> priest.getStatus() == PriestStatus.ACTIVE)
                .filter(priest -> priest.getChurch() != null)
                .filter(priest -> church.getChurchId().equals(priest.getChurch().getChurchId()))
                .orElse(null);
    }

    private ChurchEntity resolveChurch(String churchNumber) {
        return churchRepository.findByChurchNumber(churchNumber)
                .orElseThrow(() -> new IllegalStateException(messageService.get(
                        "church.notFound.withNumber",
                        "Church not found for number: {0}",
                        churchNumber
                )));
    }

}
