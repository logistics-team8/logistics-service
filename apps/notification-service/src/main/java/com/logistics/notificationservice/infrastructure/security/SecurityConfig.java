package com.logistics.notificationservice.infrastructure.security;


import com.logistics.common.security.filter.UserContextFilter;
import com.logistics.common.security.hendler.CustomAccessDeniedHandler;
import com.logistics.common.security.hendler.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.json.JsonMapper;
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain internalSecurityFilterChain(
            HttpSecurity http,
            ServiceTokenFilter serviceTokenFilter
    ) throws Exception {

        http
                .securityMatcher("/internal/**")

                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .addFilterBefore(
                        serviceTokenFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .authorizeHttpRequests(auth ->
                        auth
                                .anyRequest()
                                .hasRole("SERVICE")
                );

        return http.build();
    }


    @Bean
    @Order(2)
    public SecurityFilterChain userSecurityFilterChain(
            HttpSecurity http,
            UserContextFilter userContextFilter
    ) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .addFilterBefore(
                        userContextFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .authorizeHttpRequests(auth ->
                        auth
                                .requestMatchers(
                                        "/swagger-ui/**",
                                        "/v3/api-docs/**",
                                        "/actuator/health"
                                )
                                .permitAll()

                                .anyRequest()
                                .authenticated()
                );

        return http.build();
    }

    @Bean
    public UserContextFilter userContextFilter() {
        return new UserContextFilter();
    }
}