package com.logistics.hubservice.infrastructure.naver;

import com.logistics.hubservice.application.hubroute.initialization.HubRouteDefaultDataInitializer;
import com.logistics.hubservice.application.hubroute.initialization.HubRouteDefaultDataService;
import com.logistics.hubservice.domain.hub.HubRepository;
import com.logistics.hubservice.domain.hubroute.HubRouteRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "hub-route.default-data",
        name = "enabled",
        havingValue = "true")
@EnableConfigurationProperties(NaverMapsProperties.class)
public class DefaultHubRouteDataConfiguration {

    private static final String API_KEY_ID_HEADER = "x-ncp-apigw-api-key-id";
    private static final String API_KEY_HEADER = "x-ncp-apigw-api-key";

    @Bean("naverMapsRestClient")
    RestClient naverMapsRestClient(
            RestClient.Builder builder,
            NaverMapsProperties properties) {
        return builder
                .baseUrl(properties.baseUrl())
                .defaultHeader(API_KEY_ID_HEADER, properties.apiKeyId())
                .defaultHeader(API_KEY_HEADER, properties.apiKey())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    NaverGeocodingAdapter naverGeocodingAdapter(
            @Qualifier("naverMapsRestClient") RestClient restClient) {
        return new NaverGeocodingAdapter(restClient);
    }

    @Bean
    NaverDirectionsAdapter naverDirectionsAdapter(
            @Qualifier("naverMapsRestClient") RestClient restClient) {
        return new NaverDirectionsAdapter(restClient);
    }

    @Bean
    HubRouteDefaultDataService hubRouteDefaultDataService(
            HubRepository hubRepository,
            HubRouteRepository hubRouteRepository,
            NaverGeocodingAdapter locationProvider,
            NaverDirectionsAdapter metricProvider) {
        return new HubRouteDefaultDataService(
                hubRepository,
                hubRouteRepository,
                locationProvider,
                metricProvider);
    }

    @Bean
    HubRouteDefaultDataInitializer hubRouteDefaultDataInitializer(
            HubRouteDefaultDataService service) {
        return new HubRouteDefaultDataInitializer(service);
    }
}
