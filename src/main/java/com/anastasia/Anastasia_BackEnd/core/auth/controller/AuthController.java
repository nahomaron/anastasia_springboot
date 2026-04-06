package com.anastasia.Anastasia_BackEnd.core.auth.controller;

import com.anastasia.Anastasia_BackEnd.common.utils.RateLimiterService;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.ResetPasswordRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.VerifyLoginTwoFactorRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.service.OAuthLoginTicketService;
import com.anastasia.Anastasia_BackEnd.core.auth.service.RefreshTokenCookieService;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserDTO;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.core.auth.service.AuthService;
import com.anastasia.Anastasia_BackEnd.modules.users.service.UserService;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final OAuthLoginTicketService oauthLoginTicketService;
    private final RefreshTokenCookieService refreshTokenCookieService;
    private final UserService userService;
    private final RateLimiterService rateLimiterService;

    /**
     * Registers a new user in the system.
     * This endpoint is used for user sign-up.
     *
     * @param userDTO The data transfer object containing user details.
     * @return ResponseEntity indicating success or failure of the registration.
     * @throws MessagingException If there's an issue sending the activation email.
     */
    @PostMapping("/sign-up")
    public ResponseEntity<Map<String, String>> signUp(@Valid @RequestBody UserDTO userDTO) throws MessagingException {
        if(!userDTO.isPasswordMatch()){
            return ResponseEntity.badRequest().body(message("Passwords do not match"));
        }
        UserEntity userEntity = userService.convertToEntity(userDTO);

        authService.createUser(userEntity);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(message("Account created successfully. Please check your email to activate your profile."));
    }

    /**
     * Authenticates a user using the provided credentials.
     * This endpoint is used for logging in users.
     *
     * @param request The authentication request containing username and password.
     * @return ResponseEntity containing the authentication response with access token.
     * @throws MessagingException If there's an issue sending the activation email.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @Valid @RequestBody AuthenticationRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) throws MessagingException {
        if (!consumeRateLimit("auth:login", httpRequest, normalizeKeyComponent(request.getEmail()), 10, Duration.ofMinutes(10))) {
            return tooManyAuthenticationRequests();
        }
        return ResponseEntity.ok(withRefreshTokenCookie(authService.authenticate(request), response));
    }

    @GetMapping("/google")
    public void googleLogin(HttpServletResponse response) throws java.io.IOException {
        response.sendRedirect("/oauth2/authorization/google");
    }

    @GetMapping("/oauth/google/exchange")
    public ResponseEntity<AuthenticationResponse> exchangeGoogleLoginTicket(
            @RequestParam String ticket,
            HttpServletResponse response
    ) {
        return ResponseEntity.ok(withRefreshTokenCookie(oauthLoginTicketService.consume(ticket), response));
    }

    @PostMapping("/login/2fa/verify")
    public ResponseEntity<AuthenticationResponse> verifyTwoFactorLogin(
            @Valid @RequestBody VerifyLoginTwoFactorRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        if (!consumeRateLimit(
                "auth:login-2fa",
                httpRequest,
                normalizeKeyComponent(request.getChallengeToken()),
                5,
                Duration.ofMinutes(10))
        ) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(AuthenticationResponse.builder().message("Too many requests, try again later").build());
        }
        return ResponseEntity.ok(withRefreshTokenCookie(authService.verifyLoginTwoFactor(request), response));
    }

    /**
     * Refreshes the access token using the provided refresh token.
     * This endpoint is rate-limited to prevent abuse.
     *
     * @param request  The HTTP request containing the refresh token.
     * @param response The HTTP response to send the new access token.
     * @return ResponseEntity indicating success or failure.
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response){
        String clientIP = request.getRemoteAddr();
        if (rateLimiterService.tryConsume(clientIP, 5, Duration.ofMinutes(1))) {
            AuthenticationResponse authResponse = authService.refreshToken(request);
            return ResponseEntity.ok(withRefreshTokenCookie(authResponse, response));
        }else{
            System.out.println("Rate limit exceeded, returning 429");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(message("Too many requests, try again later"));
        }
    }

    /**
     * Activates a user's account using the provided activation token.
     * This endpoint is called when a user clicks the activation link in their email.
     *
     * @param token The activation token sent to the user's email.
     * @return ResponseEntity indicating success or failure.
     */
    @GetMapping("/activate-account")
    public ResponseEntity<?> confirm(@RequestParam String token, HttpServletResponse response) {
        long start = System.currentTimeMillis();
        try {
            AuthenticationResponse authResponse = withRefreshTokenCookie(authService.activateAccount(token), response);
            log.info("Activation took: {} ms", System.currentTimeMillis() - start);
            return ResponseEntity.ok(authResponse);
        } catch (RuntimeException ex) {
            log.warn("Activation failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message(ex.getMessage()));
        }
    }

    /**
     * Resends the activation email to the user.
     * This is useful if the user did not receive the activation email or it has expired.
     *
     * @param email The email address of the user to resend the activation email.
     * @return ResponseEntity indicating success or failure.
     */
    @PostMapping("/resend-activation")
    public ResponseEntity<Map<String, String>> resendActivation(@RequestParam String email) {
        if (!consumeRateLimit("auth:resend-activation", null, normalizeKeyComponent(email), 3, Duration.ofMinutes(15))) {
            return tooManyRequests();
        }
        try {
            authService.resendActivationEmail(email);
            return ResponseEntity.ok(message("Activation email resent successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message(e.getMessage()));
        } catch (MessagingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(message("Failed to send activation email"));
        }
    }

    /**
     * Initiates the "Forgot Password" flow.
     * Sends a password reset email to the provided email address.
     *
     * @param request A map containing the user's email (e.g., {"email": "user@example.com"}).
     * @return ResponseEntity indicating success or failure.
     * @throws MessagingException If there's an issue sending the email.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest
    ) throws MessagingException {
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(message("Email is required for password reset."));
        }
        if (!consumeRateLimit("auth:forgot-password", httpRequest, normalizeKeyComponent(email), 3, Duration.ofMinutes(15))) {
            return tooManyRequests();
        }
        authService.initiatePasswordReset(email);
        return ResponseEntity.ok(message("If an account exists with that email, a password reset link has been sent."));
    }

    /**
     * Completes the "Forgot Password" flow by allowing the user to set a new password.
     * Requires the reset token received via email and the new password.
     *
     * @param request A DTO containing the reset token, new password, and confirmation.
     * @return ResponseEntity indicating success or failure.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        if(!request.isPasswordMatch()){
            return ResponseEntity.badRequest().body(message("Passwords do not match"));
        }
        if (!consumeRateLimit("auth:reset-password", httpRequest, normalizeKeyComponent(request.getToken()), 5, Duration.ofMinutes(15))) {
            return tooManyRequests();
        }
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(message("Your password has been successfully reset. You can now log in with your new password."));
    }

    /**
     * Checks if the provided email is already registered in the system.
     * This endpoint is used to verify if a user can sign up with a given email.
     *
     * @param email The email address to check for registration.
     * @return ResponseEntity indicating whether the email is registered or not.
     */
    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestParam String email) {
        boolean isRegistered = authService.isEmailRegistered(email);
        Map<String, Object> response = new HashMap<>();
        response.put("message", isRegistered ? "Email is already registered." : "Email is available for registration.");
        response.put("registered", isRegistered);
        return ResponseEntity.ok(response);
    }

    private Map<String, String> message(String value) {
        return Map.of("message", value);
    }

    private ResponseEntity<Map<String, String>> tooManyRequests() {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(message("Too many requests, try again later"));
    }

    private ResponseEntity<AuthenticationResponse> tooManyAuthenticationRequests() {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(AuthenticationResponse.builder()
                        .message("Too many requests, try again later")
                        .build());
    }

    private boolean consumeRateLimit(
            String scope,
            HttpServletRequest request,
            String subject,
            long capacity,
            Duration period
    ) {
        String clientIp = request != null ? normalizeKeyComponent(request.getRemoteAddr()) : "n/a";
        String effectiveSubject = subject == null || subject.isBlank() ? "anonymous" : subject;
        String bucketKey = scope + ":" + clientIp + ":" + effectiveSubject;
        return rateLimiterService.tryConsume(bucketKey, capacity, period);
    }

    private String normalizeKeyComponent(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private AuthenticationResponse withRefreshTokenCookie(
            AuthenticationResponse authResponse,
            HttpServletResponse response
    ) {
        String refreshToken = authResponse.getRefreshToken();
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenCookieService.addRefreshTokenCookie(response, refreshToken);
            authResponse.setRefreshToken(null);
        }
        return authResponse;
    }
}
