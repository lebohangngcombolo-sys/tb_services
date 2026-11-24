package com.tradebridge.country.config;

import com.tradebridge.country.cache.CountryConfigCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Per-cache Redis TTLs and JSON serialization.
 *
 * <p>Uses JSON values so that cache entries are inspectable and
 * forward-compatible across restarts. Each cache gets its own TTL
 * aligned with {@code tradebridge.country.cache.ttl}.
 */
@Configuration
public class RedisCacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory,
                                          CountryProperties properties) {
        Duration ttl = properties.getCache().getTtl();
        GenericJackson2JsonRedisSerializer valueSerializer =
                new GenericJackson2JsonRedisSerializer();
        StringRedisSerializer keySerializer = new StringRedisSerializer();

        RedisCacheConfiguration baseConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(keySerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(valueSerializer))
                .disableCachingNullValues()
                .prefixCacheNameWith("tradebridge:country:");

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(baseConfig)
                .withCacheConfiguration(CountryConfigCache.CACHE_ACTIVE_CONFIG,
                        baseConfig.entryTtl(ttl))
                .withCacheConfiguration(CountryConfigCache.CACHE_KYC_REQUIREMENTS,
                        baseConfig.entryTtl(ttl))
                .withCacheConfiguration(CountryConfigCache.CACHE_ORG_REQUIREMENTS,
                        baseConfig.entryTtl(ttl))
                .build();
    }
}