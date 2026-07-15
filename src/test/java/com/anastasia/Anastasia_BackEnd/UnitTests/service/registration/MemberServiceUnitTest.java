package com.anastasia.Anastasia_BackEnd.UnitTests.service.registration;

import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.outbox.OutboxPublisher;
import com.anastasia.Anastasia_BackEnd.modules.registration.mappers.MemberMapper;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.MemberLifecycleStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.MemberStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.family.UpdateFamilyRelationshipRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.family.FamilyMemberSourceType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.family.FamilyRelationshipEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestStatus;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.MembershipStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantAdminAssignmentEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserType;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.PriestRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.FamilyRelationshipRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantAdminAssignmentRepository;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.core.notification.service.TenantAdminNotificationService;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.family.FamilyRelationshipType;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.card.MembershipCardService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.ActiveMemberLimitPolicy;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.MemberServiceImpl;
import com.anastasia.Anastasia_BackEnd.common.utils.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@LenientMockitoTest
public class MemberServiceUnitTest {

    @Mock private FamilyRelationshipRepository familyRelationshipRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private ChurchRepository churchRepository;
    @Mock private UserRepository userRepository;
    @Mock private PriestRepository priestRepository;
    @Mock private TenantAdminAssignmentRepository tenantAdminAssignmentRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private MemberMapper memberMapper;
    @Mock private SecurityUtils securityUtils;
    @Mock private SecurityContext securityContext;
    @Mock private ApplicationEventPublisher publisher;
    @Mock private CacheManager cacheManager;
    @Mock private OutboxPublisher outboxPublisher;
    @Mock private TenantAdminNotificationService tenantAdminNotificationService;
    @Mock private LocalizedMessageService messageService;
    @Mock private ActiveMemberLimitPolicy activeMemberLimitPolicy;
    @Mock private MembershipCardService membershipCardService;

    @InjectMocks
    private MemberServiceImpl memberService;

    private UserEntity user;
    private Adult_MemberEntity member;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
        user = TestDataUtil.createTestUserEntityA();
        member = TestDataUtil.createTestMember(TestDataUtil.createTestChurchEntity(TestDataUtil.createTestTenantEntity()));
        lenient().when(familyRelationshipRepository.findChildRelationshipsByOwnerIdsAndTenantIdAndRelationshipType(
                anySet(),
                any(UUID.class),
                eq(FamilyRelationshipType.CHILD)))
                .thenReturn(List.of());
        lenient().when(memberMapper.memberEntityToResponse(any(Adult_MemberEntity.class)))
                .thenAnswer(invocation -> {
                    Adult_MemberEntity entity = invocation.getArgument(0);
                    Adult_MemberResponse response = new Adult_MemberResponse();
                    response.setFirstName(entity.getFirstName());
                    response.setMembershipNumber(entity.getMembershipNumber());
                    response.setChildrenAsFatherIds(Collections.emptySet());
                    response.setChildrenAsMotherIds(Collections.emptySet());
                    return response;
                });
        lenient().when(memberRepository.save(any(Adult_MemberEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(messageService.get(anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void registerMember_shouldRegisterSuccessfully() {

        UserEntity user = TestDataUtil.createTestUserEntityA();
        user.setUuid(UUID.randomUUID());
        user.setUserType(UserType.GUEST);

        ChurchEntity church = TestDataUtil.createTestChurchEntity(TestDataUtil.createTestTenantEntity());
        Adult_MemberEntity member = TestDataUtil.createTestMember(church);

        UserPrincipal principal = new UserPrincipal(user);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);

        when(securityContext.getAuthentication()).thenReturn(auth);

        when(userRepository.findById(user.getUuid())).thenReturn(Optional.of(user));
        when(churchRepository.findByChurchNumber(member.getChurchNumber())).thenReturn(Optional.of(church));
        when(securityUtils.generateUniqueIDNumber(anyInt(), anyString())).thenReturn("M123456");
        when(memberRepository.existsByMembershipNumber(anyString())).thenReturn(false);
        when(memberRepository.save(any(Adult_MemberEntity.class))).thenAnswer(i -> {
            Adult_MemberEntity saved = i.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(userRepository.save(any(UserEntity.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Adult_MemberResponse response = memberService.registerMember(member);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getMembershipNumber()).isEqualTo("M123456");
        assertThat(response.getFirstName()).isEqualTo(member.getFirstName());


        verify(userRepository, times(1)).findById(user.getUuid());
        verify(churchRepository, times(1)).findByChurchNumber(member.getChurchNumber());
        verify(securityUtils, times(1)).generateUniqueIDNumber(anyInt(), anyString());
        verify(memberRepository, times(1)).existsByMembershipNumber(anyString());
        verify(memberRepository, times(1)).save(any(Adult_MemberEntity.class));
        verify(userRepository, times(1)).save(user); // Verify saving the updated user
        verify(securityContext, times(1)).getAuthentication(); // Verify authentication was retrieved
        verify(auth, times(1)).getPrincipal(); // Verify principal was retrieved
    }

    @Test
    void testConvertToEntity() {
        Adult_MemberDTO dto = new Adult_MemberDTO();
        when(memberMapper.memberDTOToEntity(dto)).thenReturn(member);
        Adult_MemberEntity result = memberService.convertToEntity(dto);
        assertThat(result).isEqualTo(member);
    }

    @Test
    void testConvertToDTO() {
        Adult_MemberDTO dto = new Adult_MemberDTO();
        when(memberMapper.memberEntityToDTO(member)).thenReturn(dto);
        Adult_MemberDTO result = memberService.convertToDTO(member);
        assertThat(result).isEqualTo(dto);
    }

    @Test
    void testFindAll() {
        Adult_MemberResponse response = new Adult_MemberResponse();
        Page<Adult_MemberEntity> page = new PageImpl<>(List.of(member));
        when(memberRepository.findByStatusValueNotAndTenantId(
                eq(MemberLifecycleStatus.PENDING),
                eq(tenantId),
                any(PageRequest.class)))
                .thenReturn(page);
        when(memberMapper.memberEntityToResponse(member)).thenReturn(response);
        Page<Adult_MemberResponse> result = memberService.findAll(PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(response);
    }

    @Test
    void testFindMemberById() {
        Adult_MemberResponse response = new Adult_MemberResponse();
        when(memberRepository.findByIdAndTenantId(1L, tenantId)).thenReturn(Optional.of(member));
        when(memberMapper.memberEntityToResponse(member)).thenReturn(response);
        Optional<Adult_MemberResponse> result = memberService.findMemberById(1L);
        assertThat(result).isPresent().contains(response);
    }

    @Test
    void testUpdateMembershipDetails() {
        Adult_MemberDTO request = new Adult_MemberDTO();
        request.setFirstName("Updated");
        when(memberRepository.findByIdAndTenantId(anyLong(), eq(tenantId))).thenReturn(Optional.of(member));

        Adult_MemberResponse response = memberService.updateMembershipDetails(1L, request);

        verify(memberRepository).save(argThat(updated -> "Updated".equals(updated.getFirstName())));
        assertThat(response.getFirstName()).isEqualTo("Updated");
    }

    @Test
    void testDeleteMembership() {
        when(memberRepository.findByIdAndTenantId(5L, tenantId)).thenReturn(Optional.of(member));
        memberService.deleteMembership(5L);
        verify(memberRepository).delete(member);
    }

    @Test
    void testApproveByChurch() {
        member.setApprovedByChurch(true);
        member.setApprovedByPriest(true);
        when(memberRepository.findByIdAndTenantId(1L, tenantId)).thenReturn(Optional.of(member));
        memberService.approveByChurch(1L);
        verify(memberRepository).save(member);
        assertThat(member.getStatus()).isEqualTo(MemberStatus.APPROVED.name());
    }

    @Test
    void testApproveByPriest() {
        member.setApprovedByChurch(true);
        member.setApprovedByPriest(false);
        member.setPriestNumber("K12345");
        PriestEntity priest = PriestEntity.builder()
                .priestNumber("K12345")
                .spiritualChildren(2)
                .build();
        when(memberRepository.findByIdAndTenantId(1L, tenantId)).thenReturn(Optional.of(member));
        when(priestRepository.findByPriestNumber("K12345")).thenReturn(Optional.of(priest));
        memberService.approveByPriest(1L);
        verify(memberRepository).save(member);
        verify(priestRepository).save(priest);
        assertThat(priest.getSpiritualChildren()).isEqualTo(3);
        assertThat(member.getStatus()).isEqualTo(MemberStatus.APPROVED.name());
    }

    @Test
    void testFindAllBySpecification() {
        Page<Adult_MemberEntity> page = new PageImpl<>(List.of(member));
        Adult_MemberResponse response = new Adult_MemberResponse();
        when(memberRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
        when(memberMapper.memberEntityToResponse(member)).thenReturn(response);
        Page<Adult_MemberResponse> result = memberService.findAllBySpecification(mock(Specification.class), PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void registerMemberAsAdmin_activatesDirectlyWhenPriestIsInvalid() {
        user.setUuid(UUID.randomUUID());
        user.setUserType(UserType.GUEST);

        ChurchEntity church = TestDataUtil.createTestChurchEntity(TestDataUtil.createTestTenantEntity());
        church.setChurchId(101L);
        church.getTenant().setId(tenantId);
        member.setChurchNumber(church.getChurchNumber());
        member.setPriestNumber("PRIEST-404");

        UserPrincipal principal = new UserPrincipal(user);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(userRepository.findById(user.getUuid())).thenReturn(Optional.of(user));
        when(tenantAdminAssignmentRepository.findByTenant_IdAndUserId(tenantId, user.getUuid()))
                .thenReturn(Optional.of(TenantAdminAssignmentEntity.builder()
                        .tenant(church.getTenant())
                        .userId(user.getUuid())
                        .status(MembershipStatus.ACTIVE)
                        .build()));
        when(churchRepository.findByTenantId(tenantId)).thenReturn(Optional.of(church));
        when(securityUtils.generateUniqueIDNumber(anyInt(), anyString())).thenReturn("M654321");
        when(memberRepository.existsByMembershipNumber(anyString())).thenReturn(false);
        when(memberRepository.save(any(Adult_MemberEntity.class))).thenAnswer(i -> {
            Adult_MemberEntity saved = i.getArgument(0);
            saved.setId(9L);
            return saved;
        });

        Adult_MemberResponse response = memberService.registerMemberAsAdmin(member);

        assertThat(response).isNotNull();
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE.name());
        assertThat(member.isApprovedByChurch()).isTrue();
        assertThat(member.getPriestNumber()).isNull();
        verify(activeMemberLimitPolicy).assertCanActivateMembers(tenantId, 1);
        verify(membershipCardService).issueOrRefreshForApprovedMember(member);
        verify(userRepository, never()).save(user);
    }

    @Test
    void registerMemberAsAdmin_keepsPendingWhenPriestBelongsToChurch() {
        user.setUuid(UUID.randomUUID());
        ChurchEntity church = TestDataUtil.createTestChurchEntity(TestDataUtil.createTestTenantEntity());
        church.setChurchId(102L);
        church.getTenant().setId(tenantId);
        member.setChurchNumber(church.getChurchNumber());
        member.setPriestNumber("PR12345");

        PriestEntity priest = PriestEntity.builder()
                .priestNumber("PR12345")
                .status(PriestStatus.ACTIVE)
                .church(church)
                .build();

        UserPrincipal principal = new UserPrincipal(user);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(userRepository.findById(user.getUuid())).thenReturn(Optional.of(user));
        when(tenantAdminAssignmentRepository.findByTenant_IdAndUserId(tenantId, user.getUuid()))
                .thenReturn(Optional.of(TenantAdminAssignmentEntity.builder()
                        .tenant(church.getTenant())
                        .userId(user.getUuid())
                        .status(MembershipStatus.ACTIVE)
                        .build()));
        when(churchRepository.findByTenantId(tenantId)).thenReturn(Optional.of(church));
        when(priestRepository.findByPriestNumber("PR12345")).thenReturn(Optional.of(priest));
        when(securityUtils.generateUniqueIDNumber(anyInt(), anyString())).thenReturn("M111111");
        when(memberRepository.existsByMembershipNumber(anyString())).thenReturn(false);

        memberService.registerMemberAsAdmin(member);

        assertThat(member.getStatus()).isEqualTo(MemberStatus.PENDING.name());
        assertThat(member.isApprovedByChurch()).isTrue();
        assertThat(member.isApprovedByPriest()).isFalse();
        assertThat(member.getPriestNumber()).isEqualTo("PR12345");
        verify(activeMemberLimitPolicy, never()).assertCanActivateMembers(any(), anyInt());
        verify(membershipCardService, never()).issueOrRefreshForApprovedMember(any());
    }

    @Test
    void updateFamilyRelationship_shouldUseCurrentMemberAndTenantScope() {
        UUID userId = UUID.randomUUID();
        Adult_MemberEntity owner = Adult_MemberEntity.builder().id(101L).build();
        authenticate(userId);
        when(memberRepository.findByUserIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(owner));
        when(familyRelationshipRepository.countByOwnerMemberIdAndTenantId(owner.getId(), tenantId)).thenReturn(1L);
        FamilyRelationshipEntity relationship = FamilyRelationshipEntity.builder()
                .id(55L)
                .tenantId(tenantId)
                .ownerMember(owner)
                .relationshipType(FamilyRelationshipType.PARENT)
                .sourceType(FamilyMemberSourceType.EXTERNAL)
                .displayName("Parent")
                .active(true)
                .build();
        when(familyRelationshipRepository.findByIdAndOwnerMemberIdAndTenantId(55L, owner.getId(), tenantId))
                .thenReturn(Optional.of(relationship));
        when(familyRelationshipRepository.save(any(FamilyRelationshipEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        memberService.updateFamilyRelationship(55L, new UpdateFamilyRelationshipRequest(
                null,
                null,
                null,
                null,
                "Updated Parent",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ));

        verify(familyRelationshipRepository).findByIdAndOwnerMemberIdAndTenantId(55L, owner.getId(), tenantId);
        verify(familyRelationshipRepository).save(any(FamilyRelationshipEntity.class));
    }

    @Test
    void deleteFamilyRelationship_shouldRejectRelationshipNotOwnedByCurrentMember() {
        UUID userId = UUID.randomUUID();
        Adult_MemberEntity owner = Adult_MemberEntity.builder().id(101L).build();
        authenticate(userId);
        when(memberRepository.findByUserIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(owner));
        when(familyRelationshipRepository.findByIdAndOwnerMemberIdAndTenantId(55L, owner.getId(), tenantId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.deleteFamilyRelationship(55L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Family relationship not found");

        verify(familyRelationshipRepository).findByIdAndOwnerMemberIdAndTenantId(55L, owner.getId(), tenantId);
        verify(familyRelationshipRepository, never()).delete(any());
    }

    @Test
    void mappedReadMethods_areTransactionalReadOnly() throws NoSuchMethodException {
        assertTransactionalReadOnly("findAll", org.springframework.data.domain.Pageable.class);
        assertTransactionalReadOnly("findAllSummary", org.springframework.data.domain.Pageable.class, String.class);
        assertTransactionalReadOnly("findByTenantAndPriestNumber", UUID.class, String.class, org.springframework.data.domain.Pageable.class);
        assertTransactionalReadOnly("findByTenantAndPriestNumberSummary", UUID.class, String.class, org.springframework.data.domain.Pageable.class, String.class);
        assertTransactionalReadOnly("findByTenantAndPriestNumberAndStatus", UUID.class, String.class, String.class, org.springframework.data.domain.Pageable.class);
        assertTransactionalReadOnly("findByTenantAndPriestNumberAndStatusSummary", UUID.class, String.class, String.class, org.springframework.data.domain.Pageable.class, String.class);
        assertTransactionalReadOnly("findPending", org.springframework.data.domain.Pageable.class);
        assertTransactionalReadOnly("findPendingByTenantAndPriestNumber", UUID.class, String.class, org.springframework.data.domain.Pageable.class);
        assertTransactionalReadOnly("searchNonPending", org.springframework.data.domain.Pageable.class, String.class);
        assertTransactionalReadOnly("searchNonPendingSummary", org.springframework.data.domain.Pageable.class, String.class, String.class);
        assertTransactionalReadOnly("findMemberById", Long.class);
        assertTransactionalReadOnly("findAllBySpecification", Specification.class, org.springframework.data.domain.Pageable.class);
    }

    private void assertTransactionalReadOnly(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = MemberServiceImpl.class.getMethod(methodName, parameterTypes);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isTrue();
    }

    private void authenticate(UUID userId) {
        UserEntity authenticatedUser = UserEntity.builder()
                .uuid(userId)
                .email("member@example.com")
                .build();
        UserPrincipal principal = new UserPrincipal(authenticatedUser);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        when(securityContext.getAuthentication()).thenReturn(auth);
    }
}
