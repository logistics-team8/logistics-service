package com.logistics.hubservice.infrastructure.security;

import com.logistics.common.security.filter.UserContextFilter;
import com.logistics.common.security.hendler.CustomAccessDeniedHandler;
import com.logistics.common.security.hendler.CustomAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
                        .requestMatchers(HttpMethod.POST, "/api/v1/hub-routes").hasRole("MASTER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/hubs", "/api/v1/hubs/**").hasRole("MASTER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/hubs", "/api/v1/hubs/**")
                        .hasAnyRole("MASTER", "HUB_MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/hubs", "/api/v1/hubs/**").hasRole("MASTER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/hubs", "/api/v1/hubs/**").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(new UserContextFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
