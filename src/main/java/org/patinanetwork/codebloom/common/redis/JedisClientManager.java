package org.patinanetwork.codebloom.common.redis;

import lombok.Getter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.UnifiedJedis;

@Component
@EnableConfigurationProperties(JedisClientConfiguration.class)
public class JedisClientManager {
    @Getter
    private UnifiedJedis client;

    public JedisClientManager(JedisClientConfiguration jedisClientConfiguration) {
        client = RedisClient.create(jedisClientConfiguration.getUrl());
    }
}
