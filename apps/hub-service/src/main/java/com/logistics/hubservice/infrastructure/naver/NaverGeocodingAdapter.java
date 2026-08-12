package com.logistics.hubservice.infrastructure.naver;

import com.logistics.hubservice.application.hubroute.initialization.HubCoordinates;
import com.logistics.hubservice.application.hubroute.initialization.HubLocationProvider;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class NaverGeocodingAdapter implements HubLocationProvider {

    private static final String GEOCODING_PATH = "/map-geocode/v2/geocode";

    private final RestClient restClient;

    public NaverGeocodingAdapter(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public HubCoordinates geocode(String address) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("주소는 필수입니다.");
        }

        try {
            GeocodingResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(GEOCODING_PATH)
                            .queryParam("query", address)
                            .build())
                    .retrieve()
                    .body(GeocodingResponse.class);
            return extractCoordinates(response, address);
        } catch (NaverMapsException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new NaverMapsException("네이버 Geocoding 요청에 실패했습니다: " + address, exception);
        } catch (RuntimeException exception) {
            throw new NaverMapsException("네이버 Geocoding 응답을 처리할 수 없습니다: " + address, exception);
        }
    }

    private HubCoordinates extractCoordinates(GeocodingResponse response, String address) {
        if (response == null || response.addresses() == null || response.addresses().isEmpty()) {
            throw new NaverMapsException("네이버 Geocoding 결과가 없습니다: " + address);
        }

        GeocodedAddress result = response.addresses().getFirst();
        if (result == null || result.x() == null || result.y() == null) {
            throw new NaverMapsException("네이버 Geocoding 좌표가 없습니다: " + address);
        }
        return new HubCoordinates(new BigDecimal(result.y()), new BigDecimal(result.x()));
    }

    private record GeocodingResponse(List<GeocodedAddress> addresses) {
    }

    private record GeocodedAddress(String x, String y) {
    }
}
