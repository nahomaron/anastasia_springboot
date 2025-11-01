package com.anastasia.Anastasia_BackEnd.IntegrationTest.service;

import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.child.ChildDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.child.ChildEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.child.ChildResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.MemberDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.MemberResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.MemberStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestEntity;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.core.auth.role.RoleType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.EmailNotificationService;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.PriestRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.TokenRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChildRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplateName;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.ChildService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.ChurchService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.MemberService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.PriestService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.TenantService;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.sms.service.TestSmsService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@Epic("Integration Tests")
@Feature("Service Layer - Registration Domain")
class RegistrationServicesIT extends ServiceIntegrationTestBase {

    @Autowired private TenantService tenantService;
    @Autowired private ChurchService churchService;
    @Autowired private MemberService memberService;
    @Autowired private ChildService childService;
    @Autowired private PriestService priestService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private ChildRepository childRepository;
    @Autowired private PriestRepository priestRepository;
    @Autowired private TestSmsService testSmsService;
    @Autowired private TokenRepository tokenRepository;

    @MockitoBean private EmailNotificationService emailNotificationService;
    @Captor private ArgumentCaptor<Map<String, Object>> emailTemplateCaptor;

    @BeforeEach
    void initMocks() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void subscribeTenant_andVerifyPhone_success() throws MessagingException {
        TenantDTO tenantDTO = TestDataUtil.createTestTenantDTO();
        tenantDTO.setEmail("owner+" + UUID.randomUUID() + "@example.com");
        tenantDTO.setPhoneNumber("+1555" + UUID.randomUUID().toString().substring(0, 8));
        tenantDTO.setPassword(TestDataUtil.TEST_PASSWORD);
        tenantDTO.setConfirmPassword(TestDataUtil.TEST_PASSWORD);

        tenantService.subscribeTenant(tenantDTO);

        verify(emailNotificationService).sendEmail(
                eq(tenantDTO.getEmail()),
                eq("Account Activation for Anastasia"),
                eq(EmailTemplateName.ACTIVATE_ACCOUNT),
                emailTemplateCaptor.capture()
        );

        Map<String, Object> templateProps = emailTemplateCaptor.getValue();
        assertThat(templateProps)
                .containsKeys("username", "confirmation_url", "activation_code");

        TenantEntity savedTenant = tenantRepository.findByPhoneNumber(tenantDTO.getPhoneNumber())
                .orElseThrow(() -> new AssertionError("Tenant not created"));
        assertThat(savedTenant.isPhoneVerified()).isFalse();

        UserEntity adminUser = userRepository.findByEmail(tenantDTO.getEmail())
                .orElseThrow(() -> new AssertionError("Admin user not created"));
        assertThat(adminUser.getTenant().getId()).isEqualTo(savedTenant.getId());

        assertThat(tokenRepository.findByUserUuid(adminUser.getUuid()))
                .as("Activation token persisted")
                .isNotNull();

        String otp = testSmsService.getLastOtpForPhone(tenantDTO.getPhoneNumber())
                .orElseThrow(() -> new AssertionError("OTP not generated"));

        boolean verified = tenantService.verifyTenantPhone(tenantDTO.getPhoneNumber(), otp);
        assertThat(verified).isTrue();

        TenantEntity refreshed = tenantRepository.findById(savedTenant.getId())
                .orElseThrow();
        assertThat(refreshed.isPhoneVerified()).isTrue();
    }

    @Test
    void churchService_updateChurchPersistsChanges() {
        var updatedDto = TestDataUtil.createTestChurchDTO_B();
        var updatedEntity = churchService.convertToEntity(updatedDto);

        churchService.updateChurch(church.getChurchId(), updatedEntity);

        var reloaded = churchRepository.findById(church.getChurchId())
                .orElseThrow();

        assertThat(reloaded.getChurchName()).isEqualTo(updatedDto.getChurchName());
        assertThat(reloaded.getEmail()).isEqualTo(updatedDto.getEmail());
        assertThat(reloaded.getFacebookPage()).isEqualTo(updatedDto.getFacebookPage());
    }

    @Test
    void memberService_registerUpdateAndApproveFlow() {
        Role ownerRole = fetchRole(RoleType.OWNER);
        UserEntity user = persistUser("member+" + UUID.randomUUID() + "@example.com", ownerRole);
        authenticate(user);

        MemberDTO memberDTO = TestDataUtil.createTestMemberDTO(church);
        MemberEntity memberEntity = memberService.convertToEntity(memberDTO);

        MemberResponse response = memberService.registerMember(memberEntity);

        assertThat(response.getMembershipNumber()).isNotBlank();
        assertThat(memberRepository.count()).isEqualTo(1);

        MemberEntity saved = memberRepository.findAll().get(0);
        assertThat(saved.getChurch().getChurchId()).isEqualTo(church.getChurchId());

        UserEntity refreshedUser = userRepository.findById(user.getUuid()).orElseThrow();
        assertThat(refreshedUser.getMembership()).isNotNull();

        MemberDTO updateDto = TestDataUtil.createTestMemberDTO(church);
        updateDto.setFirstName("UpdatedName");
        updateDto.setFatherOfConfession("Updated Priest");
        memberService.updateMembershipDetails(saved.getId(), updateDto);

        MemberEntity updated = memberRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getFirstName()).isEqualTo("UpdatedName");
        assertThat(updated.getFatherOfConfession()).isEqualTo("Updated Priest");

        memberService.approveByPriest(saved.getId());
        memberService.approveByChurch(saved.getId());

        MemberEntity approved = memberRepository.findById(saved.getId()).orElseThrow();
        assertThat(approved.getStatus()).isEqualTo(MemberStatus.APPROVED.name());
    }

    @Test
    void childService_registerUpdateAndDeleteFlow() {
        Role ownerRole = fetchRole(RoleType.OWNER);
        UserEntity user = persistUser("child+" + UUID.randomUUID() + "@example.com", ownerRole);
        authenticate(user);

        ChildDTO childDTO = TestDataUtil.createTestChildDTO(church);
        ChildEntity childEntity = childService.convertToEntity(childDTO);

        ChildResponse response = childService.registerChild(childEntity);

        assertThat(response.getMembershipNumber()).isNotBlank();
        assertThat(childRepository.count()).isEqualTo(1);

        ChildEntity saved = childRepository.findAll().get(0);
        assertThat(saved.getChurch().getChurchId()).isEqualTo(church.getChurchId());

        ChildDTO updateDto = TestDataUtil.createTestChildDTO(church);
        updateDto.setTitle("Young Deacon");
        childService.updateChildDetails(saved.getId(), updateDto);

        ChildEntity updated = childRepository.findById(saved.getId()).orElseThrow();
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
                eq("Account Activation for Anastasia"),
                eq(EmailTemplateName.ACTIVATE_ACCOUNT),
                emailTemplateCaptor.capture()
        );

        Map<String, Object> templateProps = emailTemplateCaptor.getValue();
        assertThat(templateProps.get("username")).isNotNull();

        PriestEntity saved = priestRepository.findAll().get(0);
        assertThat(saved.getChurch().getChurchNumber()).isEqualTo(church.getChurchNumber());
        assertThat(saved.getUser().getEmail()).isEqualTo(priestDTO.getPersonalEmail());

        PriestEntity patch = PriestEntity.builder()
                .prefixes("Abune")
                .churchEmail("updated-priest@church.org")
                .build();

        PriestEntity updated = priestService.updatePriestDetails(saved.getId(), patch);
        assertThat(updated.getPrefixes()).isEqualTo("Abune");
        assertThat(updated.getChurchEmail()).isEqualTo("updated-priest@church.org");

        priestService.deletePriest(saved.getId());
        assertThat(priestRepository.findById(saved.getId())).isEmpty();
    }
}
