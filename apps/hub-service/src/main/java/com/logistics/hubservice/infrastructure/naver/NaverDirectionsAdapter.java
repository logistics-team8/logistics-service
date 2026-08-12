package com.logistics.hubservice.infrastructure.naver;

import com.logistics.hubservice.application.hubroute.initialization.HubCoordinates;
import com.logistics.hubservice.application.hubroute.initialization.RouteMetric;
import com.logistics.hubservice.application.hubroute.initialization.RouteMetricProvider;
import java.util.List;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class NaverDirectionsAdapter implements RouteMetricProvider {

    private static final String DIRECTIONS_PATH = "/map-direction/v1/driving";
    private static final String ROUTE_OPTION = "trafast";
    private static final long MILLIS_PER_SECOND = 1_000L;

    private final RestClient restClient;

    public NaverDirectionsAdapter(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public RouteMetric getMetric(HubCoordinates source, HubCoordinates destination) {
        if (source == null || destination == null) {
            throw new IllegalArgumentException("출발 좌표와 도착 좌표는 필수입니다.");
        }

        try {
            DirectionsResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(DIRECTIONS_PATH)
                            .queryParam("start", coordinateParameter(source))
                            .queryParam("goal", coordinateParameter(destination))
                            .queryParam("option", ROUTE_OPTION)
                            .build())
                    .retrieve()
                    .body(DirectionsResponse.class);
            return extractMetric(response);
        } catch (NaverMapsException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new NaverMapsException("네이버 Directions 요청에 실패했습니다.", exception);
        } catch (RuntimeException exception) {
            throw new NaverMapsException("네이버 Directions 응답을 처리할 수 없습니다.", exception);
        }
    }

    private RouteMetric extractMetric(DirectionsResponse response) {
        List<DirectionPath> trafast = response == null || response.route() == null
                ? null
                : response.route().trafast();
        if (trafast == null || trafast.isEmpty() || trafast.getFirst() == null) {
            throw new NaverMapsException("네이버 Directions trafast 결과가 없습니다.");
        }

        DirectionSummary summary = trafast.getFirst().summary();
        if (summary == null || summary.distance() <= 0 || summary.duration() <= 0) {
            throw new NaverMapsException("네이버 Directions trafast 요약이 유효하지 않습니다.");
        }
        return new RouteMetric(
                summary.distance(),
                Math.ceilDiv(summary.duration(), MILLIS_PER_SECOND));
    }

    private String coordinateParameter(HubCoordinates coordinates) {
        return coordinates.longitude().toPlainString()
                + ","
                + coordinates.latitude().toPlainString();
    }

    private record DirectionsResponse(DirectionRoutes route) {
    }

    private record DirectionRoutes(List<DirectionPath> trafast) {
    }

    private record DirectionPath(DirectionSummary summary) {
    }

    private record DirectionSummary(long distance, long duration) {
    }
}
