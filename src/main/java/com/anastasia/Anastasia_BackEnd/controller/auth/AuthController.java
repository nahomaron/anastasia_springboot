package com.anastasia.Anastasia_BackEnd.controller.auth;

import com.anastasia.Anastasia_BackEnd.model.DTO.auth.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.model.DTO.auth.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.model.DTO.auth.UserDTO;
import com.anastasia.Anastasia_BackEnd.model.entity.auth.UserEntity;
import com.anastasia.Anastasia_BackEnd.service.interfaces.UserServices;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.github.bucket4j.local.LocalBucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
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
    public ResponseEntity<AuthenticationResponse> signUp(@RequestBody UserDTO userDTO){
        UserEntity userEntity = userServices.convertToEntity(userDTO);
        return new ResponseEntity<>(userServices.createUser(userEntity), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody AuthenticationRequest request){
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
}
