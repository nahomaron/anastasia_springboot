package com.anastasia.Anastasia_BackEnd.UnitTests.controller;

import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import com.anastasia.Anastasia_BackEnd.common.utils.RateLimiterService;
import com.anastasia.Anastasia_BackEnd.modules.publiccontact.controller.PublicContactController;
import com.anastasia.Anastasia_BackEnd.modules.publiccontact.dto.PublicContactRequest;
import com.anastasia.Anastasia_BackEnd.modules.publiccontact.service.PublicContactService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class PublicContactControllerUnitTest {

    @Mock
    private PublicContactService publicContactService;

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private PublicContactController controller;

    @Test
    void submit_usesRemoteAddrForRateLimitingAndInvokesService() {
        PublicContactRequest request = new PublicContactRequest();
        request.setEmail("Contact@Example.com");
        request.setTopic("pricing");

        when(httpServletRequest.getRemoteAddr()).thenReturn("198.51.100.10");
        when(rateLimiterService.tryConsume(
                "public:contact:198.51.100.10:contact@example.com",
                5L,
                Duration.ofMinutes(15)
        )).thenReturn(true);

        var response = controller.submit(request, httpServletRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("message", "Contact request submitted successfully.");
        verify(publicContactService).submit(request, "198.51.100.10");
    }

    @Test
    void submit_returnsTooManyRequestsWhenRateLimitFails() {
        PublicContactRequest request = new PublicContactRequest();
        request.setEmail("contact@example.com");
        request.setTopic("pricing");

        when(httpServletRequest.getRemoteAddr()).thenReturn("198.51.100.10");
        when(rateLimiterService.tryConsume(
                "public:contact:198.51.100.10:contact@example.com",
                5L,
                Duration.ofMinutes(15)
        )).thenReturn(false);

        var response = controller.submit(request, httpServletRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        verify(publicContactService, never()).submit(request, "198.51.100.10");
    }
}
