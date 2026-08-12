package com.logistics.hubservice.infrastructure.naver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.logistics.hubservice.application.hubroute.initialization.HubRouteDefaultDataInitializer;
import com.logistics.hubservice.application.hubroute.initialization.HubRouteDefaultDataService;
import com.logistics.hubservice.domain.hub.HubRepository;
import com.logistics.hubservice.domain.hubroute.HubRouteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class DefaultHubRouteDataConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(DefaultHubRouteDataConfiguration.class);

    @Test
    @DisplayName("기본 데이터 초기화는 명시적으로 활성화하지 않으면 구성하지 않는다")
    void keepsDefaultDataInitializationDisabledByDefault() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(HubRouteDefaultDataService.class);
            assertThat(context).doesNotHaveBean(HubRouteDefaultDataInitializer.class);
            assertThat(context).doesNotHaveBean(NaverGeocodingAdapter.class);
            assertThat(context).doesNotHaveBean(NaverDirectionsAdapter.class);
        });
    }

    @Test
    @DisplayName("활성화하면 네이버 Adapter와 기본 데이터 초기화기를 구성한다")
    void configuresDefaultDataInitializationWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "hub-route.default-data.enabled=true",
                        "naver.maps.base-url=https://naver.example",
                        "naver.maps.api-key-id=client-id",
                        "naver.maps.api-key=client-secret")
                .withBean(RestClient.Builder.class, RestClient::builder)
                .withBean(HubRepository.class, () -> mock(HubRepository.class))
                .withBean(HubRouteRepository.class, () -> mock(HubRouteRepository.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(HubRouteDefaultDataService.class);
                    assertThat(context).hasSingleBean(HubRouteDefaultDataInitializer.class);
                    assertThat(context).hasSingleBean(NaverGeocodingAdapter.class);
                    assertThat(context).hasSingleBean(NaverDirectionsAdapter.class);
                });
    }

    @Test
    @DisplayName("초기화를 활성화했지만 네이버 설정이 없으면 시작 단계에서 실패한다")
    void failsFastWhenEnabledWithoutNaverConfiguration() {
        contextRunner
                .withPropertyValues("hub-route.default-data.enabled=true")
                .withBean(RestClient.Builder.class, RestClient::builder)
                .withBean(HubRepository.class, () -> mock(HubRepository.class))
                .withBean(HubRouteRepository.class, () -> mock(HubRouteRepository.class))
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("네이버 전용 RestClient는 인증 헤더를 모든 요청에 포함한다")
    void addsNaverAuthenticationHeaders() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NaverMapsProperties properties = new NaverMapsProperties(
                "https://naver.example",
                "client-id",
                "client-secret");
        RestClient restClient = new DefaultHubRouteDataConfiguration()
                .naverMapsRestClient(builder, properties);
        server.expect(requestTo("https://naver.example/probe"))
                .andExpect(header("x-ncp-apigw-api-key-id", "client-id"))
                .andExpect(header("x-ncp-apigw-api-key", "client-secret"))
                .andExpect(header("Accept", MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        restClient.get().uri("/probe").retrieve().toBodilessEntity();

        server.verify();
    }
}
