package com.example.lecture02.config;


import com.example.lecture02.user.User;
import com.example.lecture02.user.UserRepository;
import com.example.lecture02.user.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    private final UserDetailsService userDetailsService;
    private final UserService userService;

    public SecurityConfig(UserDetailsService userDetailsService, UserService userService) {
        this.userDetailsService = userDetailsService;
        this.userService = userService;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        byte[] secretBytes = Base64.getDecoder().decode(secretKey.getBytes());
        SecretKey hmacKey = new SecretKeySpec(secretBytes, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(hmacKey).build();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        byte[] secretBytes = Base64.getDecoder().decode(secretKey.getBytes());

        // 1. Create a Symmetric JWK using OctetSequenceKey
        OctetSequenceKey jwk = new OctetSequenceKey.Builder(secretBytes)
                .algorithm(JWSAlgorithm.HS256)
                .build();

        // 2. Wrap it in a JWKSource
        JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));

        // 3. Return NimbusJwtEncoder
        return new NimbusJwtEncoder(jwks);
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
        // Set prefix to "" if your DB roles already have "ROLE_", otherwise set to "ROLE_"
        grantedAuthoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtEncoder jwtEncoder) throws Exception {

        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null); // Spring Security 6 default handling


        http
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(requestHandler))
//                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/news/**").hasAnyRole("EDITOR", "REPORTER", "READER")
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/news/**").hasAnyRole("EDITOR", "REPORTER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/news/**").hasRole("EDITOR")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .successHandler(customSuccessHandler(jwtEncoder)) // Prints your UUID token on screen
                        .permitAll()
                )
                .oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(customSuccessHandler(jwtEncoder)) // Prints a UUID token for Google users too!
                        .permitAll()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler customSuccessHandler(JwtEncoder jwtEncoder ) {
        return (request, response, authentication) -> {
            String username = null;

            // Check if the user logged in via OAuth2 (Google or GitHub)
            if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
                OAuth2User oauth2User = oauthToken.getPrincipal();
                if (oauth2User != null) {

                String registrationId = oauthToken.getAuthorizedClientRegistrationId(); // "google" or "github"

                if ("google".equalsIgnoreCase(registrationId)) {
                    // Google returns email
                    username = oauth2User.getAttribute("email");
                } else if ("github".equalsIgnoreCase(registrationId)) {
                    // GitHub returns login (username) or email
                    username = oauth2User.getAttribute("login");
                } else {
                    // Fallback for any other provider or default attribute
                    username = oauth2User.getName();
                }
            }
            } else {
                // Standard form-based / DaoAuthenticationProvider username
                username = authentication.getName();
            }

            // Defensive check in case the provider didn't yield a valid string
            if (username == null) {
                throw new IllegalStateException("Could not extract username from OAuth2 principal.");
            }

            userService.findOrCreateUser(username);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            Instant now = Instant.now();

            JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
            JwtClaimsSet claims = JwtClaimsSet.builder()
                    .issuer("self")
                    .issuedAt(now)
                    .expiresAt(now.plusSeconds(3600)) // 1 hour
                    .subject(userDetails.getUsername())
                    .claim("roles", userDetails.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .toList())
                    .build();

            String token = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();

            userService.setApiToken(username, token);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            Map<String, String> tokenResponse = Map.of("token", token);

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(response.getWriter(), tokenResponse);

        };
    }

}
