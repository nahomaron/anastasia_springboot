package com.anastasia.Anastasia_BackEnd.IntegrationTest.service;

import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.MemberStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestResponse;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.core.auth.role.RoleType;
import com.anastasia.Anastasia_BackEnd.core.auth.service.AuthService;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantStatus;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.EmailNotificationService;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.PriestRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.TokenRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChildRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.ChildService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.ChurchService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.MemberService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.PriestService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.TenantService;
import com.anastasia.Anastasia_BackEnd.TestSupport.ServiceIntegrationTestBase;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@Epic("Integration Tests")
@Feature("Service Layer - Registration Domain")
@Transactional
class RegistrationServicesIT extends ServiceIntegrationTestBase {

    @Autowired private TenantService tenantService;
    @Autowired private ChurchService churchService;
    @Autowired private MemberService memberService;
    @Autowired private ChildService childService;
    @Autowired private PriestService priestService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private ChildRepository childRepository;
    @Autowired private PriestRepository priestRepository;
    @Autowired private TokenRepository tokenRepository;
    @Autowired private AuthService authService;

    @MockitoBean private EmailNotificationService emailNotificationService;

    @BeforeEach
    void initMocks() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void subscribeTenant_requiresEmailActivationBeforeTenantActivation() throws MessagingException {
        TenantDTO tenantDTO = TestDataUtil.createTestTenantDTO();
        tenantDTO.setOwnerEmail("owner+" + UUID.randomUUID() + "@example.com");
        tenantDTO.setPhoneNumber("+1555" + UUID.randomUUID().toString().substring(0, 8));
        tenantDTO.setPassword(TestDataUtil.TEST_PASSWORD);
        tenantDTO.setConfirmPassword(TestDataUtil.TEST_PASSWORD);

        tenantService.subscribeTenant(tenantDTO);

        verify(emailNotificationService).sendEmail(
                eq(tenantDTO.getOwnerEmail()),
                anyString(),
                anyString(),
                anyString(),
                any()
        );

        TenantEntity savedTenant = tenantRepository.findByPhoneNumber(tenantDTO.getPhoneNumber())
                .orElseThrow(() -> new AssertionError("Tenant not created"));
        assertThat(savedTenant.isPhoneVerified()).isFalse();
        assertThat(savedTenant.getPhoneVerifiedAt()).isNull();
        assertThat(savedTenant.getStatus()).isEqualTo(TenantStatus.PENDING_VERIFICATION);
        assertThat(savedTenant.getActivatedAt()).isNull();

        UserEntity adminUser = userRepository.findByEmail(tenantDTO.getOwnerEmail())
                .orElseThrow(() -> new AssertionError("Admin user not created"));
        assertThat(adminUser.getTenant().getId()).isEqualTo(savedTenant.getId());

        authService.activateAccount(rawActivationToken(tenantDTO.getOwnerEmail()), tenantDTO.getOwnerEmail());

        TenantEntity activatedTenant = tenantRepository.findById(savedTenant.getId())
                .orElseThrow(() -> new AssertionError("Tenant missing after activation"));
        assertThat(activatedTenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(activatedTenant.getActivatedAt()).isNotNull();
    }

    @Test
    void churchService_updateChurchPersistsChanges() {
        var updatedDto = TestDataUtil.createTestChurchDTO_B();
        var updatedEntity = churchService.convertToEntity(updatedDto);

        churchService.updateChurch(church.getChurchId(), updatedEntity);

        var reloaded = churchRepository.findById(church.getChurchId())
                .orElseThrow();

        assertThat(reloaded.getChurchNameLocal()).isEqualTo(updatedDto.getChurchNameLocal());
        assertThat(reloaded.getPrefix()).isEqualTo(updatedDto.getPrefix());
        assertThat(reloaded.getPrefixLocal()).isEqualTo(updatedDto.getPrefixLocal());
        assertThat(reloaded.getNeighborhood()).isEqualTo(updatedDto.getNeighborhood());
        assertThat(reloaded.getNeighborhoodLocal()).isEqualTo(updatedDto.getNeighborhoodLocal());
        assertThat(reloaded.getEmail()).isEqualTo(updatedDto.getEmail());
        assertThat(reloaded.getFacebook()).isEqualTo(updatedDto.getFacebook());
    }

    @Test
    void memberService_registerUpdateAndApproveFlow() {
        Role ownerRole = fetchRole(RoleType.OWNER);
        UserEntity user = persistUser("member+" + UUID.randomUUID() + "@example.com", ownerRole);
        authenticate(user);

        Adult_MemberDTO adultMemberDTO = TestDataUtil.createTestMemberDTO(church);
        Adult_MemberEntity adultMemberEntity = memberService.convertToEntity(adultMemberDTO);

        Adult_MemberResponse response = memberService.registerMember(adultMemberEntity);

        assertThat(response.getMembershipNumber()).isNotBlank();
        assertThat(memberRepository.count()).isEqualTo(1);

        Adult_MemberEntity saved = memberRepository.findAll().get(0);
        assertThat(saved.getChurch().getChurchId()).isEqualTo(church.getChurchId());

        UserEntity refreshedUser = userRepository.findById(user.getUuid()).orElseThrow();
        assertThat(refreshedUser.getMembership()).isNotNull();

        Adult_MemberDTO updateDto = TestDataUtil.createTestMemberDTO(church);
        updateDto.setFirstName("UpdatedName");
        updateDto.setFatherOfConfession("Updated Priest");
        memberService.updateMembershipDetails(saved.getId(), updateDto);

        Adult_MemberEntity updated = memberRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getFirstName()).isEqualTo("UpdatedName");
        assertThat(updated.getFatherOfConfession()).isEqualTo("Updated Priest");

        memberService.approveByPriest(saved.getId());
        memberService.approveByChurch(saved.getId());

        Adult_MemberEntity approved = memberRepository.findById(saved.getId()).orElseThrow();
        assertThat(approved.getStatus()).isEqualTo(MemberStatus.APPROVED.name());
    }

    @Test
    void childService_registerUpdateAndDeleteFlow() {
        Role ownerRole = fetchRole(RoleType.OWNER);
        UserEntity user = persistUser("child+" + UUID.randomUUID() + "@example.com", ownerRole);
        authenticate(user);

        Child_MemberDTO childMemberDTO = TestDataUtil.createTestChildDTO(church);
        Child_MemberEntity childMemberEntity = childService.convertToEntity(childMemberDTO);

        Child_MemberResponse response = childService.registerChild(childMemberEntity);

        assertThat(response.getMembershipNumber()).isNotBlank();
        assertThat(childRepository.count()).isEqualTo(1);

        Child_MemberEntity saved = childRepository.findAll().get(0);
        assertThat(saved.getChurch().getChurchId()).isEqualTo(church.getChurchId());

        Child_MemberDTO updateDto = TestDataUtil.createTestChildDTO(church);
        updateDto.setTitle("Young Deacon");
        childService.updateChildDetails(saved.getId(), updateDto);

        Child_MemberEntity updated = childRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("Young Deacon");

        childService.deleteChildMembership(saved.getId());
        assertThat(childRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void priestService_registerAndUpdateFlow() throws MessagingException {
        reset(emailNotificationService);

        PriestDTO priestDTO = TestDataUtil.createTestPriestDTO(church.getChurchNumber());

        priestService.registerPriest(priestDTO);

        verify(emailNotificationService).sendEmail(
                eq(priestDTO.getPersonalEmail()),
                anyString(),
                anyString(),
                anyString(),
                any()
        );

        PriestEntity saved = priestRepository.findAll().get(0);
        assertThat(saved.getChurch().getChurchNumber()).isEqualTo(church.getChurchNumber());
        assertThat(saved.getUser().getEmail()).isEqualTo(priestDTO.getPersonalEmail());

        PriestEntity patch = PriestEntity.builder()
                .prefixes("Abune")
                .churchEmail("updated-priest@church.org")
                .build();

        PriestResponse updated = priestService.updatePriestDetails(saved.getId(), patch, null);
        assertThat(updated.getPrefixes()).isEqualTo("Abune");
        assertThat(updated.getChurchEmail()).isEqualTo("updated-priest@church.org");

        priestService.deletePriest(saved.getId());
        assertThat(priestRepository.findById(saved.getId())).isEmpty();
    }
}
