package com.anastasia.Anastasia_BackEnd.UnitTests.service.registration;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.modules.registration.mappers.ChildMapper;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.ChildResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.ChildStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserType;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChildRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.ChildServiceImpl;
import com.anastasia.Anastasia_BackEnd.common.utils.SecurityUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChildServiceUnitTest {

    @Mock private ChildRepository childRepository;
    @Mock private ChurchRepository churchRepository;
    @Mock private UserRepository userRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private ChildMapper childMapper;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks
    private ChildServiceImpl childService;

    private Child_MemberEntity child;
    private UserEntity user;
    private ChurchEntity church;
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
                .build();

        user = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .email("test@example.com")
                .userType(UserType.GUEST)
                .build();

        church = new ChurchEntity();
        church.setTenant(TenantEntity.builder().id(tenantId).build());
        principal = new UserPrincipal(user);
        authentication = mock(Authentication.class);
        securityContext = mock(SecurityContext.class);

//        when(authentication.getPrincipal()).thenReturn(principal);
//        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
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

        ChildResponse response = childService.registerChild(child);

        assertNotNull(response);
        assertEquals("John Doe Smith", response.getName());
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
        when(childRepository.findByStatusNotAndTenantId(
                eq(ChildStatus.PENDING.name()),
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
        childService.updateChildDetails(childId, updateRequest);

        // Then
        ArgumentCaptor<Child_MemberEntity> captor = ArgumentCaptor.forClass(Child_MemberEntity.class);
        verify(childRepository).save(captor.capture());
        Child_MemberEntity updated = captor.getValue();

        assertEquals("NEW_CH", updated.getChurchNumber());
        assertEquals("NewFirst", updated.getFirstName());
        assertEquals("0000", updated.getPhone()); // unchanged because null in request
    }

    @Test
    void updateChildDetails_shouldNotCallSave_whenChildNotFound() {
        when(childRepository.findByIdAndTenantId(99L, tenantId)).thenReturn(Optional.empty());

        childService.updateChildDetails(99L, Child_MemberDTO.builder().build());

        verify(childRepository, never()).save(any());
    }
}
