package com.anastasia.Anastasia_BackEnd.UnitTests.service.publiccontact;

import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import com.anastasia.Anastasia_BackEnd.modules.publiccontact.dto.PublicContactRequest;
import com.anastasia.Anastasia_BackEnd.modules.publiccontact.service.PublicContactService;
import com.anastasia.Anastasia_BackEnd.modules.publiccontact.service.TurnstileVerificationService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@LenientMockitoTest
class PublicContactServiceUnitTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TurnstileVerificationService turnstileVerificationService;

    private PublicContactService publicContactService;

    @BeforeEach
    void setUp() {
        publicContactService = new PublicContactService(
                mailSender,
                turnstileVerificationService,
                "noreply@example.com",
                "support@example.com"
        );
    }

    @Test
    void submit_acceptsPdfAttachmentAndSendsMail() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        PublicContactRequest request = validRequest();
        request.setDocument(new MockMultipartFile(
                "document",
                "migration-plan.pdf",
                "application/pdf",
                "plan".getBytes(StandardCharsets.UTF_8)
        ));

        publicContactService.submit(request, "198.51.100.10");

        verify(turnstileVerificationService).verify("turnstile-token", "198.51.100.10");
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void submit_rejectsLegacyOfficeAttachments() {
        PublicContactRequest request = validRequest();
        request.setDocument(new MockMultipartFile(
                "document",
                "contact-request.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "docx".getBytes(StandardCharsets.UTF_8)
        ));

        assertBadRequest(request, "Attachment type is not supported.");
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void submit_rejectsMismatchedAttachmentContentType() {
        PublicContactRequest request = validRequest();
        request.setDocument(new MockMultipartFile(
                "document",
                "contact-request.pdf",
                "image/png",
                "fake".getBytes(StandardCharsets.UTF_8)
        ));

        assertBadRequest(request, "Attachment content type does not match the file extension.");
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void submit_rejectsOversizedAttachment() {
        PublicContactRequest request = validRequest();
        request.setDocument(new MockMultipartFile(
                "document",
                "contact-request.pdf",
                "application/pdf",
                new byte[10 * 1024 * 1024 + 1]
        ));

        assertBadRequest(request, "Attachment exceeds the 10 MB upload limit.");
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    private PublicContactRequest validRequest() {
        PublicContactRequest request = new PublicContactRequest();
        request.setTopic("pricing");
        request.setRequestDescription(
                "This church needs a migration plan and rollout estimate for its current tools and staffing flow."
        );
        request.setEmail("contact@example.com");
        request.setFullName("Abel Kidane");
        request.setChurchName("St. Michael");
        request.setPhone("+12025550123");
        request.setTextPermission(true);
        request.setTurnstileToken("turnstile-token");
        return request;
    }

    private void assertBadRequest(PublicContactRequest request, String reason) {
        assertThatThrownBy(() -> publicContactService.submit(request, "198.51.100.10"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) ex;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(BAD_REQUEST);
                    assertThat(responseStatusException.getReason()).isEqualTo(reason);
                });
    }
}
