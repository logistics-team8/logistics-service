package com.logistics.userservice.application;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.delivery.DeliveryManagerCreateCommand;
import com.logistics.userservice.application.dto.user.UserCreateCommand;
import com.logistics.userservice.application.port.DeliveryClientPort;
import com.logistics.userservice.domain.RequestedRole;
import com.logistics.userservice.domain.User;
import com.logistics.userservice.domain.UserRepository;
import com.logistics.userservice.error.ClientErrorCode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeliveryManagerSyncServiceTest - 단위 테스트")
class DeliveryManagerSyncServiceUnitTest {
    @Mock private DeliveryClientPort deliveryClientPort;
    @Mock private UserRepository userRepository;
    @InjectMocks private DeliveryManagerSyncService deliveryManagerSyncService;

    @Nested
    @DisplayName("배송 담당자 생성 테스트")
    class Create {
        @Test
        @DisplayName("PROCESSING 상태인 배송 담당자 생성을 완료한다.")
        void create_success() {
            // given
            UUID userId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();
            RequestedRole role = RequestedRole.HUB_DELIVERY;

            UserCreateCommand deliveryCommand =
                    new UserCreateCommand(
                            "delivery1234",
                            "Testtest123!",
                            "김철수",
                            "U123354789",
                            UUID.randomUUID(),
                            null,
                            RequestedRole.HUB_DELIVERY);

            User deliveryUser = User.create(deliveryCommand);

            deliveryUser.approve(UUID.randomUUID());

            DeliveryManagerCreateCommand command =
                    DeliveryManagerCreateCommand.of(userId, hubId, role);

            given(userRepository.findByIdAndDeletedAtIsNull(any()))
                    .willReturn(Optional.of(deliveryUser));

            willDoNothing().given(deliveryClientPort).createDeliveryManager(any(), any(), any());

            // when & then
            assertThatCode(() -> deliveryManagerSyncService.create(command))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("회원이 PROCESSING 상태가 아니면 메서드를 종료한다.")
        void create_success_when_not_found() {
            // given
            UUID userId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();
            RequestedRole role = RequestedRole.HUB_DELIVERY;

            UserCreateCommand deliveryCommand =
                    new UserCreateCommand(
                            "delivery1234",
                            "Testtest123!",
                            "김철수",
                            "U123354789",
                            UUID.randomUUID(),
                            null,
                            RequestedRole.HUB_DELIVERY);

            User deliveryUser = User.create(deliveryCommand);

            DeliveryManagerCreateCommand command =
                    DeliveryManagerCreateCommand.of(userId, hubId, role);

            given(userRepository.findByIdAndDeletedAtIsNull(any()))
                    .willReturn(Optional.of(deliveryUser));

            // when & then
            assertThatCode(() -> deliveryManagerSyncService.create(command))
                    .doesNotThrowAnyException();

            verify(deliveryClientPort, never()).createDeliveryManager(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("스케쥴러 배송 담당자 PROCESSING 상태 동기화 테스트")
    class SyncProcessingDeliveryManagers {
        @Test
        @DisplayName("PROCESSING 상태인 회원의 배송 담당자 생성을 요청하고 승인 처리를 완료한다. ")
        void syncProcessingDeliveryManagers_success() {
            // given
            UUID userId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();
            RequestedRole role = RequestedRole.HUB_DELIVERY;

            User dummyUser = mock(User.class);

            given(dummyUser.getId()).willReturn(userId);
            given(dummyUser.getHubId()).willReturn(hubId);
            given(dummyUser.getRequestedRole()).willReturn(role);

            given(userRepository.findAllByUserStatus(any(), any())).willReturn(List.of(dummyUser));

            // when
            deliveryManagerSyncService.syncProcessingDeliveryManagers();

            // then
            verify(userRepository).findAllByUserStatus(any(), any());
            verify(deliveryClientPort).createDeliveryManager(any(), any(), any());
            verify(dummyUser).completeProvisioning();
        }

        @Test
        @DisplayName("PROCESSING 상태인 회원이 없으면 메서드를 종료한다.")
        void syncProcessingDeliveryManagers_success_when_not_processing_user() {
            // given
            given(userRepository.findAllByUserStatus(any(), any())).willReturn(List.of());

            // when
            deliveryManagerSyncService.syncProcessingDeliveryManagers();

            // then
            verify(deliveryClientPort, never()).createDeliveryManager(any(), any(), any());
        }

        @Test
        @DisplayName("배송 담당자 생성에 실패 시 해당 유저는 승인 처리를 하지 않는다.")
        void syncProcessingDeliveryManagers_fail_when_delivery_service_error() {
            // given
            UUID userId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();
            RequestedRole role = RequestedRole.HUB_DELIVERY;

            User dummyUser = mock(User.class);

            given(dummyUser.getId()).willReturn(userId);
            given(dummyUser.getHubId()).willReturn(hubId);
            given(dummyUser.getRequestedRole()).willReturn(role);

            given(userRepository.findAllByUserStatus(any(), any())).willReturn(List.of(dummyUser));

            willThrow(new BusinessException(ClientErrorCode.SERVICE_UNAVAILABLE))
                    .given(deliveryClientPort)
                    .createDeliveryManager(any(), any(), any());

            // when
            deliveryManagerSyncService.syncProcessingDeliveryManagers();

            // then
            verify(userRepository).findAllByUserStatus(any(), any());
            verify(deliveryClientPort).createDeliveryManager(any(), any(), any());
            verify(dummyUser, never()).completeProvisioning();
        }

        @Test
        @DisplayName("배송 담당자 생성에 실패해도 리스트가 끝날 때까지 계속 시도한다.")
        void syncProcessingDeliveryManagers_continues_when_one_user_failed() {
            // given
            UUID successUserId = UUID.randomUUID();
            UUID successHubId = UUID.randomUUID();

            UUID failUserId = UUID.randomUUID();
            UUID failHubId = UUID.randomUUID();

            User successUser = mock(User.class);
            User failUser = mock(User.class);

            RequestedRole role = RequestedRole.HUB_DELIVERY;

            given(successUser.getId()).willReturn(successUserId);
            given(successUser.getHubId()).willReturn(successHubId);
            given(successUser.getRequestedRole()).willReturn(role);

            given(failUser.getId()).willReturn(failUserId);
            given(failUser.getHubId()).willReturn(failHubId);
            given(failUser.getRequestedRole()).willReturn(role);

            given(userRepository.findAllByUserStatus(any(), any()))
                    .willReturn(List.of(failUser, successUser));

            willThrow(new BusinessException(ClientErrorCode.SERVICE_UNAVAILABLE))
                    .given(deliveryClientPort)
                    .createDeliveryManager(failUserId, failHubId, role);

            // when
            deliveryManagerSyncService.syncProcessingDeliveryManagers();

            // then
            verify(deliveryClientPort).createDeliveryManager(successUserId, successHubId, role);

            verify(failUser, never()).completeProvisioning();
            verify(successUser).completeProvisioning();
        }
    }
}
