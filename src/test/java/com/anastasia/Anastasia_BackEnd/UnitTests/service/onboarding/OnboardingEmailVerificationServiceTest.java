package com.anastasia.Anastasia_BackEnd.UnitTests.service.onboarding;

import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplateService;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.OnboardingEmailVerificationCodeEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.OnboardingEmailVerificationCodeRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.OnboardingEmailVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class OnboardingEmailVerificationServiceTest {

    @Mock
    private OnboardingEmailVerificationCodeRepository repository;

    @Mock
    private EmailTemplateService emailTemplateService;

    @Mock
    private LocalizedMessageService messageService;

    private OnboardingEmailVerificationService service;

    @BeforeEach
    void setUp() {
        service = new OnboardingEmailVerificationService(repository, emailTemplateService, messageService);
        ReflectionTestUtils.setField(service, "helpUrl", "https://app.anastasia.com/help/security");
        when(messageService.currentLocale()).thenReturn(java.util.Locale.US);
        when(messageService.get(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        when(repository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(repository.save(any(OnboardingEmailVerificationCodeEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void sendCode_shouldUseBlockingEmailDeliveryPath() {
        service.sendCode("owner@example.com");

        ArgumentCaptor<OnboardingEmailVerificationCodeEntity> entityCaptor =
                ArgumentCaptor.forClass(OnboardingEmailVerificationCodeEntity.class);
        verify(repository).save(entityCaptor.capture());
        verify(emailTemplateService).sendTemplateEmailOrThrow(anyString(), anyString(), anyMap(), any());

        OnboardingEmailVerificationCodeEntity saved = entityCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("owner@example.com");
        assertThat(saved.getCodeHash()).isNotBlank();
        assertThat(saved.getExpiresAt()).isNotNull();
    }
}
