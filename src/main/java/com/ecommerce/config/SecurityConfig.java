package com.ecommerce.config;

import com.ecommerce.security.JwtAuthenticationEntryPoint;
import com.ecommerce.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configure(http))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - ANYONE can access
                        .requestMatchers(
                                "/api/admin/register",
                                "/api/admin/login",
                                "/api/admin/confirm/**",
                                "/api/admin/reject/**",
                                "/api/admin/forgot-password",
                                "/api/admin/verify-otp",
                                "/api/admin/reset-password",
                                "/api/user/register",
                                "/api/user/login",
                                "/api/user/verify-otp",
                                "/api/user/forgot-password",
                                "/api/user/verify-reset-otp",
                                "/api/user/reset-password",
                                "/api/items/all",
                                "/api/items/category/**",
                                "/api/items/search",
                                "/api/items/{id}",
                                "/api/reviews/item/**",
                                "/api/contact/submit",
                                "/uploads/**",
                                "/api/uploads/**"
                        ).permitAll()

                        // ADMIN ONLY endpoints - Must have ROLE_ADMIN
                        .requestMatchers(
                                "/api/admin/profile",
                                "/api/admin/dashboard",
                                "/api/items/add",
                                "/api/items/update/**",
                                "/api/items/delete/**",
                                "/api/items/admin",
                                "/api/orders/all",
                                "/api/reviews/all",
                                "/api/reviews/delete/**",
                                "/api/contact/all",
                                "/api/contact/delete/**"
                        ).hasRole("ADMIN")

                        // USER ONLY endpoints - Must have ROLE_USER
                        .requestMatchers(
                                "/api/user/profile",
                                "/api/user/dashboard",
                                "/api/orders/create",
                                "/api/orders/verify",
                                "/api/orders/my-orders",
                                "/api/orders/{id}",
                                "/api/reviews/add"
                        ).hasRole("USER")

                        // Any other request must be authenticated
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}