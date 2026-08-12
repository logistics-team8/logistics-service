package com.logistics.notificationservice.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI noticationOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Notification Service API")
                        .description("Slack 메시지 및 AI 알림 API")
                        .version("v1"));
    }
    @Bean
    public OpenApiCustomizer userContextHeaderCustomizer() {

        return openApi -> {

            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths().forEach(
                    (path, pathItem) -> {

                        // 사용자 API에만 X-* 헤더 추가
                        if (!path.startsWith("/api/")) {return;}

                        pathItem.readOperations().forEach(operation -> {

                            operation.addParametersItem(
                                    new Parameter()
                                            .in("header")
                                            .name("X-User-Id")
                                            .description("사용자 UUID")
                                            .required(true)
                                            .example(
                                                    "550e8400-e29b-41d4-a716-446655440000"
                                            )
                            );

                            operation.addParametersItem(
                                    new Parameter()
                                            .in("header")
                                            .name("X-Role")
                                            .description(
                                                    "사용자 권한 (예: MASTER)"
                                            )
                                            .required(true)
                                            .example("MASTER")
                            );
                        });
                    }
            );
        };
    }
}
