package com.logistics.hubservice.infrastructure.naver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.logistics.hubservice.application.hubroute.initialization.HubCoordinates;
import com.logistics.hubservice.application.hubroute.initialization.RouteMetric;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

class NaverMapsAdapterTest {

    private MockRestServiceServer server;
    private NaverGeocodingAdapter geocodingAdapter;
    private NaverDirectionsAdapter directionsAdapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://naver.example");
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        geocodingAdapter = new NaverGeocodingAdapter(restClient);
        directionsAdapter = new NaverDirectionsAdapter(restClient);
    }

    @Test
    @DisplayName("Geocoding 결과의 x를 경도, y를 위도로 변환한다")
    void geocodesAnAddress() {
        server.expect(once(), requestTo(startsWith(
                        "https://naver.example/map-geocode/v2/geocode")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam(
                        "query",
                        UriUtils.encodeQueryParam(
                                "서울특별시 송파구 송파대로 55",
                                StandardCharsets.UTF_8)))
                .andRespond(withSuccess("""
                        {
                          "addresses": [
                            {"x": "127.12345678", "y": "37.87654321"}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        HubCoordinates coordinates = geocodingAdapter.geocode("서울특별시 송파구 송파대로 55");

        assertThat(coordinates.latitude()).isEqualByComparingTo("37.8765432");
        assertThat(coordinates.longitude()).isEqualByComparingTo("127.1234568");
        server.verify();
    }

    @Test
    @DisplayName("Geocoding 검색 결과가 비어 있으면 초기화를 중단한다")
    void rejectsAnEmptyGeocodingResult() {
        server.expect(requestTo(startsWith(
                        "https://naver.example/map-geocode/v2/geocode")))
                .andRespond(withSuccess("{\"addresses\": []}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> geocodingAdapter.geocode("찾을 수 없는 주소"))
                .isInstanceOf(NaverMapsException.class)
                .hasMessageContaining("Geocoding 결과가 없습니다");
        server.verify();
    }

    @Test
    @DisplayName("Directions trafast 결과를 미터와 올림한 초 단위로 변환한다")
    void getsTheTrafastRouteMetric() {
        HubCoordinates source = new HubCoordinates(
                new BigDecimal("37.1234567"),
                new BigDecimal("127.7654321"));
        HubCoordinates destination = new HubCoordinates(
                new BigDecimal("36.1111111"),
                new BigDecimal("128.2222222"));
        server.expect(once(), requestTo(startsWith(
                        "https://naver.example/map-direction/v1/driving")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("start", "127.7654321,37.1234567"))
                .andExpect(queryParam("goal", "128.2222222,36.1111111"))
                .andExpect(queryParam("option", "trafast"))
                .andRespond(withSuccess("""
                        {
                          "route": {
                            "trafast": [
                              {"summary": {"distance": 12345, "duration": 123456}}
                            ]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        RouteMetric metric = directionsAdapter.getMetric(source, destination);

        assertThat(metric.distanceMeters()).isEqualTo(12_345L);
        assertThat(metric.durationSeconds()).isEqualTo(124L);
        server.verify();
    }

    @Test
    @DisplayName("Directions trafast 경로가 비어 있으면 초기화를 중단한다")
    void rejectsAnEmptyDirectionsResult() {
        HubCoordinates source = new HubCoordinates(BigDecimal.ONE, BigDecimal.TEN);
        HubCoordinates destination = new HubCoordinates(BigDecimal.TEN, BigDecimal.ONE);
        server.expect(requestTo(startsWith(
                        "https://naver.example/map-direction/v1/driving")))
                .andRespond(withSuccess(
                        "{\"route\": {\"trafast\": []}}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> directionsAdapter.getMetric(source, destination))
                .isInstanceOf(NaverMapsException.class)
                .hasMessageContaining("Directions trafast 결과가 없습니다");
        server.verify();
    }

    @Test
    @DisplayName("Directions API 오류를 초기화 실패 예외로 변환한다")
    void translatesDirectionsApiErrors() {
        HubCoordinates source = new HubCoordinates(BigDecimal.ONE, BigDecimal.TEN);
        HubCoordinates destination = new HubCoordinates(BigDecimal.TEN, BigDecimal.ONE);
        server.expect(requestTo(startsWith(
                        "https://naver.example/map-direction/v1/driving")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> directionsAdapter.getMetric(source, destination))
                .isInstanceOf(NaverMapsException.class)
                .hasMessageContaining("Directions 요청에 실패했습니다");
        server.verify();
    }
}
