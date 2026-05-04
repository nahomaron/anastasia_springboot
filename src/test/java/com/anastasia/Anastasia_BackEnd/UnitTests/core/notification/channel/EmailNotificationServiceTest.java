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
import com.anastasia.Anastasia_BackEnd.core.notification.template.TemplateService;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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
                true,
                "noreply@example.com"
        );
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

        emailNotificationService.sendEmail(
                event,
                "Subject",
                com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplateName.NOTIFICATION
        );

        verify(mailSender, never()).send(any(jakarta.mail.internet.MimeMessage.class));
        verify(notificationRepository, never()).save(any());
    }
}
