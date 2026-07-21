package com.example.lecture02.config;


import com.example.lecture02.auth.JwtAuthenticationFilter;
import com.example.lecture02.auth.JwtService;
import com.example.lecture02.auth.TokenRequestFilter;
import com.example.lecture02.user.User;
import com.example.lecture02.user.UserRepository;
import com.example.lecture02.user.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import java.util.UUID;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserService userService;

    public SecurityConfig(UserService userService, UserDetailsService userDetailsService, JwtAuthenticationFilter jwtAuthenticationFilter, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userService = userService;
    }


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService) throws Exception {

        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null); // Spring Security 6 default handling


        http
            .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(requestHandler))
//                .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET,"/api/v1/news/**").hasAnyRole("EDITOR", "REPORTER", "READER")
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/news/**").hasAnyRole("EDITOR", "REPORTER")
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/news/**").hasRole("EDITOR")
                    .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .successHandler(customSuccessHandler()) // Prints your UUID token on screen
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(customSuccessHandler()) // Prints a UUID token for Google users too!
                        .permitAll()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler customSuccessHandler()  {
        return (request, response, authentication) -> {
            String username;

            // Check if the user logged in via OAuth2 (Google or GitHub)
            if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
                OAuth2User oauth2User = oauthToken.getPrincipal();
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
            } else {
                // Standard form-based / DaoAuthenticationProvider username
                username = authentication.getName();
            }

            // Defensive check in case the provider didn't yield a valid string
            if (username == null) {
                throw new IllegalStateException("Could not extract username from OAuth2 principal.");
            }


            String token = userService.generateToken(username);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"token\": \"" + token + "\"}");
            response.getWriter().flush();
        };
    }
}
