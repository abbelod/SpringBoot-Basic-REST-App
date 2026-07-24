package com.example.lecture02.auth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private Jwt mockJwt;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(jwtEncoder);
    }

    @Test
    void generateToken_ShouldReturnValidTokenValue_AndSetCorrectClaims() {
        // Arrange
        String username = "testuser";
        String expectedTokenValue = "mocked.jwt.token";

        UserDetails userDetails = new User(username, "password", Collections.emptyList());

        when(mockJwt.getTokenValue()).thenReturn(expectedTokenValue);

        // FIX: Match any instance of JwtEncoderParameters cleanly
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        // Act
        String token = jwtService.generateToken(userDetails);

        // Assert
        assertThat(token).isEqualTo(expectedTokenValue);

        // Capture parameters passed to JwtEncoder to verify claims
        ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());

        JwtEncoderParameters parameters = captor.getValue();

        // Check expiration is roughly 60 minutes after issuedAt
        Instant issuedAt = parameters.getClaims().getIssuedAt();
        Instant expiresAt = parameters.getClaims().getExpiresAt();
        assertThat(expiresAt).isEqualTo(issuedAt.plusSeconds(3600));
    }
}