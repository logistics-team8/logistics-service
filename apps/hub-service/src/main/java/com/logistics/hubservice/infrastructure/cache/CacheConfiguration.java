package com.logistics.hubservice.infrastructure.cache;

import com.logistics.hubservice.application.hubroute.dto.HubRoutePathResponse;
import com.logistics.hubservice.application.hubroute.dto.HubRouteResponse;
import java.time.Duration;
import java.util.Map;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableCaching
public class CacheConfiguration implements CachingConfigurer {

    private static final String HUB_ROUTE_BY_ID_CACHE = "hubRouteById";
    private static final String HUB_ROUTE_PATH_CACHE = "hubRoutePath";
    private static final Duration HUB_ROUTE_CACHE_TTL = Duration.ofHours(1);

    @Bean
    CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();
        JacksonJsonRedisSerializer<HubRouteResponse> hubRouteSerializer =
                new JacksonJsonRedisSerializer<>(jsonMapper, HubRouteResponse.class);
        JacksonJsonRedisSerializer<HubRoutePathResponse> hubRoutePathSerializer =
                new JacksonJsonRedisSerializer<>(jsonMapper, HubRoutePathResponse.class);

        RedisCacheConfiguration defaultConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(HUB_ROUTE_CACHE_TTL)
                .disableCachingNullValues();
        RedisCacheConfiguration hubRouteConfiguration = defaultConfiguration
                .serializeValuesWith(SerializationPair.fromSerializer(hubRouteSerializer));
        RedisCacheConfiguration hubRoutePathConfiguration = defaultConfiguration
                .serializeValuesWith(SerializationPair.fromSerializer(hubRoutePathSerializer));
        RedisCacheWriter cacheWriter = RedisCacheWriter.create(
                connectionFactory,
                configurer -> configurer.immediateWrites());

        return RedisCacheManager.builder(cacheWriter)
                .cacheDefaults(defaultConfiguration)
                .withInitialCacheConfigurations(Map.of(
                        HUB_ROUTE_BY_ID_CACHE,
                        hubRouteConfiguration,
                        HUB_ROUTE_PATH_CACHE,
                        hubRoutePathConfiguration))
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler(true);
    }
}
