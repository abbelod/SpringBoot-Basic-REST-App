package com.example.lecture02.user;


import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    public User findOrCreateUser(String username) {
        User user = userRepository.findByUsername(username).orElseGet(()-> {
            User newUser = new User();
            newUser.setUsername(username);
            newUser.setRole("ROLE_REPORTER");
            newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            userRepository.save(newUser);
            return newUser;
        });
        return user;

    }
}