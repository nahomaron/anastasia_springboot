package com.anastasia.Anastasia_BackEnd.UnitTests.service.sms;

import com.anastasia.Anastasia_BackEnd.core.notification.channel.sms.OtpEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.OtpRepository;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.sms.service.PhoneVerificationService;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.sms.service.SmsService;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.sms.service.SmsTemplateType;
import com.google.common.hash.Hashing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhoneVerificationServiceUnitTest {

    private static final String PHONE = "+251900000000";

    @Mock
    private SmsService smsService;
    @Mock
    private OtpRepository otpRepository;

    @InjectMocks
    private PhoneVerificationService phoneVerificationService;

    @BeforeEach
    void setUp() {
        lenient().when(smsService.sendSms(eq(PHONE), eq(SmsTemplateType.OTP), anyMap()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void startVerification_shouldInvalidateExistingOtpAndSendNewCode() {
        OtpEntity existing = OtpEntity.builder()
                .id(1L)
                .phone(PHONE)
                .otpHash("old-hash")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(otpRepository.findValid(eq(PHONE), any(LocalDateTime.class))).thenReturn(Optional.of(existing));

        phoneVerificationService.startVerification(PHONE);

        verify(otpRepository).delete(existing);

        ArgumentCaptor<OtpEntity> otpCaptor = ArgumentCaptor.forClass(OtpEntity.class);
        verify(otpRepository).save(otpCaptor.capture());

        ArgumentCaptor<Map<String, Object>> propsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(smsService).sendSms(eq(PHONE), eq(SmsTemplateType.OTP), propsCaptor.capture());

        Map<String, Object> props = propsCaptor.getValue();
        assertThat(props.get("otp_code")).isNotNull();
        assertThat(props.get("otp_code").toString()).hasSize(6);
        assertThat(props.get("otp_expiry_minutes")).isEqualTo(5);

        String generatedOtp = props.get("otp_code").toString();
        String expectedHash = Hashing.sha256()
                .hashString(generatedOtp, StandardCharsets.UTF_8)
                .toString();

        OtpEntity saved = otpCaptor.getValue();
        assertThat(saved.getPhone()).isEqualTo(PHONE);
        assertThat(saved.getOtpHash()).isEqualTo(expectedHash);
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void startVerification_whenNoExistingOtp_shouldSkipDeletion() {
        when(otpRepository.findValid(eq(PHONE), any(LocalDateTime.class))).thenReturn(Optional.empty());

        phoneVerificationService.startVerification(PHONE);

        verify(otpRepository, never()).delete(any());
        verify(otpRepository).save(any(OtpEntity.class));
    }

    @Test
    void confirmOtp_whenMatches_shouldDeleteAndReturnTrue() {
        String rawOtp = "123456";
        String hash = Hashing.sha256().hashString(rawOtp, StandardCharsets.UTF_8).toString();
        OtpEntity entity = OtpEntity.builder()
                .phone(PHONE)
                .otpHash(hash)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(otpRepository.findValid(eq(PHONE), any(LocalDateTime.class))).thenReturn(Optional.of(entity));

        boolean result = phoneVerificationService.confirmOtp(PHONE, rawOtp);

        assertThat(result).isTrue();
        verify(otpRepository).delete(entity);
    }

    @Test
    void confirmOtp_whenCodeMismatch_shouldReturnFalseAndNotDelete() {
        String rawOtp = "654321";
        String hash = Hashing.sha256().hashString("123456", StandardCharsets.UTF_8).toString();
        OtpEntity entity = OtpEntity.builder()
                .phone(PHONE)
                .otpHash(hash)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(otpRepository.findValid(eq(PHONE), any(LocalDateTime.class))).thenReturn(Optional.of(entity));

        boolean result = phoneVerificationService.confirmOtp(PHONE, rawOtp);

        assertThat(result).isFalse();
        verify(otpRepository, never()).delete(entity);
    }

    @Test
    void confirmOtp_whenNoActiveOtp_shouldReturnFalse() {
        when(otpRepository.findValid(eq(PHONE), any(LocalDateTime.class))).thenReturn(Optional.empty());

        boolean result = phoneVerificationService.confirmOtp(PHONE, "000000");

        assertThat(result).isFalse();
    }
}
