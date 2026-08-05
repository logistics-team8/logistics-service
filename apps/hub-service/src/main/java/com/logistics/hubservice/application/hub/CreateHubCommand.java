package com.logistics.hubservice.application.hub;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateHubCommand(
        @NotBlank(message = "허브 이름은 필수입니다.")
        @Size(max = 100, message = "허브 이름은 100자 이하여야 합니다.")
        String name,
        @NotBlank(message = "허브 주소는 필수입니다.")
        @Size(max = 255, message = "허브 주소는 255자 이하여야 합니다.")
        String address,
        @NotNull(message = "위도는 필수입니다.")
        @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
        @Digits(integer = 2, fraction = 7, message = "위도는 소수점 이하 7자리 이하여야 합니다.")
        BigDecimal latitude,
        @NotNull(message = "경도는 필수입니다.")
        @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
        @Digits(integer = 3, fraction = 7, message = "경도는 소수점 이하 7자리 이하여야 합니다.")
        BigDecimal longitude
) {
}
