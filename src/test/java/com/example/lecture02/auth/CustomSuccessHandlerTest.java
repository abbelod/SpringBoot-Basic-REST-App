package com.example.lecture02.auth;
import com.example.lecture02.config.SecurityConfig;
import com.example.lecture02.user.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomSuccessHandlerTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private UserService userService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private Jwt mockJwt;

    private AuthenticationSuccessHandler successHandler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private UserDetails dummyUserDetails;

    @BeforeEach
    void setUp() {
        // Instantiate your configuration class or setup class containing the @Bean
        // Replace 'SecurityConfig' with the name of the class where this @Bean lives
        SecurityConfig config = new SecurityConfig(userDetailsService, userService);
        successHandler = config.customSuccessHandler(jwtEncoder);

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        dummyUserDetails = new User("testuser@example.com", "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void authenticationManager_ShouldReturnNonNullInstance() throws Exception {
        SecurityConfig config = new SecurityConfig(userDetailsService, userService);
        AuthenticationConfiguration cfg = mock(AuthenticationConfiguration.class);
        AuthenticationManager mockAuthManager = mock(AuthenticationManager.class);

        when(cfg.getAuthenticationManager()).thenReturn(mockAuthManager);

        AuthenticationManager result = config.authenticationManager(cfg);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(mockAuthManager);
    }

    @Test
    void jwtDecoder_ShouldReturnNonNullNimbusJwtDecoder() {
        // 1. Arrange: Ensure your secretKey field is initialized
        // (If using Base64, supply a valid Base64-encoded secret key string)
        String testBase64Secret = Base64.getEncoder().encodeToString(
                "mySuperSecretKeyThatIsAtLeast256BitsLongForHmacSha256!".getBytes()
        );

        SecurityConfig securityConfig = new SecurityConfig(userDetailsService, userService);
        // Set the secret key field (via reflection, constructor, or setter)
        ReflectionTestUtils.setField(securityConfig, "secretKey", testBase64Secret);

        // 2. Act
        JwtDecoder decoder = securityConfig.jwtDecoder();

        // 3. Assert: Killing the 'replaced return value with null' mutant
        assertThat(decoder).isNotNull();
    }

    @Test
    @DisplayName("Google OAuth2 login extracts 'email' attribute and returns token")
    void onAuthenticationSuccess_GoogleOAuth2_ShouldReturnJwtToken() throws Exception {
        // Arrange
        OAuth2User oAuth2User = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("email", "googleuser@gmail.com", "sub", "12345"),
                "sub"
        );
        OAuth2AuthenticationToken authToken = new OAuth2AuthenticationToken(oAuth2User, oAuth2User.getAuthorities(), "google");

        when(userDetailsService.loadUserByUsername("googleuser@gmail.com")).thenReturn(dummyUserDetails);
        when(mockJwt.getTokenValue()).thenReturn("mocked.google.jwt");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        // Act
        successHandler.onAuthenticationSuccess(request, response, authToken);

        // Assert
        verify(userService).findOrCreateUser("googleuser@gmail.com");
        verify(userDetailsService).loadUserByUsername("googleuser@gmail.com");

        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");

        Map<String, String> jsonResponse = new ObjectMapper().readValue(
                response.getContentAsString(), new TypeReference<>() {}
        );
        assertThat(jsonResponse).containsEntry("token", "mocked.google.jwt");
    }

    @Test
    @DisplayName("GitHub OAuth2 login extracts 'login' attribute and returns token")
    void onAuthenticationSuccess_GitHubOAuth2_ShouldReturnJwtToken() throws Exception {
        // Arrange
        OAuth2User oAuth2User = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("login", "githubdev", "id", "67890"),
                "login"
        );
        OAuth2AuthenticationToken authToken = new OAuth2AuthenticationToken(oAuth2User, oAuth2User.getAuthorities(), "github");

        when(userDetailsService.loadUserByUsername("githubdev")).thenReturn(dummyUserDetails);
        when(mockJwt.getTokenValue()).thenReturn("mocked.github.jwt");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        // Act
        successHandler.onAuthenticationSuccess(request, response, authToken);

        // Assert
        verify(userService).findOrCreateUser("githubdev");
        assertThat(response.getContentAsString()).contains("mocked.github.jwt");
    }

    @Test
    @DisplayName("Standard Form/Dao Login extracts username from authentication.getName()")
    void onAuthenticationSuccess_StandardAuthentication_ShouldReturnJwtToken() throws Exception {
        // Arrange
        Authentication authToken = mock(Authentication.class);
        when(authToken.getName()).thenReturn("standarduser");

        when(userDetailsService.loadUserByUsername("standarduser")).thenReturn(dummyUserDetails);
        when(mockJwt.getTokenValue()).thenReturn("mocked.standard.jwt");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        // Act
        successHandler.onAuthenticationSuccess(request, response, authToken);

        // Assert
        verify(userService).findOrCreateUser("standarduser");
        assertThat(response.getContentAsString()).contains("mocked.standard.jwt");
    }

    @Test
    @DisplayName("Should throw IllegalStateException when username cannot be resolved")
    void onAuthenticationSuccess_NullUsername_ShouldThrowException() {
        // Arrange
        OAuth2User oAuth2User = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("id", "12345"), // No 'email' or 'login' key present
                "id"
        );
        // Using "google" registrationId, but user lacks 'email' attribute
        OAuth2AuthenticationToken authToken = new OAuth2AuthenticationToken(oAuth2User, oAuth2User.getAuthorities(), "google");

        // Act & Assert
        assertThatThrownBy(() -> successHandler.onAuthenticationSuccess(request, response, authToken))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Could not extract username from OAuth2 principal.");
    }
}