package com.logistics.userservice.infrastructure.client.hub;

import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.port.HubClientPort;
import com.logistics.userservice.error.ClientErrorCode;
import feign.FeignException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubFeignAdapter implements HubClientPort {
    private final HubFeignClient hubFeignClient;

    @Override
    public boolean existsById(UUID hubId) {
        try {
            HubExistsResponse response = hubFeignClient.checkHubExists(hubId).getData();
            boolean exists = response != null && response.exists();

            log.info("[SUCCESS] Hub 존재 여부 조회 성공 hubId = {} result = {}", hubId, exists);
            return exists;

        } catch (FeignException e) {
            log.error(
                    "[ERROR] Hub-Service 호출 실패 hubId = {}, status = {}, content = {}",
                    hubId,
                    e.status(),
                    e.contentUTF8(),
                    e);
            throw new BusinessException(ClientErrorCode.SERVICE_UNAVAILABLE);
        }
    }
}
