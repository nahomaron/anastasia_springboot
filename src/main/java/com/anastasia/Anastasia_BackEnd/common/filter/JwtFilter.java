package com.anastasia.Anastasia_BackEnd.common.filter;

import com.anastasia.Anastasia_BackEnd.core.auth.repository.TokenRepository;
import com.anastasia.Anastasia_BackEnd.common.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
/*
JwtFilter is a Spring Security filter that intercepts HTTP requests to:

1 - Extract and validate JWT tokens from the Authorization header.
2 - Authenticate the user using UserDetailsService and TokenRepository.
3 - Set the security context if the token is valid and active (not expired or revoked).

This filter ensures that only authenticated users with valid tokens can access protected endpoints,
and it gracefully handles invalid or missing tokens by returning a JSON error response.

 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    private final JwtUtil jwtUtil;

    @Lazy
    private final UserDetailsService userDetailsService;
    private final TokenRepository tokenRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        String token = null;
        String username = null;

        if (authHeader == null || !authHeader.toLowerCase().startsWith("bearer ")) {
            filterChain.doFilter(request, response);
            return; // Exit early
        }
        if (authHeader.startsWith("Bearer ")){
            token = authHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(token);
            } catch (Exception e) {
                sendErrorResponse(response, "Invalid or expired token", HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
//
//        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null){
//
//            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
//
//        var isTokenStillValid = tokenRepository.findTopByTokenOrderByIdDesc(token)
//                    .map(t -> !t.isExpired() && !t.isRevoked()).orElse(false);
//
//
//            if(jwtUtil.isTokenValid(token, userDetails) && isTokenStillValid){
//                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
//                        userDetails, null, userDetails.getAuthorities()
//                );
//                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
//            }
//        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                var isTokenStillValid = tokenRepository.findTopByTokenOrderByIdDesc(token)
                        .map(t -> !t.isExpired() && !t.isRevoked()).orElse(false);

                if (jwtUtil.isTokenValid(token, userDetails) && isTokenStillValid) {
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            } catch (UsernameNotFoundException e) {
                // Log a quiet, one-line warning instead of a massive error
                log.warn("Security Exception: User '{}' not found in JwtFilter", username);
                sendErrorResponse(response, "User not found or account disabled", HttpServletResponse.SC_UNAUTHORIZED);
                return; // Stop the filter chain here
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Utility method to send JSON error response.
     */
    private void sendErrorResponse(HttpServletResponse response, String message, int statusCode) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(statusCode);
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}
