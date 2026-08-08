package com.logistics.gateway.presentation.exception;

import com.logistics.gateway.config.TestFilterConfig;
import com.logistics.gateway.config.TestRouteConfig;
import com.logistics.gateway.presentation.error.GatewayErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@Import({TestFilterConfig.class, TestRouteConfig.class})
@DisplayName("게이트웨이 예외 처리 테스트")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.cloud.config.enabled=false",
            "eureka.client.register-with-eureka=false",
            "eureka.client.fetch-registry=false",
            "path.whitelist[0]=/business-exception",
            "path.whitelist[1]=/not-found",
            "path.whitelist[2]=/bad-request",
            "path.whitelist[3]=/internal-server-error"
        })
class GatewayExceptionHandlerTest {

    private WebTestClient webTestClient;

    @LocalServerPort int port;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void handle_businessException() {
        // when & then
        webTestClient
                .get()
                .uri("/business-exception")
                .exchange()
                .expectStatus()
                .isEqualTo(GatewayErrorCode.TOKEN_EXPIRED.status())
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.error.code")
                .isEqualTo(GatewayErrorCode.TOKEN_EXPIRED.code());
    }

    @Test
    void handle_responseStatusException_notFound() {
        // when & then
        webTestClient
                .get()
                .uri("/not-found")
                .exchange()
                .expectStatus()
                .isEqualTo(GatewayErrorCode.RESOURCE_NOT_FOUND.status())
                .expectBody()
                .jsonPath("$.error.code")
                .isEqualTo(GatewayErrorCode.RESOURCE_NOT_FOUND.code());
    }

    @Test
    void handle_responseStatusException_badRequest() {
        // when & then
        webTestClient
                .get()
                .uri("/bad-request")
                .exchange()
                .expectStatus()
                .isEqualTo(GatewayErrorCode.INVALID_INPUT.status())
                .expectBody()
                .jsonPath("$.error.code")
                .isEqualTo(GatewayErrorCode.INVALID_INPUT.code());
    }

    @Test
    void handle_internalServerError() {
        // when & then
        webTestClient
                .get()
                .uri("/internal-server-error")
                .exchange()
                .expectStatus()
                .isEqualTo(GatewayErrorCode.INTERNAL_SERVER_ERROR.status())
                .expectBody()
                .jsonPath("$.error.code")
                .isEqualTo(GatewayErrorCode.INTERNAL_SERVER_ERROR.code());
    }
}
