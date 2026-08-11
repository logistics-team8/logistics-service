package com.logistics.hubservice.application.hubroute.initialization;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record HubCoordinates(BigDecimal latitude, BigDecimal longitude) {

    private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");
    private static final int DATABASE_SCALE = 7;

    public HubCoordinates {
        validateRange(latitude, MIN_LATITUDE, MAX_LATITUDE, "위도");
        validateRange(longitude, MIN_LONGITUDE, MAX_LONGITUDE, "경도");
        latitude = latitude.setScale(DATABASE_SCALE, RoundingMode.HALF_UP);
        longitude = longitude.setScale(DATABASE_SCALE, RoundingMode.HALF_UP);
    }

    private static void validateRange(
            BigDecimal value,
            BigDecimal minimum,
            BigDecimal maximum,
            String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + "는 필수입니다.");
        }
        if (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
            throw new IllegalArgumentException(fieldName + "가 허용 범위를 벗어났습니다.");
        }
    }
}
