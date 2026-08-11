package com.logistics.hubservice.application.hubroute;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class HubRouteCacheEvictionListener {

    private final HubRouteCacheEvictor hubRouteCacheEvictor;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void evictDeletedHubRoutes(HubRoutesDeletedEvent event) {
        event.hubRouteIds().forEach(hubRouteCacheEvictor::evictHubRouteById);
        hubRouteCacheEvictor.evictAllPaths();
    }
}
