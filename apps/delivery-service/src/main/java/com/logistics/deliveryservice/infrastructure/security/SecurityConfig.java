package com.logistics.deliveryservice.infrastructure.security;

import com.logistics.common.security.filter.UserContextFilter;
import com.logistics.common.security.hendler.CustomAccessDeniedHandler;
import com.logistics.common.security.hendler.CustomAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JsonMapper jsonMapper = JsonMapper.builder().build();

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new CustomAuthenticationEntryPoint(jsonMapper))
                        .accessDeniedHandler(new CustomAccessDeniedHandler(jsonMapper)))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/internal/v1/delivery/managers")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/delivery/managers")
                        .hasAnyRole("MASTER", "HUB_MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/delivery/managers/{userId}")
                        .hasAnyRole("MASTER", "HUB_MANAGER", "DELIVERY_MANAGER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/delivery/managers/{userId}")
                        .hasAnyRole("MASTER", "HUB_MANAGER")
                        .anyRequest().permitAll())
                .addFilterBefore(new UserContextFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
