package com.logistics.hubservice.infrastructure.cache;

import com.logistics.hubservice.application.hub.dto.HubResponse;
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

    private static final String HUB_BY_ID_CACHE = "hubById";
    private static final String HUB_ROUTE_BY_ID_CACHE = "hubRouteById";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    @Bean
    CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();
        JacksonJsonRedisSerializer<HubResponse> hubSerializer =
                new JacksonJsonRedisSerializer<>(jsonMapper, HubResponse.class);
        JacksonJsonRedisSerializer<HubRouteResponse> hubRouteSerializer =
                new JacksonJsonRedisSerializer<>(jsonMapper, HubRouteResponse.class);

        RedisCacheConfiguration defaultConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(CACHE_TTL)
                .disableCachingNullValues();
        RedisCacheConfiguration hubConfiguration = defaultConfiguration
                .serializeValuesWith(SerializationPair.fromSerializer(hubSerializer));
        RedisCacheConfiguration hubRouteConfiguration = defaultConfiguration
                .serializeValuesWith(SerializationPair.fromSerializer(hubRouteSerializer));
        RedisCacheWriter cacheWriter = RedisCacheWriter.create(
                connectionFactory,
                configurer -> configurer.immediateWrites());

        return RedisCacheManager.builder(cacheWriter)
                .cacheDefaults(defaultConfiguration)
                .withInitialCacheConfigurations(Map.of(
                        HUB_BY_ID_CACHE,
                        hubConfiguration,
                        HUB_ROUTE_BY_ID_CACHE,
                        hubRouteConfiguration))
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler(true);
    }
}
