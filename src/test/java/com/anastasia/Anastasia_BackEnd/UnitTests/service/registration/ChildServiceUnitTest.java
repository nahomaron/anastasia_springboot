package com.anastasia.Anastasia_BackEnd.UnitTests.service.registration;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.registration.mappers.ChildMapper;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.MemberLifecycleStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.ChildStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.MembershipStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantAdminAssignmentEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserType;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.notification.service.TenantAdminNotificationService;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChildRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.PriestRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantAdminAssignmentRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.ActiveMemberLimitPolicy;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.ChildServiceImpl;
import com.anastasia.Anastasia_BackEnd.common.utils.SecurityUtils;
import org.junit.jupiter.api.*;
import org.mockito.*;
import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@LenientMockitoTest
public class ChildServiceUnitTest {

    @Mock private ChildRepository childRepository;
    @Mock private ChurchRepository churchRepository;
    @Mock private UserRepository userRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private PriestRepository priestRepository;
    @Mock private TenantAdminAssignmentRepository tenantAdminAssignmentRepository;
    @Mock private ChildMapper childMapper;
    @Mock private SecurityUtils securityUtils;
    @Mock private TenantAdminNotificationService tenantAdminNotificationService;
    @Mock private LocalizedMessageService messageService;
    @Mock private ActiveMemberLimitPolicy activeMemberLimitPolicy;

    @InjectMocks
    private ChildServiceImpl childService;

    private Child_MemberEntity child;
    private UserEntity user;
    private ChurchEntity church;
    private ChurchEntity updateChurch;
    private UserPrincipal principal;
    private Authentication authentication;
    private SecurityContext securityContext;
    private UUID tenantId;

    @BeforeEach
    void setup() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
        child = Child_MemberEntity.builder()
                .churchNumber("CH123")
                .firstName("John")
                .fatherName("Doe")
                .grandFatherName("Smith")
                .deacon(false)
                .tenantId(tenantId)
                .build();

        user = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .email("test@example.com")
                .userType(UserType.GUEST)
                .build();

        church = new ChurchEntity();
        church.setChurchId(101L);
        church.setChurchNumber("CH123");
        church.setTenant(TenantEntity.builder().id(tenantId).build());

        updateChurch = new ChurchEntity();
        updateChurch.setChurchId(202L);
        updateChurch.setChurchNumber("NEW_CH");
        updateChurch.setTenant(TenantEntity.builder().id(tenantId).build());
        principal = new UserPrincipal(user);
        authentication = mock(Authentication.class);
        securityContext = mock(SecurityContext.class);

//        when(authentication.getPrincipal()).thenReturn(principal);
//        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        lenient().when(childMapper.childEntityToResponse(any())).thenAnswer(invocation -> {
            Child_MemberEntity source = invocation.getArgument(0);
            if (source == null) {
                return Child_MemberResponse.builder().build();
            }
            return Child_MemberResponse.builder()
                    .id(source.getId())
                    .firstName(source.getFirstName())
                    .build();
        });
        lenient().when(messageService.get(anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        lenient().when(messageService.get(anyString(), anyString(), any(Object[].class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        lenient().when(churchRepository.findByChurchNumber(anyString())).thenAnswer(invocation -> {
            String requested = invocation.getArgument(0);
            if ("NEW_CH".equalsIgnoreCase(requested)) {
                return Optional.of(updateChurch);
            }
            return Optional.of(church);
        });
        lenient().when(childRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void testRegisterChild_Success() {
        when(authentication.getPrincipal()).thenReturn(principal);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        when(userRepository.findById(user.getUuid())).thenReturn(Optional.of(user));
        when(churchRepository.findByChurchNumber(child.getChurchNumber())).thenReturn(Optional.of(church));
        when(securityUtils.generateUniqueIDNumber(anyInt(), anyString())).thenReturn("C12345");
        when(childRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Child_MemberResponse response = childService.registerChild(child);

        assertNotNull(response);
        assertEquals("John", response.getFirstName());
        assertEquals("C12345", child.getMembershipNumber());

        verify(childRepository).save(any());
    }

    @Test
    void testRegisterChild_throwsIfUserNotAuthenticated() {
        SecurityContextHolder.clearContext();
        assertThrows(IllegalStateException.class, () -> childService.registerChild(child));
    }

    @Test
    void testRegisterChild_throwsIfUserNotFound() {
        when(authentication.getPrincipal()).thenReturn(principal);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        when(userRepository.findById(user.getUuid())).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> childService.registerChild(child));
    }

    @Test
    void testRegisterChild_throwsIfChurchNotFound() {
        when(authentication.getPrincipal()).thenReturn(principal);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        when(userRepository.findById(user.getUuid())).thenReturn(Optional.of(user));
        when(churchRepository.findByChurchNumber(child.getChurchNumber())).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> childService.registerChild(child));
    }

    @Test
    void testFindAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Child_MemberEntity> page = new PageImpl<>(List.of(child));
        Child_MemberResponse response = Child_MemberResponse.builder().id(1L).build();
        when(childRepository.findByStatusValueNotAndTenantId(
                eq(MemberLifecycleStatus.PENDING),
                eq(tenantId),
                eq(pageable)))
                .thenReturn(page);
        when(childMapper.childEntityToResponse(child)).thenReturn(response);

        Page<Child_MemberResponse> result = childService.findAll(pageable);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testFindChildById() {
        Child_MemberResponse response = Child_MemberResponse.builder().id(1L).build();
        when(childRepository.findByIdAndTenantId(1L, tenantId)).thenReturn(Optional.of(child));
        when(childMapper.childEntityToResponse(child)).thenReturn(response);
        Optional<Child_MemberResponse> result = childService.findChildById(1L);
        assertTrue(result.isPresent());
    }

    @Test
    void testDeleteChildMembership() {
        when(childRepository.findByIdAndTenantId(1L, tenantId)).thenReturn(Optional.of(child));
        childService.deleteChildMembership(1L);
        verify(childRepository).delete(child);
    }

    @Test
    void updateChildDetails_shouldUpdateOnlyNonNullFields() {
        // Given
        Long childId = 1L;
        Child_MemberEntity existing = Child_MemberEntity.builder()
                .churchNumber("OLD_CH")
                .firstName("OldFirst")
                .phone("0000")
                .build();

        Child_MemberDTO updateRequest = Child_MemberDTO.builder()
                .churchNumber("NEW_CH")
                .firstName("NewFirst")
                .phone(null) // Should stay as "0000"
                .build();

        when(childRepository.findByIdAndTenantId(childId, tenantId)).thenReturn(Optional.of(existing));

        // When
        Child_MemberResponse response = childService.updateChildDetails(childId, updateRequest);

        // Then
        ArgumentCaptor<Child_MemberEntity> captor = ArgumentCaptor.forClass(Child_MemberEntity.class);
        verify(childRepository).save(captor.capture());
        Child_MemberEntity updated = captor.getValue();

        assertEquals("NEW_CH", updated.getChurchNumber());
        assertEquals("NewFirst", updated.getFirstName());
        assertEquals("0000", updated.getPhone()); // unchanged because null in request
        assertEquals("NewFirst", response.getFirstName());
    }

    @Test
    void updateChildDetails_shouldThrow_whenChildNotFound() {
        when(childRepository.findByIdAndTenantId(99L, tenantId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> childService.updateChildDetails(99L, Child_MemberDTO.builder().build()));
        verify(childRepository, never()).save(any());
    }

    @Test
    void registerChildAsAdmin_activatesDirectlyWhenPriestIsInvalid() {
        child.setChurchNumber(church.getChurchNumber());
        child.setPriestNumber("PRIEST-404");
        when(authentication.getPrincipal()).thenReturn(principal);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(userRepository.findById(user.getUuid())).thenReturn(Optional.of(user));
        when(tenantAdminAssignmentRepository.findByTenant_IdAndUserId(tenantId, user.getUuid()))
                .thenReturn(Optional.of(TenantAdminAssignmentEntity.builder()
                        .tenant(church.getTenant())
                        .userId(user.getUuid())
                        .status(MembershipStatus.ACTIVE)
                        .build()));
        when(churchRepository.findByTenantId(tenantId)).thenReturn(Optional.of(church));
        when(securityUtils.generateUniqueIDNumber(anyInt(), anyString())).thenReturn("C12345");

        childService.registerChildAsAdmin(child);

        assertEquals(ChildStatus.ACTIVE.name(), child.getStatus());
        assertTrue(child.isApprovedByChurch());
        assertNull(child.getPriestNumber());
        verify(activeMemberLimitPolicy).assertCanActivateMembers(tenantId, 1);
    }

    @Test
    void registerChildAsAdmin_keepsPendingWhenPriestBelongsToChurch() {
        child.setChurchNumber(church.getChurchNumber());
        child.setPriestNumber("PR12345");
        PriestEntity priest = PriestEntity.builder()
                .priestNumber("PR12345")
                .status(PriestStatus.ACTIVE)
                .church(church)
                .build();
        when(authentication.getPrincipal()).thenReturn(principal);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(userRepository.findById(user.getUuid())).thenReturn(Optional.of(user));
        when(tenantAdminAssignmentRepository.findByTenant_IdAndUserId(tenantId, user.getUuid()))
                .thenReturn(Optional.of(TenantAdminAssignmentEntity.builder()
                        .tenant(church.getTenant())
                        .userId(user.getUuid())
                        .status(MembershipStatus.ACTIVE)
                        .build()));
        when(churchRepository.findByTenantId(tenantId)).thenReturn(Optional.of(church));
        when(priestRepository.findByPriestNumber("PR12345")).thenReturn(Optional.of(priest));
        when(securityUtils.generateUniqueIDNumber(anyInt(), anyString())).thenReturn("C54321");

        childService.registerChildAsAdmin(child);

        assertEquals(ChildStatus.PENDING.name(), child.getStatus());
        assertTrue(child.isApprovedByChurch());
        assertEquals("PR12345", child.getPriestNumber());
        verify(activeMemberLimitPolicy, never()).assertCanActivateMembers(any(), anyInt());
    }

    @Test
    void mappedReadMethods_areTransactionalReadOnly() throws NoSuchMethodException {
        assertTransactionalReadOnly("findAll", Pageable.class);
        assertTransactionalReadOnly("findAllSummary", Pageable.class, String.class);
        assertTransactionalReadOnly("findByTenantAndPriestNumber", UUID.class, String.class, Pageable.class);
        assertTransactionalReadOnly("findByTenantAndPriestNumberSummary", UUID.class, String.class, Pageable.class, String.class);
        assertTransactionalReadOnly("findPending", Pageable.class);
        assertTransactionalReadOnly("searchNonPending", Pageable.class, String.class);
        assertTransactionalReadOnly("searchNonPendingSummary", Pageable.class, String.class, String.class);
        assertTransactionalReadOnly("findChildById", Long.class);
        assertTransactionalReadOnly("findAllBySpecification", org.springframework.data.jpa.domain.Specification.class, Pageable.class);
    }

    private void assertTransactionalReadOnly(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = ChildServiceImpl.class.getMethod(methodName, parameterTypes);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertTrue(transactional.readOnly());
    }
}
