package com.adwitiya.feedbackportal.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Redis-backed caches with per-cache time-to-live.
 *
 * <p>Dashboard aggregates are the expensive reads in this application; caching
 * them for a minute removes almost all of the analytical query load without
 * making the numbers meaningfully stale.</p>
 */
@Configuration
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
public class CacheConfig {

    public static final String CACHE_DASHBOARD = "dashboardStats";
    public static final String CACHE_DEPARTMENTS = "departments";
    public static final String CACHE_TRENDS = "feedbackTrends";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // EVERYTHING, not NON_FINAL. Every DTO cached here is a record, and
        // records are final, so NON_FINAL writes no type id for them. A cached
        // List<DepartmentResponse> then round-trips as a list of untyped
        // objects and Jackson fails on read with "expected VALUE_STRING: need
        // ... type id". The type id has to be written for final types too.
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .activateDefaultTyping(
                        com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator.instance,
                        ObjectMapper.DefaultTyping.EVERYTHING,
                        com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY);

        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(mapper)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base)
                .withInitialCacheConfigurations(Map.of(
                        CACHE_DASHBOARD, base.entryTtl(Duration.ofMinutes(1)),
                        CACHE_TRENDS, base.entryTtl(Duration.ofMinutes(10)),
                        CACHE_DEPARTMENTS, base.entryTtl(Duration.ofHours(1))))
                .transactionAware()
                .build();
    }
}
