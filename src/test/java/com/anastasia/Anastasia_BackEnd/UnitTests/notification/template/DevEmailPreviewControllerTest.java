package com.anastasia.Anastasia_BackEnd.UnitTests.notification.template;

import com.anastasia.Anastasia_BackEnd.core.notification.controller.DevEmailPreviewController;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplate;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DevEmailPreviewControllerTest {

    private EmailTemplateService emailTemplateService;
    private DevEmailPreviewController controller;

    @BeforeEach
    void setUp() {
        emailTemplateService = mock(EmailTemplateService.class);
        controller = new DevEmailPreviewController(emailTemplateService);
    }

    @Test
    void returnsRenderedHtmlForTemplateKey() {
        when(emailTemplateService.renderHtml(ArgumentMatchers.anyString(), ArgumentMatchers.anyMap()))
                .thenReturn("<html><body>ok</body></html>");

        String html = controller.preview(EmailTemplate.VERIFY_EMAIL_OTP.templateKey()).getBody();

        assertThat(html).contains("ok");
    }
}
