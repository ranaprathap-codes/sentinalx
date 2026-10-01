package com.sentinelx.shared.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig {

    @Bean
    fun redisTemplate(
        connectionFactory: RedisConnectionFactory,
        objectMapper: ObjectMapper
    ): RedisTemplate<String, String> {
        val template = RedisTemplate<String, String>()

        template.connectionFactory = connectionFactory
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer =
            GenericJackson2JsonRedisSerializer(objectMapper)
        template.hashKeySerializer = StringRedisSerializer()
        template.hashValueSerializer =
            GenericJackson2JsonRedisSerializer(objectMapper)

        template.afterPropertiesSet()

        return template
    }

    @Bean
    fun objectMapper(): ObjectMapper {
        return ObjectMapper().apply {
            registerModule(
                KotlinModule.Builder().build()
            )
            registerModule(JavaTimeModule())
        }
    }
}