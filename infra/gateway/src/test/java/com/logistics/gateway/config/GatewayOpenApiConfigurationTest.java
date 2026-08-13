package com.logistics.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties.SwaggerUrl;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.FileSystemResource;

class GatewayOpenApiConfigurationTest {

    private static final Map<String, String> DOMAIN_DOCS =
            Map.of(
                    "user-service", "/user-service/v3/api-docs",
                    "hub-service", "/hub-service/v3/api-docs",
                    "company-product-service", "/company-product-service/v3/api-docs",
                    "order-service", "/order-service/v3/api-docs",
                    "delivery-service", "/delivery-service/v3/api-docs",
                    "notification-service", "/notification-service/v3/api-docs");

    @Test
    void registersAllDomainDocumentsInSwaggerUi() throws IOException {
        PropertySource<?> gateway = loadGatewayConfiguration();
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(gateway);

        SwaggerUiConfigProperties swaggerUi =
                Binder.get(environment)
                        .bind("springdoc.swagger-ui", Bindable.of(SwaggerUiConfigProperties.class))
                        .orElseThrow(IllegalStateException::new);
        Map<String, String> registeredDocs =
                swaggerUi.getUrls().stream()
                        .collect(Collectors.toMap(SwaggerUrl::getName, SwaggerUrl::getUrl));

        assertThat(registeredDocs).containsExactlyInAnyOrderEntriesOf(DOMAIN_DOCS);
    }

    @Test
    void routesNotificationPublicApiThroughGateway() throws IOException {
        PropertySource<?> gateway = loadGatewayConfiguration();

        assertThat(gateway.getProperty("spring.cloud.gateway.server.webflux.routes[10].id"))
                .isEqualTo("notification-service");
        assertThat(
                        gateway.getProperty(
                                "spring.cloud.gateway.server.webflux.routes[10].predicates[0]"))
                .isEqualTo("Path=/api/slack-messages/**");
    }

    private PropertySource<?> loadGatewayConfiguration() throws IOException {
        Path repositoryRoot = Path.of(System.getProperty("repositoryRoot"));
        FileSystemResource resource =
                new FileSystemResource(repositoryRoot.resolve("config-repo/gateway.yml"));

        return new YamlPropertySourceLoader().load("gateway", resource).getFirst();
    }
}
