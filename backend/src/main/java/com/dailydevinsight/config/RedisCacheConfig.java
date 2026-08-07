package com.dailydevinsight.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class RedisCacheConfig {

    public static final String CACHE_WEEKLY_TOP10 = "weeklyHotTop10";
    public static final String CACHE_WEEKLY_TOP5 = "weeklyHotTop5";
    public static final String CACHE_INSIGHTS_BY_DATE = "insightsByDate";
    public static final String CACHE_INSIGHTS_BY_RANGE = "insightsByRange";
    public static final String CACHE_INSIGHT_ENGAGEMENT = "insightEngagement";
    public static final String CACHE_ADMIN_STATS = "adminStats";
    public static final String CACHE_ADMIN_CONTENT_VIEW_STATS = "adminContentViewStats";
    public static final String CACHE_ADMIN_BOOKMARK_STATS = "adminBookmarkStats";

    /**
     * @date 2026-04-15
     * @desc Redis 캐시 매니저를 생성하고 캐시별 TTL 정책을 설정합니다.
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaultConfiguration = createDefaultRedisCacheConfiguration(Duration.ofMinutes(10));
        Map<String, RedisCacheConfiguration> cacheConfigurations = createCacheConfigurations();

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfiguration)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    /**
     * @date 2026-04-15
     * @desc 기본 직렬화/TTL 정책을 포함한 Redis 캐시 설정을 생성합니다.
     */
    private RedisCacheConfiguration createDefaultRedisCacheConfiguration(Duration ttl) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer));
    }

    /**
     * @date 2026-04-15
     * @desc 캐시 이름별 TTL 차등 정책 맵을 구성합니다.
     */
    private Map<String, RedisCacheConfiguration> createCacheConfigurations() {
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(CACHE_WEEKLY_TOP10, createDefaultRedisCacheConfiguration(Duration.ofMinutes(20)));
        cacheConfigurations.put(CACHE_WEEKLY_TOP5, createDefaultRedisCacheConfiguration(Duration.ofMinutes(20)));
        cacheConfigurations.put(CACHE_INSIGHTS_BY_DATE, createDefaultRedisCacheConfiguration(Duration.ofMinutes(10)));
        cacheConfigurations.put(CACHE_INSIGHTS_BY_RANGE, createDefaultRedisCacheConfiguration(Duration.ofMinutes(5)));
        cacheConfigurations.put(CACHE_INSIGHT_ENGAGEMENT, createDefaultRedisCacheConfiguration(Duration.ofSeconds(90)));
        cacheConfigurations.put(CACHE_ADMIN_STATS, createDefaultRedisCacheConfiguration(Duration.ofMinutes(5)));
        cacheConfigurations.put(CACHE_ADMIN_CONTENT_VIEW_STATS, createDefaultRedisCacheConfiguration(Duration.ofMinutes(5)));
        cacheConfigurations.put(CACHE_ADMIN_BOOKMARK_STATS, createDefaultRedisCacheConfiguration(Duration.ofMinutes(5)));
        return cacheConfigurations;
    }
}
