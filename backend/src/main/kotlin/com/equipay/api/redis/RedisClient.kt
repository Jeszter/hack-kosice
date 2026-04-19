package com.equipay.api.redis

import com.equipay.api.config.RedisConfig
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.net.URI

class RedisClient(cfg: RedisConfig) {
    private val pool: JedisPool

    init {
        val uri = URI(cfg.url)
        pool = JedisPool(JedisPoolConfig(), uri)
    }

    fun setEx(key: String, ttlSeconds: Long, value: String) {
        pool.resource.use { it.setex(key, ttlSeconds, value) }
    }

    fun get(key: String): String? = pool.resource.use { it.get(key) }

    fun del(key: String) {
        pool.resource.use { it.del(key) }
    }

    fun incrWithTtl(key: String, ttlSeconds: Long): Long {
        return pool.resource.use { jedis ->
            val v = jedis.incr(key)
            if (v == 1L) jedis.expire(key, ttlSeconds)
            v
        }
    }

    fun close() = pool.close()
}
