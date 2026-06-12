package com.anastasia.Anastasia_BackEnd.UnitTests.core.notification.channel;

import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.EmailNotificationService;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationChannelType;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationPreferenceEntity;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationType;
import com.anastasia.Anastasia_BackEnd.core.notification.repository.NotificationPreferenceRepository;
import com.anastasia.Anastasia_BackEnd.core.notification.repository.NotificationRepository;
import com.anastasia.Anastasia_BackEnd.core.notification.service.EmailSuppressionService;
import com.anastasia.Anastasia_BackEnd.core.notification.service.NotificationIdempotencyService;
import com.anastasia.Anastasia_BackEnd.core.notification.service.TenantEmailPolicyService;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailCategory;
import com.anastasia.Anastasia_BackEnd.core.notification.template.TemplateService;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import jakarta.mail.Address;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class EmailNotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateService templateService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @Mock
    private NotificationIdempotencyService notificationIdempotencyService;

    @Mock
    private EmailSuppressionService emailSuppressionService;

    @Mock
    private TenantEmailPolicyService tenantEmailPolicyService;

    @Mock
    private TenantRepository tenantRepository;

    private EmailNotificationService emailNotificationService;

    @BeforeEach
    void setUp() {
        emailNotificationService = new EmailNotificationService(
                mailSender,
                templateService,
                notificationRepository,
                notificationPreferenceRepository,
                notificationIdempotencyService,
                emailSuppressionService,
                tenantEmailPolicyService,
                tenantRepository,
                true,
                "noreply@example.com"
        );
    }

    @Test
    void sendsBrandedFromHeaderForOutgoingEmail() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(emailSuppressionService.isSuppressed("user@example.com")).thenReturn(false);
        when(tenantEmailPolicyService.evaluate(null, EmailCategory.ADMIN_ALERT, NotificationType.NOTIFICATION))
                .thenReturn(new TenantEmailPolicyService.EmailPolicyDecision(true, null, null, null));

        emailNotificationService.sendEmail(
                "user@example.com",
                "Subject",
                "<p>Hello</p>",
                "Hello",
                new com.anastasia.Anastasia_BackEnd.core.notification.template.EmailSendMetadata(
                        EmailCategory.ADMIN_ALERT,
                        null,
                        null,
                        null,
                        "test-template"
                )
        );

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        Address[] from = messageCaptor.getValue().getFrom();
        assertThat(from).hasSize(1);
        assertThat(from[0].toString()).isEqualTo("Anastasis <noreply@example.com>");
    }

    @Test
    void doesNotSendWhenAddressIsSuppressed() {
        when(emailSuppressionService.isSuppressed("blocked@example.com")).thenReturn(true);

        emailNotificationService.sendEmail("blocked@example.com", "Subject", "html", "text", null);

        verify(mailSender, never()).send(any(jakarta.mail.internet.MimeMessage.class));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void doesNotSendWhenUserEmailPreferenceIsDisabled() {
        UserEntity user = new UserEntity();
        user.setUuid(UUID.randomUUID());
        user.setEmail("user@example.com");

        NotificationEvent event = new NotificationEvent(
                this,
                NotificationType.NOTIFICATION,
                user,
                Map.of(),
                Set.of(NotificationChannelType.EMAIL)
        );

        NotificationPreferenceEntity preference = new NotificationPreferenceEntity();
        preference.setUserId(user.getUuid());
        preference.setEmailEnabled(false);

        when(emailSuppressionService.isSuppressed("user@example.com")).thenReturn(false);
        when(notificationIdempotencyService.computeKey(event, NotificationChannelType.EMAIL, "user@example.com"))
                .thenReturn("idem-1");
        when(notificationRepository.existsByIdempotencyKeyAndChannel("idem-1", NotificationChannelType.EMAIL))
                .thenReturn(false);
        when(notificationPreferenceRepository.findByTenantIdIsNullAndUserId(user.getUuid()))
                .thenReturn(Optional.of(preference));
        when(templateService.renderTemplate(anyString(), any(Map.class))).thenReturn("<p>Hello</p>");
        when(tenantEmailPolicyService.evaluate(null, null, NotificationType.NOTIFICATION))
                .thenReturn(new TenantEmailPolicyService.EmailPolicyDecision(true, null, null, null));

        emailNotificationService.sendEmail(
                event,
                "Subject",
                com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplateName.NOTIFICATION
        );

        verify(mailSender, never()).send(any(jakarta.mail.internet.MimeMessage.class));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void doesNotSendWhenNotificationTypeIsMuted() {
        UserEntity user = new UserEntity();
        user.setUuid(UUID.randomUUID());
        user.setEmail("user@example.com");

        NotificationEvent event = new NotificationEvent(
                this,
                NotificationType.EVENT_REMINDER,
                user,
                Map.of(),
                EnumSet.of(NotificationChannelType.EMAIL)
        );

        NotificationPreferenceEntity preference = new NotificationPreferenceEntity();
        preference.setUserId(user.getUuid());
        preference.setEmailEnabled(true);
        preference.setMutedTypes(EnumSet.of(NotificationType.EVENT_REMINDER));

        when(emailSuppressionService.isSuppressed("user@example.com")).thenReturn(false);
        when(notificationIdempotencyService.computeKey(event, NotificationChannelType.EMAIL, "user@example.com"))
                .thenReturn("idem-2");
        when(notificationRepository.existsByIdempotencyKeyAndChannel("idem-2", NotificationChannelType.EMAIL))
                .thenReturn(false);
        when(notificationPreferenceRepository.findByTenantIdIsNullAndUserId(user.getUuid()))
                .thenReturn(Optional.of(preference));
        when(templateService.renderTemplate(anyString(), any(Map.class))).thenReturn("<p>Hello</p>");
        when(tenantEmailPolicyService.evaluate(null, null, NotificationType.EVENT_REMINDER))
                .thenReturn(new TenantEmailPolicyService.EmailPolicyDecision(true, null, null, null));

        emailNotificationService.sendEmail(
                event,
                "Subject",
                com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplateName.NOTIFICATION
        );

        verify(mailSender, never()).send(any(jakarta.mail.internet.MimeMessage.class));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void doesNotSendWhenTenantPolicyBlocksDelivery() {
        UUID tenantId = UUID.randomUUID();
        when(emailSuppressionService.isSuppressed("user@example.com")).thenReturn(false);
        when(tenantEmailPolicyService.evaluate(tenantId, EmailCategory.ADMIN_ALERT, NotificationType.NOTIFICATION))
                .thenReturn(new TenantEmailPolicyService.EmailPolicyDecision(
                        false,
                        TenantEmailPolicyService.ERROR_CODE_QUOTA_EXCEEDED,
                        "Tenant email monthly quota has been reached",
                        null
                ));

        emailNotificationService.sendEmail(
                "user@example.com",
                "Subject",
                "<p>Hello</p>",
                "Hello",
                new com.anastasia.Anastasia_BackEnd.core.notification.template.EmailSendMetadata(
                        EmailCategory.ADMIN_ALERT,
                        null,
                        tenantId,
                        null,
                        "test-template"
                )
        );

        verify(mailSender, never()).send(any(jakarta.mail.internet.MimeMessage.class));
        verify(notificationRepository).save(any());
    }
}
