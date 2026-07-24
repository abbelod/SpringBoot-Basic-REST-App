package com.example.lecture02.auth;

import com.example.lecture02.user.User;
import com.example.lecture02.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class DbTokenValidationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public DbTokenValidationFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Check if request was successfully authenticated by Spring Security's OAuth2 Resource Server
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String incomingToken = jwt.getTokenValue();
            String username = jwt.getSubject();

            Optional<User> userOptional = userRepository.findByUsername(username);

            // Verify if user exists in DB AND their stored token matches the incoming JWT
            if (userOptional.isEmpty() || userOptional.get().getApiToken() == null ||
                    !userOptional.get().getApiToken().equals(incomingToken)) {

                // Token revoked, cleared, or replaced in DB -> Reject Request
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Token has been revoked or is invalid.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}