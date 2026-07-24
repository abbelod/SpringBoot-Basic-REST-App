package com.example.lecture02.user;


import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;


@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    public void setApiToken(String username, String token) {
        User user = findOrCreateUser(username);
        user.setApiToken(token);
        userRepository.save(user);
    }

    public User findOrCreateUser(String username) {
        User user = userRepository.findByUsername(username).orElseGet(() -> {
            User newUser = new User();
            newUser.setUsername(username);
            newUser.setRole("ROLE_REPORTER");
            newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            userRepository.save(newUser);
            return newUser;
        });
        return user;

    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void revokeToken() {
        userRepository.findAll().forEach(user -> log.info("username: {} api: {}", user.getUsername(), user.getApiToken()));
       int revokedCount = userRepository.revokeAllApiTokens();
       log.info("Successfully Revoked API Tokens for {} users",revokedCount);
       userRepository.findAll().forEach(user -> log.info("username: {} api: {}", user.getUsername(), user.getApiToken()));
    }
}