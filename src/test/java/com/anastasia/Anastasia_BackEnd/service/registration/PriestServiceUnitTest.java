package com.anastasia.Anastasia_BackEnd.service.registration;

import com.anastasia.Anastasia_BackEnd.mappers.PriestMapper;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.priest.PriestDTO;
import com.anastasia.Anastasia_BackEnd.model.priest.PriestEntity;
import com.anastasia.Anastasia_BackEnd.model.role.Role;
import com.anastasia.Anastasia_BackEnd.model.role.RoleType;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.repository.PriestRepository;
import com.anastasia.Anastasia_BackEnd.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.RoleRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.UserRepository;
import com.anastasia.Anastasia_BackEnd.service.auth.AuthServiceImpl;
import com.anastasia.Anastasia_BackEnd.util.SecurityUtils;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
        import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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


    @InjectMocks
    private PriestServiceImpl priestService;

    private PriestDTO priestDTO;
    private UserEntity priestUser;
    private Role priestRole;

    @BeforeEach
    void setup() {
        priestRole = Role.builder()
                .roleName(RoleType.PRIEST.name())
                .build();

        priestDTO = PriestDTO.builder()
                .firstName("Abune")
                .fatherName("Paulos")
                .grandFatherName("Tesfa")
                .personalEmail("abune@example.com")
                .password("secure")
                .churchNumber("CH123")
                .build();

        priestUser = UserEntity.builder()
                .fullName("Abune Paulos Tesfa")
                .email("abune@example.com")
                .roles(Set.of(priestRole))
                .build();

    }

    @Test
    void testRegisterPriest_createsNewUserAndSavesPriest() throws MessagingException {
        when(userRepository.findByEmail(priestDTO.getPersonalEmail())).thenReturn(Optional.empty());
        when(roleRepository.findByRoleName("PRIEST")).thenReturn(Optional.of(priestRole));
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        when(userRepository.save(any())).thenReturn(priestUser);
        when(securityUtils.generateUniqueIDNumber(anyInt(), anyString())).thenReturn("K12345");
        when(churchRepository.findByChurchNumber(priestDTO.getChurchNumber())).thenReturn(Optional.of(new ChurchEntity()));

        assertDoesNotThrow(() -> priestService.registerPriest(priestDTO));

        verify(userRepository).save(any());
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

        when(priestRepository.findById(1L)).thenReturn(Optional.of(found));
        when(priestRepository.save(any())).thenReturn(found);

        PriestEntity result = priestService.updatePriestDetails(1L, input);

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
        when(priestRepository.findById(1L)).thenReturn(Optional.of(new PriestEntity()));
        assertTrue(priestService.findPriestById(1L).isPresent());
    }

    @Test
    void testFindPriestById_notFound() {
        when(priestRepository.findById(1L)).thenReturn(Optional.empty());
        assertFalse(priestService.findPriestById(1L).isPresent());
    }
}
