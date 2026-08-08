package com.logistics.companyproductservice.infrastructure.config;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.logistics.common.security.filter.UserContextFilter;
import com.logistics.common.security.hendler.CustomAccessDeniedHandler;
import com.logistics.common.security.hendler.CustomAuthenticationEntryPoint;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        JsonMapper jsonMapper = JsonMapper.builder().build();

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new CustomAuthenticationEntryPoint(jsonMapper))
                        .accessDeniedHandler(new CustomAccessDeniedHandler(jsonMapper)))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/actuator/health").permitAll()
                        // TODO: Company/Product 역할 기반 인가 규칙 미정. 확정되면 교체.
                        .anyRequest().permitAll())
                .addFilterBefore(new UserContextFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}