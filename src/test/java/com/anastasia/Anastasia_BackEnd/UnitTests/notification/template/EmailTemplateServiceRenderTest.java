package com.anastasia.Anastasia_BackEnd.UnitTests.notification.template;

import com.anastasia.Anastasia_BackEnd.core.notification.channel.EmailNotificationService;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplate;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplateService;
import com.anastasia.Anastasia_BackEnd.core.notification.template.TemplateRenderer;
import com.anastasia.Anastasia_BackEnd.core.notification.template.TemplateResolution;
import com.anastasia.Anastasia_BackEnd.core.notification.template.TemplateResolver;
import com.anastasia.Anastasia_BackEnd.core.notification.template.TemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplateServiceRenderTest {

    private EmailTemplateService emailTemplateService;

    @BeforeEach
    void setUp() {
        SpringTemplateEngine engine = new SpringTemplateEngine();

        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setTemplateMode("HTML");
        resolver.setCheckExistence(true);
        resolver.setOrder(1);
        engine.addTemplateResolver(resolver);

        TemplateResolver templateResolver = Mockito.mock(TemplateResolver.class);
        Mockito.when(templateResolver.resolve(Mockito.anyString()))
                .thenAnswer(invocation -> TemplateResolution.classpath(invocation.getArgument(0)));

        TemplateService templateService = new TemplateService(templateResolver, new TemplateRenderer(engine));

        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("messages");
        messageSource.setDefaultEncoding("UTF-8");

        emailTemplateService = new EmailTemplateService(
                templateService,
                messageSource,
                Mockito.mock(EmailNotificationService.class),
                "Anastasia",
                "St. Raphael Cathedral",
                "https://example.com/logo.png",
                "https://example.com/church-logo.png",
                "support@example.com",
                "12 Church St",
                "https://app.anastasia.com",
                "https://app.anastasia.com/help",
                "en-US"
        );
    }

    @Test
    void rendersEveryTemplateWithRequiredModelAndNoUnresolvedPlaceholders() {
        for (EmailTemplate template : EmailTemplate.values()) {
            Map<String, Object> model = minimalModel(template);
            String html = emailTemplateService.renderHtml(template.templateKey(), model);
            String text = emailTemplateService.renderText(template.templateKey(), model);

            assertThat(html)
                    .contains("support@example.com")
                    .doesNotContain("${")
                    .doesNotContain("th:text")
                    .doesNotContain("th:replace");

            assertThat(text)
                    .doesNotContain("${")
                    .doesNotContain("th:text");

            if (html.contains("button does not work")) {
                assertThat(html).contains("If the button does not work, use this link");
            }
        }
    }

    private Map<String, Object> minimalModel(EmailTemplate template) {
        Map<String, Object> model = new HashMap<>();
        model.put("userName", "Mariam");
        model.put("memberName", "Mariam Salib");
        model.put("ownerName", "Fr. Daniel");
        model.put("formName", "Baptism Request");
        model.put("churchName", "St. Raphael Cathedral");
        model.put("loginUrl", "https://app.anastasia.com/login");
        model.put("supportEmail", "support@example.com");
        model.put("code", "123456");
        model.put("expiresMinutes", 10);
        model.put("helpUrl", "https://app.anastasia.com/help");
        model.put("verifyUrl", "https://app.anastasia.com/verify");
        model.put("expiresHours", 24);
        model.put("resetUrl", "https://app.anastasia.com/reset");
        model.put("requestIp", "203.0.113.2");
        model.put("requestDevice", "Safari on iPhone");
        model.put("dashboardUrl", "https://app.anastasia.com/dashboard");
        model.put("nextStepsUrl", "https://app.anastasia.com/onboarding");
        model.put("memberEmail", "member@example.com");
        model.put("memberPhone", "+1 202-555-0199");
        model.put("memberId", "M-1001");
        model.put("reviewUrl", "https://app.anastasia.com/admin/member/M-1001");
        model.put("portalUrl", "https://app.anastasia.com/portal");
        model.put("eventTitle", "Community Gathering");
        model.put("date", "2026-03-08");
        model.put("time", "18:00");
        model.put("location", "Parish Hall");
        model.put("eventUrl", "https://app.anastasia.com/events/1");
        model.put("icsUrl", "https://app.anastasia.com/events/1.ics");
        model.put("rsvpUrl", "https://app.anastasia.com/events/1/rsvp");
        model.put("requestedTimes", "2026-03-09 10:00, 2026-03-10 16:00");
        model.put("purpose", "Counseling");
        model.put("memberContact", "member@example.com");
        model.put("approveUrl", "https://app.anastasia.com/appointments/1/approve");
        model.put("declineUrl", "https://app.anastasia.com/appointments/1/decline");
        model.put("dateTime", "2026-03-09T10:00");
        model.put("locationOrMeetingLink", "Room 2");
        model.put("cancelUrl", "https://app.anastasia.com/appointments/1/cancel");
        model.put("rescheduleUrl", "https://app.anastasia.com/appointments/1/reschedule");
        model.put("submittedAt", "2026-03-05T09:00");
        model.put("trackingId", "FRM-1");
        model.put("approvedAt", "2026-03-05T10:00");
        model.put("newMembersCount", 5);
        model.put("pendingFormsCount", 3);
        model.put("upcomingEventsCount", 4);

        return model;
    }
}
