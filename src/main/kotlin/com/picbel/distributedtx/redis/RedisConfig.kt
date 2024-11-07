package com.picbel.distributedtx.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.client.codec.StringCodec
import org.redisson.codec.JsonJacksonCodec
import org.redisson.config.Config
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RedisConfig {

    @Bean
    fun redissonClient(
        @Value("\${spring.data.redis.host}") redisHost: String,
        @Value("\${spring.data.redis.port}") redisPort: Int
    ): RedissonClient {
        val config = Config()

        // Redis 서버 주소 설정
        config.useSingleServer().address = "redis://$redisHost:$redisPort"

        // JSON 직렬화를 위한 Jackson ObjectMapper 설정
        val objectMapper: ObjectMapper = jacksonObjectMapper()
        config.codec = JsonJacksonCodec(objectMapper)

        return Redisson.create(config)
    }

}
