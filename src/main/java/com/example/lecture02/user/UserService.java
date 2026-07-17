package com.example.lecture02.user;


import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String generateToken(String username) {

        User user = userRepository.findByUsername(username)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setUsername(username);
                    newUser.setRole("ROLE_REPORTER");
                    newUser.setPassword(null);
                    return newUser;
                });

        String token = UUID.randomUUID().toString();
        user.setApiToken(token);
        userRepository.save(user);

        return token;
    }

}