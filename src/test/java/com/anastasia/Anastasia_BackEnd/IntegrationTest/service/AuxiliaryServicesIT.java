package com.anastasia.Anastasia_BackEnd.IntegrationTest.service;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarType;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.core.auth.role.RoleType;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.AvatarRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.OtpRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.AvatarService;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.sms.service.PhoneVerificationService;
import com.anastasia.Anastasia_BackEnd.TestSupport.TestSmsService;
import com.anastasia.Anastasia_BackEnd.TestSupport.ServiceIntegrationTestBase;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Integration Tests")
@Feature("Service Layer - Auxiliary Services")
class AuxiliaryServicesIT extends ServiceIntegrationTestBase {

    @Autowired private PhoneVerificationService phoneVerificationService;
    @Autowired private TestSmsService testSmsService;
    @Autowired private OtpRepository otpRepository;
    @Autowired private AvatarService avatarService;
    @Autowired private AvatarRepository avatarRepository;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        Role ownerRole = fetchRole(RoleType.OWNER);
        user = persistUser("aux+" + UUID.randomUUID() + "@integration.com", ownerRole);
    }

    @Test
    void phoneVerification_handlesOtpLifecycle() {
        String phone = "+1550" + UUID.randomUUID().toString().substring(0, 8);

        phoneVerificationService.startVerification(phone);

        String issuedOtp = testSmsService.getLastOtpForPhone(phone)
                .orElseThrow(() -> new AssertionError("OTP not issued"));

        boolean invalid = phoneVerificationService.confirmOtp(phone, "000000");
        assertThat(invalid).isFalse();

        boolean valid = phoneVerificationService.confirmOtp(phone, issuedOtp);
        assertThat(valid).isTrue();
        assertThat(otpRepository.findValid(phone, java.time.LocalDateTime.now())).isEmpty();

        phoneVerificationService.resendOtp(phone);
        String resentOtp = testSmsService.getLastOtpForPhone(phone)
                .orElseThrow(() -> new AssertionError("Resent OTP missing"));
        assertThat(resentOtp).isNotEqualTo(issuedOtp);
    }

    @Test
    void avatarService_returnsPresignedUrlAndRetrievesAvatar() {
        var presigned = avatarService.requestPresignedUrl("profile.png");
        assertThat(presigned.getObjectKey()).startsWith("test-avatars/");
        assertThat(presigned.getUploadUrl()).contains("mock-presigned-url");

        AvatarEntity directAvatar = AvatarEntity.builder()
                .ownerId(user.getUuid())
                .avatarType(AvatarType.USER)
                .imageUrl("https://cdn.example.com/avatar.png")
                .imageSize("15KB")
                .build();
        avatarRepository.save(directAvatar);

        AvatarDTO fetched = avatarService.getAvatar("USER", user.getUuid());
        assertThat(fetched.getImageUrl()).isEqualTo("https://cdn.example.com/avatar.png");
        assertThat(fetched.getImageSize()).isEqualTo("15KB");
    }
}
