package br.com.itau.recuperacao.renegociacao.infrastructure.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuração do Redis para cache e operações de template.
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * Configura o RedisTemplate com serialização String para chaves e valores.
     *
     * @param connectionFactory fábrica de conexões Redis
     * @return RedisTemplate configurado
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Configura o RedisCacheManager com TTLs diferenciados por cache:
     * propostas (5 min), simulacoes (10 min), dividas (2 min).
     *
     * @param connectionFactory fábrica de conexões Redis
     * @return RedisCacheManager configurado
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("propostas", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("simulacoes", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("dividas", defaultConfig.entryTtl(Duration.ofMinutes(2)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
