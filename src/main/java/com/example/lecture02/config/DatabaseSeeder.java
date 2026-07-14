package com.example.lecture02.config;


import com.example.lecture02.user.User;
import com.example.lecture02.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DatabaseSeeder {


    @Bean
    CommandLineRunner seedUsers(UserRepository userRepository, PasswordEncoder encoder) {
      return args -> {
          if(userRepository.count()== 0) {
              userRepository.save(new User(null, "readerUser", encoder.encode("pass"), "ROLE_READER"));
              userRepository.save(new User(null, "editorUser", encoder.encode("pass"), "ROLE_EDITOR"));
              userRepository.save(new User(null, "reporterUser1", encoder.encode("pass"), "ROLE_REPORTER"));
              userRepository.save(new User(null, "reporterUser2", encoder.encode("pass"), "ROLE_REPORTER"));
          }
      };
    }

}
