package com.example.lecture02.user;


import com.example.lecture02.auth.JwtService;
import com.example.lecture02.config.PasswordEncoderConfig;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;


@Service
public class UserService {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, UserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    private Boolean userExists(String username) {
        Optional<User> User = userRepository.findByUsername(username);
        return User.isPresent();
    }
    public String generateToken(String username) {
        User user = findOrCreateUser(username);
        userRepository.save(user);
        UserDetails userDetail = userDetailsService.loadUserByUsername(username);

        return jwtService.generateToken(userDetail);
    }
    private User findOrCreateUser(String username) {
        User user = userRepository.findByUsername(username).orElseGet(()-> {
            User newUser = new User();
            newUser.setUsername(username);
            newUser.setRole("ROLE_REPORTER");
            newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            return newUser;
        });
        return user;

    }
}