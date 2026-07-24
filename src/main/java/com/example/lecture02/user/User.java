package com.example.lecture02.user;


import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = true)
    private String password;

    @Column(nullable = false)
    private String role;

    @Column(unique = true)
    private String apiToken;

    public List<SimpleGrantedAuthority> getAuthorities() {
        if (this.role == null) {
            return Collections.emptyList();
        }
        String formattedRole = this.role.startsWith("ROLE_") ? this.role : "ROLE_" + this.role;
        return Collections.singletonList(new SimpleGrantedAuthority(formattedRole));
    }
}