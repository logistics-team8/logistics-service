package com.logistics.hubservice.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.interceptor.CacheErrorHandler;

class CacheConfigurationTest {

    @Test
    @DisplayName("캐시 오류를 로그로 남기고 애플리케이션에 전파하지 않는다")
    void cacheErrorsAreIgnoredForDatabaseFallback() {
        CacheErrorHandler errorHandler = new CacheConfiguration().errorHandler();
        Cache cache = new ConcurrentMapCache("hubRouteById");
        RuntimeException cacheFailure = new IllegalStateException("Redis unavailable");

        assertThatCode(() -> {
            errorHandler.handleCacheGetError(cacheFailure, cache, "route-id");
            errorHandler.handleCachePutError(cacheFailure, cache, "route-id", "value");
            errorHandler.handleCacheEvictError(cacheFailure, cache, "route-id");
            errorHandler.handleCacheClearError(cacheFailure, cache);
        }).doesNotThrowAnyException();
    }
}
