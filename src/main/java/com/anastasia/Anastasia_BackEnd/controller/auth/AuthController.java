package com.anastasia.Anastasia_BackEnd.controller.auth;

import com.anastasia.Anastasia_BackEnd.model.DTO.auth.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.model.DTO.auth.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.model.DTO.auth.UserDTO;
import com.anastasia.Anastasia_BackEnd.model.entity.auth.UserEntity;
import com.anastasia.Anastasia_BackEnd.service.auth.UserServices;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.github.bucket4j.local.LocalBucket;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserServices userServices;

    // Enabling any user to sign up and create account in anastasia app
    // here user mapper is used to map between UserEntity and UserDTO
    // Finally return the userDTO with http status of OK
    @PostMapping("/sign-up")
    public ResponseEntity<?> signUp(@Valid @RequestBody UserDTO userDTO) throws MessagingException {
        if(!userDTO.isPasswordMatch()){
            return ResponseEntity.badRequest().body("Password do not match");
        }
        UserEntity userEntity = userServices.convertToEntity(userDTO);
        userServices.createUser(userEntity);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody AuthenticationRequest request) throws MessagingException {
        return ResponseEntity.ok(userServices.authenticate(request));
    }

    private final ConcurrentMap<String, LocalBucket> buckets = new ConcurrentHashMap<>();
    private Bucket getBucket(String key) {
        return buckets.computeIfAbsent(key, k -> Bucket.builder()
                .addLimit(Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1)))) // 5 requests per minute
                .build());
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response){
        String clientIP = request.getRemoteAddr();
        Bucket bucket = getBucket(clientIP);

        if(bucket.tryConsume(1)){
            userServices.refreshToken(request, response);
            return ResponseEntity.ok().build();
        }else{
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Too many requests, try again later");
        }
    }

    @GetMapping("/info")
    public Map<String, Object> getUserInfo(@AuthenticationPrincipal OAuth2User principal) {
        return principal.getAttributes();
    }

    @GetMapping("/dashboard")
    public String getDashboard(){
        return "bravo! You are logged in";
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> listOfUsers(){
        List<UserDTO> users = userServices.findAllUsers().stream().map(userServices::convertToDTO).toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{userid}")
    public ResponseEntity<UserDTO> getUser(@PathVariable UUID userId){
        Optional<UserEntity> foundUser = userServices.findOne(userId);
        return foundUser.map(userEntity -> {
            UserDTO userDTO = userServices.convertToDTO(userEntity);
            return ResponseEntity.ok(userDTO);
        }).orElse(
                new ResponseEntity<>(HttpStatus.NOT_FOUND)
        );
    }

    @GetMapping("/activate-account")
    public void confirm(@RequestParam String token) throws MessagingException {
        userServices.activateAccount(token);
    }





}
