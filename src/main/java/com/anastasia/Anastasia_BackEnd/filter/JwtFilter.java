package com.anastasia.Anastasia_BackEnd.filter;

import com.anastasia.Anastasia_BackEnd.repository.auth.TokenRepository;
import com.anastasia.Anastasia_BackEnd.service.auth.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
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
                username = jwtService.extractUsername(token);
            } catch (Exception e) {
                sendErrorResponse(response, "Invalid or expired token", HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null){

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            var isTokenStillValid = tokenRepository.findByToken(token)
                    .map(t -> !t.isExpired() && !t.isRevoked()).orElse(false);


            if(jwtService.isTokenValid(token, userDetails) && isTokenStillValid){
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
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
