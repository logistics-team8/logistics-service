package com.logistics.notificationservice.infrastructure.security;


import com.logistics.common.security.filter.UserContextFilter;
import com.logistics.common.security.hendler.CustomAccessDeniedHandler;
import com.logistics.common.security.hendler.CustomAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
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
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            UserContextFilter userContextFilter,
            CustomAccessDeniedHandler customAccessDeniedHandler,
            CustomAuthenticationEntryPoint customAuthenticationEntryPoint
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

                .exceptionHandling(config ->
                        config.authenticationEntryPoint(customAuthenticationEntryPoint)
                                .accessDeniedHandler(customAccessDeniedHandler))

                .authorizeHttpRequests(auth ->
                        auth
                                .requestMatchers(
                                        "/internal/**",
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

    // Bean 설정
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