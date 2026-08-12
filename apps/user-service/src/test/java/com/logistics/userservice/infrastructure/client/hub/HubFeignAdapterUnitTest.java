package com.logistics.userservice.infrastructure.client.hub;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.logistics.common.exception.BusinessException;
import com.logistics.common.response.ApiResponse;
import com.logistics.userservice.error.ClientErrorCode;
import feign.FeignException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("HubFeignAdapterTest - 단위 테스트")
@ExtendWith(MockitoExtension.class)
class HubFeignAdapterUnitTest {

    @Mock private HubFeignClient hubFeignClient;

    @InjectMocks private HubFeignAdapter hubFeignAdapter;

    @Test
    @DisplayName("허브 조회에 성공하면 true를 반환한다.")
    void checkHubExists_success() {
        // given
        UUID hubId = UUID.randomUUID();
        HubExistsResponse hubExistsResponse = new HubExistsResponse(hubId, true);

        given(hubFeignClient.checkHubExists(hubId))
                .willReturn(ApiResponse.success(hubExistsResponse));

        // when
        boolean result = hubFeignAdapter.existsById(hubId);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("허브의 ID가 일치하지 않으면 false를 반환한다.")
    void checkHubExists_fail_when_invalid_hubId() {
        UUID hubId = UUID.randomUUID();
        HubExistsResponse hubExistsResponse = new HubExistsResponse(hubId, false);

        given(hubFeignClient.checkHubExists(hubId))
                .willReturn(ApiResponse.success(hubExistsResponse));

        // when
        boolean result = hubFeignAdapter.existsById(hubId);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Hub-service 호출에 실패하면 SERVICE_UNAVAILABLE 예외가 발생한다.")
    void checkHubExists_fail_when_service_unavailable() {
        // given
        UUID hubId = UUID.randomUUID();

        given(hubFeignClient.checkHubExists(hubId))
                .willThrow(FeignException.ServiceUnavailable.class);

        // when & then
        assertThatThrownBy(() -> hubFeignAdapter.existsById(hubId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ClientErrorCode.SERVICE_UNAVAILABLE);
    }
}
