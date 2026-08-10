package com.logistics.userservice.infrastructure.client.hub;

import com.logistics.common.error.CommonErrorCode;
import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.port.HubClientPort;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubFeignAdapter implements HubClientPort {
    private final HubFeignClient hubFeignClient;

    @Override
    public boolean existsById(UUID hubId) {
        // TODO : HUB REDIS Cache 먼저 조회
        try {
            HubExistsResponse response =
                    hubFeignClient.checkHubExists(hubId).getData();
            log.info("[SUCCESS] HubService 호출 성공 hubId = {}", hubId);
            return response != null && response.exists();
        } catch (FeignException.NotFound e) {
            log.info("[SUCCESS] HubService 호출 성공 404 {}", e.getMessage());
            return false;
        } catch (FeignException e) {
            log.error("[ERROR] HubService 호출 실패 hubId = {}", hubId, e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
