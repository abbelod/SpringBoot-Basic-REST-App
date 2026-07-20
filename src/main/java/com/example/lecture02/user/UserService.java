package com.example.lecture02.user;


import com.example.lecture02.config.PasswordEncoderConfig;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;


@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String generateToken(String username) {

        User user = userRepository.findByUsername(username)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setUsername(username);
                    newUser.setRole("ROLE_REPORTER");
                    newUser.setPassword(null);
                    newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                    return newUser;
                });

        String token = UUID.randomUUID().toString();
        user.setApiToken(token);
        userRepository.save(user);

        return token;
    }

}