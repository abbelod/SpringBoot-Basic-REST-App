package com.example.lecture02.config;


import com.example.lecture02.auth.TokenRequestFilter;
import com.example.lecture02.user.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserService userService;
    private final TokenRequestFilter tokenRequestFilter;

    public SecurityConfig(UserService userService, TokenRequestFilter tokenRequestFilter) {
        this.userService = userService;
        this.tokenRequestFilter = tokenRequestFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null); // Spring Security 6 default handling


        http
//            .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
//                    .csrfTokenRequestHandler(requestHandler))
                .csrf(AbstractHttpConfigurer::disable)
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

        http.addFilterBefore(tokenRequestFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler customSuccessHandler() {
        return (request, response, authentication) -> {
            String username;

            // Check if the user logged in via Google OAuth2
            if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User oauth2User) {
                // Extract the email address from Google to use as their username
                username = oauth2User.getAttribute("email");
            } else {
                // Standard form login username
                username = authentication.getName();
            }

            // Generate your UUID token, save it to the DB, and print it to the screen
            String token = userService.generateToken(username);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"token\": \"" + token + "\"}");
            response.getWriter().flush();
        };
    }
}
