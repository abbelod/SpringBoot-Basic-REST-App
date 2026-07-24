package com.example.lecture02.config;


import com.example.lecture02.user.User;
import com.example.lecture02.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DatabaseSeeder {

    @Value("${DEFAULT_PASSWORD}")
     String defaultPassword;


    @Bean
    CommandLineRunner seedUsers(UserRepository userRepository, PasswordEncoder encoder) {
        return args -> {
            if (userRepository.count() == 0) {
                userRepository.save(new User(null, "readerUser", encoder.encode(defaultPassword), "ROLE_READER", null));
                userRepository.save(new User(null, "editorUser", encoder.encode(defaultPassword), "ROLE_EDITOR", null));
                userRepository.save(new User(null, "reporterUser1", encoder.encode(defaultPassword), "ROLE_REPORTER", null));
                userRepository.save(new User(null, "reporterUser2", encoder.encode(defaultPassword), "ROLE_REPORTER", null));
            }
        };
    }

}
