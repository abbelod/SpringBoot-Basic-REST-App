package com.example.lecture02.user;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByApiToken(String apiToken);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.apiToken = NULL WHERE u.apiToken IS NOT NULL")
    int revokeAllApiTokens();

}