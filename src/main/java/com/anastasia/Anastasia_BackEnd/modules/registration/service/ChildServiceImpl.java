package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.common.utils.SecurityUtils;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.mappers.ChildMapper;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.ChildResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.ChildStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.ParentSummary;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChildRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
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
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class ChildServiceImpl implements ChildService{

    private final ChildRepository childRepository;
    private final ChurchRepository churchRepository;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final ChildMapper childMapper;
    private final SecurityUtils securityUtils;

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
    public ChildResponse registerChild(Child_MemberEntity childMemberEntity) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)){
            throw new IllegalStateException("User not authenticated");
        }
        UserEntity user = userRepository.findById(userPrincipal.getUserUuid())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));


        String churchNumber = normalizeChurchNumber(childMemberEntity.getChurchNumber());
        if (!StringUtils.hasText(churchNumber)) {
            throw new IllegalStateException("Church number must be provided");
        }

        childMemberEntity.setChurchNumber(churchNumber);
        ChurchEntity church = churchRepository.findByChurchNumber(churchNumber)
                .orElseThrow(() -> new IllegalStateException("Church not found for number: " + churchNumber));
        childMemberEntity.setChurch(church);
        childMemberEntity.setTenantId(church.getTenant().getId());

        childMemberEntity.setMembershipNumber(generateUniqueChildMembershipNumber(6, childMemberEntity.isDeacon()));
        childMemberEntity.setUser(user);
        childMemberEntity.setStatus(ChildStatus.PENDING.name());
        Child_MemberEntity membership = childRepository.save(childMemberEntity);

        return ChildResponse.builder()
                .name(membership.getFirstName() + " " + membership.getFatherName() + " " + membership.getGrandFatherName())
                .membershipNumber(membership.getMembershipNumber())
                .fatherOfConfession(membership.getFatherOfConfession())
                .build();
    }

    private String normalizeChurchNumber(String rawChurchNumber) {
        if (!StringUtils.hasText(rawChurchNumber)) {
            return rawChurchNumber;
        }
        return rawChurchNumber.replace("\"", "").trim();
    }

    @Override
    public Page<Child_MemberResponse> findAll(Pageable pageable) {
        return childRepository.findByStatusNotAndTenantId(
                ChildStatus.PENDING.name(),
                requireTenantId(),
                pageable)
                .map(childMapper::childEntityToResponse);
    }

    @Override
    public long countNonPending() {
        return childRepository.countByStatusNotAndTenantId(
                ChildStatus.PENDING.name(),
                requireTenantId());
    }

    @Override
    public Page<Child_MemberResponse> findByTenantAndPriestNumber(UUID tenantId, String priestNumber, Pageable pageable) {
        UUID effectiveTenantId = tenantId != null ? tenantId : requireTenantId();
        return childRepository.findByTenantIdAndPriestNumber(effectiveTenantId, priestNumber, pageable)
                .map(childMapper::childEntityToResponse);
    }

    @Override
    public Page<Child_MemberResponse> findPending(Pageable pageable) {
        return childRepository.findByStatusAndTenantId(
                ChildStatus.PENDING.name(),
                requireTenantId(),
                pageable)
                .map(childMapper::childEntityToResponse);
    }

    @Override
    public Page<Child_MemberResponse> searchNonPending(Pageable pageable, String query) {
        if (query == null || query.isBlank()) {
            return childRepository.findByStatusNotAndTenantId(
                    ChildStatus.PENDING.name(),
                    requireTenantId(),
                    pageable)
                    .map(childMapper::childEntityToResponse);
        }
        return childRepository.searchNonPending(
                query.trim(),
                ChildStatus.PENDING.name(),
                requireTenantId(),
                pageable)
                .map(childMapper::childEntityToResponse);
    }

    @Cacheable(value = "children", key = "#childId")
    @Override
    public Optional<Child_MemberResponse> findChildById(Long childId) {
        return childRepository.findByIdAndTenantId(childId, requireTenantId())
                .map(childMapper::childEntityToResponse);
    }

    @Caching(
            evict = {@CacheEvict( value = "children_all",
                        keyGenerator = "tenantAwareKeyGenerator",  allEntries = true),
                    @CacheEvict(value = "children",
                            key = "#childId")}
    )
    @Override
    public void updateChildDetails(Long childId, Child_MemberDTO request) {
        childRepository.findByIdAndTenantId(childId, requireTenantId()).ifPresent(memberEntity -> {

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
            Optional.ofNullable(request.getPriestNumber()).ifPresent(memberEntity::setPriestNumber);

            Optional.ofNullable(request.getAddress()).ifPresent(memberEntity::setAddress);
            if (request.getFather() != null) {
                linkParent(memberEntity, request.getFather(), true);
            }
            if (request.getMother() != null) {
                linkParent(memberEntity, request.getMother(), false);
            }

            childRepository.save(memberEntity);
        });
    }

    @Caching(
            evict = {
                    @CacheEvict(value = "children",
                            key ="#childId"
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
    public Page<Child_MemberResponse> findAllBySpecification(Specification<Child_MemberEntity> spec, Pageable pageable) {
        Specification<Child_MemberEntity> tenantSpec = (root, query, cb) ->
                cb.equal(root.get("tenantId"), requireTenantId());
        Specification<Child_MemberEntity> combinedSpec = Specification.allOf(tenantSpec).and(spec);
        return childRepository.findAll(combinedSpec, pageable)
                .map(childMapper::childEntityToResponse);
    }

    @Caching(
            put = {@CachePut(value = "children", key = "#childId")},
            evict = {@CacheEvict(value = "children_all",
                    keyGenerator = "tenantAwareKeyGenerator",
                    allEntries = true)}
    )
    @Override
    public Child_MemberResponse approveByChurch(Long childId) {
        Child_MemberEntity child = childRepository.findByIdAndTenantId(childId, requireTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Child not found"));
        child.setStatus(ChildStatus.APPROVED.name());
        Child_MemberEntity saved = childRepository.save(child);
        return convertToResponse(saved);
    }

    @Caching(
            put = {@CachePut(value = "children", key = "#childId")},
            evict = {@CacheEvict(value = "children_all",
                    keyGenerator = "tenantAwareKeyGenerator",
                    allEntries = true)}
    )
    @Override
    public Child_MemberResponse approveByPriest(Long childId) {
        Child_MemberEntity child = childRepository.findByIdAndTenantId(childId, requireTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Child not found"));
        child.setStatus(ChildStatus.APPROVED.name());
        Child_MemberEntity saved = childRepository.save(child);
        return convertToResponse(saved);
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

        Adult_MemberEntity parent = memberRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Parent not found for id: " + parentId));

        Adult_MemberEntity current = isFather ? child.getFather() : child.getMother();
        if (current != null && current.getId().equals(parentId)) {
            return;
        }

        detachParent(child, isFather);

        Consumer<Child_MemberEntity> adder = isFather
                ? parent.getChildrenAsFather()::add
                : parent.getChildrenAsMother()::add;

        if (isFather) {
            child.setFather(parent);
        } else {
            child.setMother(parent);
        }
        adder.accept(child);
    }

    private void detachParent(Child_MemberEntity child, boolean isFather) {
        Adult_MemberEntity existing = isFather ? child.getFather() : child.getMother();
        if (existing != null) {
            Consumer<Child_MemberEntity> remover = isFather
                    ? existing.getChildrenAsFather()::remove
                    : existing.getChildrenAsMother()::remove;
            remover.accept(child);
        }
        if (isFather) {
            child.setFather(null);
        } else {
            child.setMother(null);
        }
    }

    private UUID requireTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is missing for child member access");
        }
        return tenantId;
    }

}
