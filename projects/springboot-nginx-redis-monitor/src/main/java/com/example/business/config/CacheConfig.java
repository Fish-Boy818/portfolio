package com.example.business.config;

import java.time.Duration;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

@Configuration
public class CacheConfig {
    @Bean
    RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return builder -> builder
            .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues());
    }

    @Bean
    KeyGenerator stableKeyGenerator() {
        return (target, method, params) -> {
            StringBuilder key = new StringBuilder(method.getName());
            for (Object param : params) {
                key.append(':').append(param);
            }
            return key.toString();
        };
    }
}
