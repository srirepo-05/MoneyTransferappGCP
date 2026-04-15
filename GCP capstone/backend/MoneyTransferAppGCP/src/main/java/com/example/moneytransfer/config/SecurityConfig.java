package com.example.moneytransfer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Stateless HTTP Basic Auth security configuration.
 *
 * <p>Roles:
 * <ul>
 *   <li>{@code USER}  – can read accounts and execute transfers
 *   <li>{@code ADMIN} – additionally can access actuator / admin endpoints
 * </ul>
 *
 * <p>In a production setup, replace {@link InMemoryUserDetailsManager} with a
 * database-backed {@code UserDetailsService} and switch to JWT.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — REST API uses stateless tokens, not browser sessions
            .csrf(AbstractHttpConfigurer::disable)

            // Stateless — no HTTP session created or used
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                    // Health / info endpoints are public
                    .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                    // All transfer and account endpoints require authentication
                    .requestMatchers(HttpMethod.POST, "/api/v1/transfers").hasAnyRole("USER", "ADMIN")
                    .requestMatchers(HttpMethod.GET,  "/api/v1/accounts/**").hasAnyRole("USER", "ADMIN")

                    // Analytics endpoints — ADMIN only (BigQuery data)
                    .requestMatchers(HttpMethod.GET, "/api/v1/analytics/**").hasRole("ADMIN")

                    // Any other request must be authenticated
                    .anyRequest().authenticated()
            )

            // Use HTTP Basic for simplicity (swap with JWT filter in Module 3)
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * In-memory users for development.
     * Credentials: user / password  and  admin / admin
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        UserDetails user = User.builder()
                .username("user")
                .password(encoder.encode("password"))
                .roles("USER")
                .build();

        UserDetails admin = User.builder()
                .username("admin")
                .password(encoder.encode("admin"))
                .roles("USER", "ADMIN")
                .build();

        return new InMemoryUserDetailsManager(user, admin);
    }
}
