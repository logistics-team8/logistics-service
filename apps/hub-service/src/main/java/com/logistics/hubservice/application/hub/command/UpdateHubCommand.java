package com.logistics.hubservice.application.hub.command;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateHubCommand(
        @Pattern(regexp = "(?s).*\\S.*", message = "허브 이름은 공백만으로 구성할 수 없습니다.")
        @Size(max = 100, message = "허브 이름은 100자 이하여야 합니다.")
        String name,
        @Pattern(regexp = "(?s).*\\S.*", message = "허브 주소는 공백만으로 구성할 수 없습니다.")
        @Size(max = 255, message = "허브 주소는 255자 이하여야 합니다.")
        String address,
        @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
        @Digits(integer = 2, fraction = 7, message = "위도는 소수점 이하 7자리 이하여야 합니다.")
        BigDecimal latitude,
        @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
        @Digits(integer = 3, fraction = 7, message = "경도는 소수점 이하 7자리 이하여야 합니다.")
        BigDecimal longitude
) {

    @AssertTrue(message = "수정할 항목을 하나 이상 입력해야 합니다.")
    public boolean isUpdateRequested() {
        return name != null || address != null || latitude != null || longitude != null;
    }
}
