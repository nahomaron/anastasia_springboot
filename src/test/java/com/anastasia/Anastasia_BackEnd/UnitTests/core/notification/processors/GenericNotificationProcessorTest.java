package com.anastasia.Anastasia_BackEnd.UnitTests.core.notification.processors;

import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.InAppNotificationService;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.SmsNotificationService;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.WhatsAppNotificationService;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.sms.service.SmsTemplateType;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationChannelType;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationType;
import com.anastasia.Anastasia_BackEnd.core.notification.processors.GenericNotificationProcessor;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplate;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplateService;
import com.anastasia.Anastasia_BackEnd.modules.registration.common.Address;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class GenericNotificationProcessorTest {

    @Mock
    private EmailTemplateService emailTemplateService;

    @Mock
    private InAppNotificationService inAppNotificationService;

    @Mock
    private SmsNotificationService smsNotificationService;

    @Mock
    private WhatsAppNotificationService whatsAppNotificationService;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private ChurchRepository churchRepository;

    private GenericNotificationProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new GenericNotificationProcessor(
                emailTemplateService,
                inAppNotificationService,
                smsNotificationService,
                whatsAppNotificationService,
                tenantRepository,
                churchRepository
        );
    }

    @Test
    void rendersBrandedAdminMemberEmailForEmailNotifications() {
        UUID tenantId = UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.setUuid(UUID.randomUUID());
        user.setFullName("Mariam Salib");
        user.setEmail("member@example.com");
        user.setTenantId(tenantId);

        TenantEntity tenant = TenantEntity.builder()
                .id(tenantId)
                .displayName("St. Raphael Cathedral")
                .build();

        ImageAssetEntity logo = ImageAssetEntity.builder()
                .imageUrl("https://cdn.example.com/church-logo.png")
                .build();

        ChurchEntity church = ChurchEntity.builder()
                .churchName("St. Raphael Cathedral")
                .profilePicture(logo)
                .address(Address.builder()
                        .addressLine1("12 Church Street")
                        .city("Boston")
                        .stateProvince("MA")
                        .country("USA")
                        .build())
                .build();

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(churchRepository.findByTenantId(tenantId)).thenReturn(Optional.of(church));
        when(emailTemplateService.renderHtml(eq(EmailTemplate.ADMIN_MEMBER_NOTIFICATION.templateKey()), any(Map.class)))
                .thenReturn("<html>rendered</html>");
        when(emailTemplateService.renderText(eq(EmailTemplate.ADMIN_MEMBER_NOTIFICATION.templateKey()), any(Map.class)))
                .thenReturn("rendered");

        NotificationEvent event = new NotificationEvent(
                this,
                NotificationType.NOTIFICATION,
                user,
                Map.of(
                        "title", "Liturgy schedule update",
                        "subject", "A message from St. Raphael Cathedral",
                        "message_content", "Please arrive 20 minutes early."
                ),
                EnumSet.of(NotificationChannelType.EMAIL, NotificationChannelType.IN_APP)
        );

        processor.process(event);

        ArgumentCaptor<Map<String, Object>> modelCaptor = ArgumentCaptor.forClass(Map.class);
        verify(emailTemplateService).renderHtml(eq(EmailTemplate.ADMIN_MEMBER_NOTIFICATION.templateKey()), modelCaptor.capture());
        assertThat(modelCaptor.getValue())
                .containsEntry("memberName", "Mariam Salib")
                .containsEntry("messageTitle", "Liturgy schedule update")
                .containsEntry("messageContent", "Please arrive 20 minutes early.")
                .containsEntry("emailSubject", "A message from St. Raphael Cathedral")
                .containsEntry("churchName", "St. Raphael Cathedral")
                .containsEntry("churchLogoUrl", "https://cdn.example.com/church-logo.png")
                .containsEntry("footerAddress", "12 Church Street, Boston, MA, USA");

        verify(emailTemplateService).sendEmail(
                eq("member@example.com"),
                eq("A message from St. Raphael Cathedral"),
                eq("<html>rendered</html>"),
                eq("rendered"),
                any()
        );
        verify(smsNotificationService).send(eq(event), eq(SmsTemplateType.CUSTOM), any(Map.class));
        verify(whatsAppNotificationService).send(eq(event), eq(null), any(Map.class));
        verify(inAppNotificationService).send(event);
    }
}
