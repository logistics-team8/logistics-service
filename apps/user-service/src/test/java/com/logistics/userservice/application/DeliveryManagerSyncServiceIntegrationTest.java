package com.logistics.userservice.application;

import com.logistics.userservice.application.port.DeliveryClientPort;
import com.logistics.userservice.config.test.AbstractIntegrationTest;
import com.logistics.userservice.domain.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeliveryManagerSyncServiceTest - 통합 테스트")
class DeliveryManagerSyncServiceIntegrationTest extends AbstractIntegrationTest {
    @Mock private DeliveryClientPort deliveryClientPort;
    @Mock private UserRepository userRepository;
    @InjectMocks private DeliveryManagerSyncService deliveryManagerSyncService;

    @Test
    @DisplayName("PROCESSING 상태인 배송 담당자 생성을 완료한다.")
    void create_success() {}

    @Test
    void syncProcessingDeliveryManagers() {}
}
