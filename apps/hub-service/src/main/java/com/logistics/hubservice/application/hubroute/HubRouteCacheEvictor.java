package com.logistics.hubservice.application.hubroute;

import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

@Component
public class HubRouteCacheEvictor {

    @CacheEvict(cacheNames = "hubRouteById", key = "#hubRouteId")
    public void evictHubRouteById(UUID hubRouteId) {
    }

    @CacheEvict(cacheNames = "hubRoutePath", allEntries = true)
    public void evictAllPaths() {
    }
}
