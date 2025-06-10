package com.anastasia.Anastasia_BackEnd.controller;

import com.anastasia.Anastasia_BackEnd.config.RateLimiterConfig;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.model.auth.ResetPasswordRequest;
import com.anastasia.Anastasia_BackEnd.model.user.UserDTO;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.service.auth.AuthService;
import com.anastasia.Anastasia_BackEnd.service.auth.user.UserService;
import io.github.bucket4j.Bucket;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final RateLimiterConfig rateLimiterConfig;

    /**
     * Registers a new user in the system.
     * This endpoint is used for user sign-up.
     *
     * @param userDTO The data transfer object containing user details.
     * @return ResponseEntity indicating success or failure of the registration.
     * @throws MessagingException If there's an issue sending the activation email.
     */
    @PostMapping("/sign-up")
    public ResponseEntity<?> signUp(@Valid @RequestBody UserDTO userDTO) throws MessagingException {
        if(!userDTO.isPasswordMatch()){
            return ResponseEntity.badRequest().body("Password do not match");
        }
        UserEntity userEntity = userService.convertToEntity(userDTO);
        authService.createUser(userEntity);
        return new ResponseEntity<>(HttpStatus.CREATED);
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
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody AuthenticationRequest request) throws MessagingException {
        return ResponseEntity.ok(authService.authenticate(request));
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
        Bucket bucket = rateLimiterConfig.getBucket(clientIP);

        if(bucket.tryConsume(1)){
            authService.refreshToken(request, response);
            return ResponseEntity.ok().build();
        }else{
            System.out.println("Rate limit exceeded, returning 429");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Too many requests, try again later");
        }
    }

    /**
     * Activates a user's account using the provided activation token.
     * This endpoint is called when a user clicks the activation link in their email.
     *
     * @param token The activation token sent to the user's email.
     * @throws MessagingException If there's an issue sending the activation email.
     */
    @GetMapping("/activate-account")
    public void confirm(@RequestParam String token) throws MessagingException {
        authService.activateAccount(token);
    }

    /**
     * Resends the activation email to the user.
     * This is useful if the user did not receive the activation email or it has expired.
     *
     * @param email The email address of the user to resend the activation email.
     * @return ResponseEntity indicating success or failure.
     */
    @PostMapping("/resend-activation")
    public ResponseEntity<String> resendActivation(@RequestParam String email) {
        try {
            authService.resendActivationEmail(email);
            return ResponseEntity.ok("Activation email resent successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (MessagingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send activation email");
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
    public ResponseEntity<String> forgotPassword(@RequestBody Map<String, String> request) throws MessagingException {
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            return new ResponseEntity<>("Email is required for password reset.", HttpStatus.BAD_REQUEST);
        }
        authService.initiatePasswordReset(email);
        return ResponseEntity.ok("If an account exists with that email, a password reset link has been sent.");
    }

    /**
     * Completes the "Forgot Password" flow by allowing the user to set a new password.
     * Requires the reset token received via email and the new password.
     *
     * @param request A DTO containing the reset token, new password, and confirmation.
     * @return ResponseEntity indicating success or failure.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        if(!request.isPasswordMatch()){
            return ResponseEntity.badRequest().body("Password do not match");
        }
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok("Your password has been successfully reset. You can now log in with your new password.");
    }

    /**
     * Checks if the provided email is already registered in the system.
     * This endpoint is used to verify if a user can sign up with a given email.
     *
     * @param email The email address to check for registration.
     * @return ResponseEntity indicating whether the email is registered or not.
     */
    @GetMapping("/check-email")
    public ResponseEntity<String> checkEmail(@RequestParam String email) {
        boolean isRegistered = authService.isEmailRegistered(email);
        if (isRegistered) {
            return ResponseEntity.ok("Email is already registered.");
        } else {
            return ResponseEntity.ok("Email is available for registration.");
        }
    }







}
