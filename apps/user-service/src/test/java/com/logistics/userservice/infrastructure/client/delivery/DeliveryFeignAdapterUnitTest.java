package com.logistics.userservice.infrastructure.client.delivery;

import static com.logistics.userservice.domain.RequestedRole.HUB_DELIVERY;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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

@DisplayName("DeliveryFeignAdapterTest - 단위 테스트")
@ExtendWith(MockitoExtension.class)
class DeliveryFeignAdapterUnitTest {
    @Mock private DeliveryFeignClient deliveryFeignClient;
    @InjectMocks private DeliveryFeignAdapter deliveryFeignAdapter;

    @Test
    @DisplayName("배송 담당자 생성에 성공한다.")
    void createDeliveryManager_success() {
        // given
        UUID userId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        CreateDeliveryManagerRequest request =
                CreateDeliveryManagerRequest.of(userId, hubId, HUB_DELIVERY);

        given(deliveryFeignClient.createDeliveryManager(request))
                .willReturn(ApiResponse.success(null));

        // when
        deliveryFeignAdapter.createDeliveryManager(userId, hubId, HUB_DELIVERY);

        // then
        then(deliveryFeignClient).should().createDeliveryManager(request);
    }

    @Test
    @DisplayName("이미 중복된 생성된 배송 담당자를 생성할 시 성공으로 처리한다.")
    void DeliveryManager_success_duplicate() {
        // given
        UUID userId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        CreateDeliveryManagerRequest request =
                CreateDeliveryManagerRequest.of(userId, hubId, HUB_DELIVERY);

        given(deliveryFeignClient.createDeliveryManager(request))
                .willThrow(FeignException.Conflict.class);

        // when
        deliveryFeignAdapter.createDeliveryManager(userId, hubId, HUB_DELIVERY);

        // then
        then(deliveryFeignClient).should().createDeliveryManager(request);
    }

    @Test
    @DisplayName("Hub-service 호출에 실패하면 SERVICE_UNAVAILABLE 예외가 발생한다.")
    void DeliveryManager_fail_when_service_unavailable() {
        // given
        UUID userId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        CreateDeliveryManagerRequest request =
                CreateDeliveryManagerRequest.of(userId, hubId, HUB_DELIVERY);

        given(deliveryFeignClient.createDeliveryManager(request))
                .willThrow(FeignException.ServiceUnavailable.class);

        // when & then
        assertThatThrownBy(
                        () ->
                                deliveryFeignAdapter.createDeliveryManager(
                                        userId, hubId, HUB_DELIVERY))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ClientErrorCode.SERVICE_UNAVAILABLE);
    }
}
