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
        User user = userRepository.findByUsername(username).orElseThrow(()-> new RuntimeException("User not found"));

        String token = UUID.randomUUID().toString();
        user.setApiToken(token);
        userRepository.save(user);

        return token;
    }

}
