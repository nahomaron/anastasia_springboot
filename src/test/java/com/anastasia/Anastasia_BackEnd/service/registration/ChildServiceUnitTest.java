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
    @Mock private ChildMapper childMapper;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks
    private ChildServiceImpl childService;

    private ChildEntity child;
    private UserEntity user;
    private ChurchEntity church;
    private UserPrincipal principal;
    private Authentication authentication;
    private SecurityContext securityContext;

    @BeforeEach
    void setup() {
        child = ChildEntity.builder()
                .churchNumber("CH123")
                .firstName("John")
                .fatherName("Doe")
                .grandFatherName("Smith")
                .deacon(false)
                .build();

        user = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .email("test@example.com")
                .build();

        church = new ChurchEntity();
        principal = new UserPrincipal(user);
        authentication = mock(Authentication.class);
        securityContext = mock(SecurityContext.class);

//        when(authentication.getPrincipal()).thenReturn(principal);
//        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
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
        Page<ChildEntity> page = new PageImpl<>(List.of(child));
        when(childRepository.findAll(pageable)).thenReturn(page);

        Page<ChildEntity> result = childService.findAll(pageable);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testFindChildById() {
        when(childRepository.findById(1L)).thenReturn(Optional.of(child));
        Optional<ChildEntity> result = childService.findChildById(1L);
        assertTrue(result.isPresent());
    }

    @Test
    void testDeleteChildMembership() {
        childService.deleteChildMembership(1L);
        verify(childRepository).deleteById(1L);
    }

    @Test
    void updateChildDetails_shouldUpdateOnlyNonNullFields() {
        // Given
        Long childId = 1L;
        ChildEntity existing = ChildEntity.builder()
                .churchNumber("OLD_CH")
                .firstName("OldFirst")
                .phone("0000")
                .build();

        ChildDTO updateRequest = ChildDTO.builder()
                .churchNumber("NEW_CH")
                .firstName("NewFirst")
                .phone(null) // Should stay as "0000"
                .build();

        when(childRepository.findById(childId)).thenReturn(Optional.of(existing));

        // When
        childService.updateChildDetails(childId, updateRequest);

        // Then
        ArgumentCaptor<ChildEntity> captor = ArgumentCaptor.forClass(ChildEntity.class);
        verify(childRepository).save(captor.capture());
        ChildEntity updated = captor.getValue();

        assertEquals("NEW_CH", updated.getChurchNumber());
        assertEquals("NewFirst", updated.getFirstName());
        assertEquals("0000", updated.getPhone()); // unchanged because null in request
    }

    @Test
    void updateChildDetails_shouldNotCallSave_whenChildNotFound() {
        when(childRepository.findById(99L)).thenReturn(Optional.empty());

        childService.updateChildDetails(99L, ChildDTO.builder().build());

        verify(childRepository, never()).save(any());
    }
}

