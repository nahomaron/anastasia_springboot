package com.anastasia.Anastasia_BackEnd.UnitTests.service.sms;

import com.anastasia.Anastasia_BackEnd.core.notification.channel.sms.OtpEntity;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.sms.service.PhoneVerificationService;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationChannelType;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationType;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.OtpRepository;
import com.google.common.hash.Hashing;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import org.springframework.context.ApplicationEventPublisher;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class PhoneVerificationServiceUnitTest {

    private static final String PHONE = "+251900000000";

    @Mock
    private OtpRepository otpRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PhoneVerificationService phoneVerificationService;

    @Test
    void startVerification_shouldInvalidateExistingOtpAndPublishNotification() {
        OtpEntity existing = OtpEntity.builder()
                .id(1L)
                .phone(PHONE)
                .otpHash("old-hash")
                .expiresAt(Instant.now().plusSeconds(5 * 60L))
                .build();

        when(otpRepository.findValid(eq(PHONE), any(Instant.class))).thenReturn(Optional.of(existing));

        phoneVerificationService.startVerification(PHONE);

        verify(otpRepository).delete(existing);

        ArgumentCaptor<OtpEntity> savedCaptor = ArgumentCaptor.forClass(OtpEntity.class);
        verify(otpRepository).save(savedCaptor.capture());

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        NotificationEvent event = eventCaptor.getValue();
        assertThat(event.getType()).isEqualTo(NotificationType.PHONE_VERIFICATION);
        assertThat(event.getChannels()).isEqualTo(EnumSet.of(NotificationChannelType.SMS));

        Map<String, Object> props = event.getProperties();
        assertThat(props.get("otp_code")).isNotNull();
        assertThat(props.get("otp_code").toString()).hasSize(6);
        assertThat(props.get("otp_expiry_minutes")).isEqualTo(5);
        assertThat(props.get("phone")).isEqualTo(PHONE);

        String generatedOtp = props.get("otp_code").toString();
        String expectedHash = Hashing.sha256()
                .hashString(generatedOtp, StandardCharsets.UTF_8)
                .toString();

        OtpEntity saved = savedCaptor.getValue();
        assertThat(saved.getPhone()).isEqualTo(PHONE);
        assertThat(saved.getOtpHash()).isEqualTo(expectedHash);
        assertThat(saved.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void startVerification_whenNoExistingOtp_shouldSkipDeletion() {
        when(otpRepository.findValid(eq(PHONE), any(Instant.class))).thenReturn(Optional.empty());

        phoneVerificationService.startVerification(PHONE);

        verify(otpRepository, never()).delete(any());
        verify(otpRepository).save(any(OtpEntity.class));
        verify(eventPublisher).publishEvent(any(NotificationEvent.class));
    }

    @Test
    void confirmOtp_whenMatches_shouldDeleteAndReturnTrue() {
        String rawOtp = "123456";
        String hash = Hashing.sha256().hashString(rawOtp, StandardCharsets.UTF_8).toString();
        OtpEntity entity = OtpEntity.builder()
                .phone(PHONE)
                .otpHash(hash)
                .expiresAt(Instant.now().plusSeconds(5 * 60L))
                .build();

        when(otpRepository.findValid(eq(PHONE), any(Instant.class))).thenReturn(Optional.of(entity));

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
                .expiresAt(Instant.now().plusSeconds(5 * 60L))
                .build();

        when(otpRepository.findValid(eq(PHONE), any(Instant.class))).thenReturn(Optional.of(entity));

        boolean result = phoneVerificationService.confirmOtp(PHONE, rawOtp);

        assertThat(result).isFalse();
        verify(otpRepository, never()).delete(entity);
    }

    @Test
    void confirmOtp_whenNoActiveOtp_shouldReturnFalse() {
        when(otpRepository.findValid(eq(PHONE), any(Instant.class))).thenReturn(Optional.empty());

        boolean result = phoneVerificationService.confirmOtp(PHONE, "000000");

        assertThat(result).isFalse();
    }
}
