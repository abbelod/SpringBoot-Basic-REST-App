package com.example.lecture02.config;


import com.example.lecture02.auth.TokenAuthenticationSuccessHandler;
import com.example.lecture02.auth.TokenRequestFilter;
import com.example.lecture02.user.User;
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
    private final TokenAuthenticationSuccessHandler successHandler;
    private final TokenRequestFilter tokenRequestFilter;

    public SecurityConfig(TokenAuthenticationSuccessHandler tokenAuthenticationSuccessHandler, TokenRequestFilter tokenRequestFilter, UserService userService) {
        this.tokenRequestFilter = tokenRequestFilter;
        this.successHandler = tokenAuthenticationSuccessHandler;
        this.userService = userService;
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
            .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(requestHandler))
            .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET,"/api/v1/news/**").hasAnyRole("EDITOR", "REPORTER", "READER")
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/news/**").hasAnyRole("EDITOR", "REPORTER")
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/news/**").hasRole("EDITOR")
                    .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .successHandler(customSuccessHandler()) // Defined below
                        .permitAll()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
//                .httpBasic(Customizer.withDefaults());

        http.addFilterBefore(tokenRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler customSuccessHandler() {
        return (request, response, authentication) -> {
            // 1. Generate your token (replace with your actual JWT generation utility)
            String username = authentication.getName();
            String jwtToken = userService.generateToken(username);

            // 2. Set the response type to JSON or plain text
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            // 3. Write the raw token directly to the screen
            response.getWriter().write("{\"token\": \"" + jwtToken + "\"}");
            response.getWriter().flush();
        };
    }
}
