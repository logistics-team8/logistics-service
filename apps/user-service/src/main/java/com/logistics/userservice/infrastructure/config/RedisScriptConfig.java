package com.logistics.userservice.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.RedisScript;

@Configuration
public class RedisScriptConfig {
    @Bean
    public RedisScript<Void> loginSessionScript() {
        return RedisScript.of(new ClassPathResource("redis/login.lua"));
    }
}
