package com.anastasia.Anastasia_BackEnd.UnitTests.service.registration;

import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.registration.mappers.PriestMapper;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestResponse;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.core.auth.role.RoleType;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserType;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.PriestRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.service.AuthServiceImpl;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.PriestServiceImpl;
import com.anastasia.Anastasia_BackEnd.common.utils.SecurityUtils;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@LenientMockitoTest
public class PriestServiceUnitTest {

    @Mock private PriestMapper priestMapper;
    @Mock private PriestRepository priestRepository;
    @Mock private ChurchRepository churchRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TenantRepository tenantRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthServiceImpl authService;
    @Mock private RoleRepository roleRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private LocalizedMessageService messageService;


    @InjectMocks
    private PriestServiceImpl priestService;

    private PriestDTO priestDTO;
    private UserEntity priestUser;
    private Role priestRole;
    private ChurchEntity priestChurch;

    @BeforeEach
    void setup() {
        priestRole = Role.builder()
                .roleName(RoleType.PRIEST.name())
                .build();

        lenient().when(messageService.get(anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        lenient().when(messageService.get(anyString(), anyString(), any(Object[].class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        priestDTO = PriestDTO.builder()
                .firstName("Abune")
                .fatherName("Paulos")
                .grandFatherName("Tesfa")
                .personalEmail("abune@example.com")
                .password("secure")
                .churchNumber("CH123")
            .build();

        priestChurch = new ChurchEntity();
        priestChurch.setChurchNumber(priestDTO.getChurchNumber());
        priestChurch.setTenant(TenantEntity.builder().id(UUID.randomUUID()).build());

        priestUser = UserEntity.builder()
                .fullName("Abune Paulos Tesfa")
                .email("abune@example.com")
                .roles(Set.of(priestRole))
                .userType(UserType.PRIEST)
                .build();

    }

    @Test
    void testRegisterPriest_createsNewUserAndSavesPriest() throws MessagingException {
        when(userRepository.findByEmail(priestDTO.getPersonalEmail())).thenReturn(Optional.empty());
        when(roleRepository.findByRoleName("PRIEST")).thenReturn(Optional.of(priestRole));
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        when(userRepository.save(any())).thenReturn(priestUser);
        when(securityUtils.generateUniqueIDNumber(anyInt(), anyString())).thenReturn("K12345");
        when(churchRepository.findByChurchNumber(priestDTO.getChurchNumber())).thenReturn(Optional.of(priestChurch));

        assertDoesNotThrow(() -> priestService.registerPriest(priestDTO));

        verify(userRepository, times(2)).save(any());
        verify(priestRepository).save(any());
        verify(authService).sendValidationEmail(any());
    }

    @Test
    void testRegisterPriest_throwsIfBothTenantAndChurchPresent() {
        when(roleRepository.findByRoleName(RoleType.PRIEST.name()))
                .thenReturn(Optional.of(priestRole));

        priestDTO.setTenantId(UUID.randomUUID());

        assertThrows(IllegalStateException.class, () -> priestService.registerPriest(priestDTO));
    }

    @Test
    void testRegisterPriest_throwsIfNeitherTenantNorChurchProvided() {
        when(roleRepository.findByRoleName(RoleType.PRIEST.name()))
                .thenReturn(Optional.of(priestRole));

        priestDTO.setChurchNumber(null);

        assertThrows(IllegalStateException.class, () -> priestService.registerPriest(priestDTO));
    }

    @Test
    void testUpdatePriestDetails_updatesAndSaves() {
        PriestEntity input = PriestEntity.builder()
                .firstName("NewFirst")
                .build();

        PriestEntity found = PriestEntity.builder()
                .priestNumber("K12345")
                .build();

        PriestResponse response = PriestResponse.builder()
                .firstName("NewFirst")
                .build();

        when(priestRepository.findById(1L)).thenReturn(Optional.of(found));
        when(priestRepository.save(any())).thenReturn(found);
        when(priestMapper.priestEntityToResponse(found)).thenReturn(response);

        PriestResponse result = priestService.updatePriestDetails(1L, input, null);

        assertNotNull(result);
        verify(priestRepository).save(any());
    }

    @Test
    void testDeletePriest() {
        priestService.deletePriest(1L);
        verify(priestRepository).deleteById(1L);
    }

    @Test
    void testFindPriestById_found() {
        PriestEntity entity = new PriestEntity();
        when(priestRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(priestMapper.priestEntityToResponse(entity)).thenReturn(PriestResponse.builder().build());
        assertTrue(priestService.findPriestById(1L).isPresent());
    }

    @Test
    void testFindPriestById_notFound() {
        when(priestRepository.findById(1L)).thenReturn(Optional.empty());
        assertFalse(priestService.findPriestById(1L).isPresent());
    }
}
