package com.anastasia.Anastasia_BackEnd.common.config;

import org.springframework.cache.Cache;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for Redis caching.
 * Uses JSON serialization instead of Java binary (for readability & compatibility).
 * Sets TTL so stale data expires automatically.
 * Ready for multi-tenant extension later (we can inject tenant ID in cache keys).
 */
@Configuration
@Profile("!test")
@EnableCaching
public class RedisCacheConfig {

    private GenericJackson2JsonRedisSerializer redisSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.activateDefaultTypingAsProperty(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.EVERYTHING,
                "@class"
        );
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new JavaTimeModule());
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // default TTL: 30 min
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(redisSerializer()));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("imageAssets", defaultConfig.entryTtl(Duration.ofHours(6)));
        cacheConfigurations.put("members", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("members_all", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("children", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("children_all", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("users", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("users_all", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("users_all_list", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("tenants", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("tenants_page", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("tenants_by_phone", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("events_visible", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("event_managers", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }


    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(redisSerializer());
        return template;
    }

    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                throw exception;
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                throw exception;
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                throw exception;
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                throw exception;
            }
        };
    }

}
