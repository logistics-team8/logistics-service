package com.logistics.userservice.infrastructure.security;

import com.logistics.common.security.filter.UserContextFilter;
import com.logistics.common.security.hendler.CustomAccessDeniedHandler;
import com.logistics.common.security.hendler.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
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
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity httpSecurity,
            UserContextFilter userContextFilter,
            AccessDeniedHandler accessDeniedHandler,
            CustomAuthenticationEntryPoint customAuthenticationEntryPoint) {
        // csrf 비활성화
        httpSecurity.csrf(AbstractHttpConfigurer::disable);

        // 세션 STATELESS
        httpSecurity.sessionManagement(
                sessionManagement ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 필터 관리
        httpSecurity.addFilterBefore(userContextFilter, UsernamePasswordAuthenticationFilter.class);

        // 예외 핸들러 설정
        httpSecurity.exceptionHandling(
                config ->
                        config.authenticationEntryPoint(customAuthenticationEntryPoint)
                                .accessDeniedHandler(accessDeniedHandler));

        // URL 인가 설정
        httpSecurity.authorizeHttpRequests(
                (requests) ->
                        // Swagger
                        requests.requestMatchers("/v3/api-docs/**", "/swagger-ui/**")
                                .permitAll()

                                // 회원가입, 로그인
                                .requestMatchers(
                                        HttpMethod.POST, "/api/v1/users", "/api/v1/auth/login")
                                .permitAll()

                                // 내부 API 허용
                                .requestMatchers("/internal/**")
                                .permitAll()

                                .anyRequest()
                                .authenticated());

        return httpSecurity.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {

        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CustomAccessDeniedHandler customAccessDeniedHandler(JsonMapper jsonMapper) {
        return new CustomAccessDeniedHandler(jsonMapper);
    }

    @Bean
    public CustomAuthenticationEntryPoint customAuthenticationEntryPoint(JsonMapper jsonMapper) {
        return new CustomAuthenticationEntryPoint(jsonMapper);
    }

    @Bean
    public UserContextFilter userContextFilter() {
        return new UserContextFilter();
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy(
                """
                ROLE_MASTER > ROLE_HUB_MANAGER
                ROLE_HUB_MANAGER > ROLE_COMPANY_MANAGER
                ROLE_HUB_MANAGER > ROLE_DELIVERY_MANAGER
                """);
    }
}
