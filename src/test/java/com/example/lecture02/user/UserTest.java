package com.example.lecture02.user;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void getAuthorities_WhenRoleIsNull_ShouldReturnEmptyList() {
        User user = new User();
        user.setRole(null);

        List<SimpleGrantedAuthority> authorities = user.getAuthorities();

        assertThat(authorities).isEmpty();
    }

    @Test
    void getAuthorities_WhenRoleDoesNotStartWithRole_ShouldPrependPrefix() {
        User user = new User();
        user.setRole("READER");

        List<SimpleGrantedAuthority> authorities = user.getAuthorities();

        assertThat(authorities).hasSize(1);
        assertThat(authorities.get(0).getAuthority()).isEqualTo("ROLE_READER");
    }

    @Test
    void getAuthorities_WhenRoleAlreadyStartsWithRole_ShouldKeepRoleAsIs() {
        User user = new User();
        user.setRole("ROLE_EDITOR");

        List<SimpleGrantedAuthority> authorities = user.getAuthorities();

        assertThat(authorities).hasSize(1);
        assertThat(authorities.get(0).getAuthority()).isEqualTo("ROLE_EDITOR");
    }
}