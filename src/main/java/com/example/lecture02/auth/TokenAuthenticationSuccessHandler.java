package com.example.lecture02.auth;

import com.example.lecture02.user.User;
import com.example.lecture02.user.UserRepository;
import com.sun.jdi.ObjectCollectedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class TokenAuthenticationSuccessHandler implements AuthenticationSuccessHandler {


    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TokenAuthenticationSuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {


        String username = authentication.getName();

        User user = userRepository.findByUsername(username).orElseThrow(()-> new RuntimeException("User not found"));

        String token = UUID.randomUUID().toString();

        user.setApiToken(token);
        userRepository.save(user);

        Map<String, String> tokenResponse = new HashMap<>();
        tokenResponse.put("token", token);
        tokenResponse.put("type", "Bearer");

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), tokenResponse);

    }

}