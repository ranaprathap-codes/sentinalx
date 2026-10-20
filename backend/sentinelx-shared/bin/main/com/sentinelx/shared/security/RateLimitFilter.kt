package com.sentinelx.shared.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration

/**
 * Sliding-window rate limiter for sensitive endpoints (auth login/register).
 * Gracefully permits requests if Redis is temporarily unreachable.
 */
@Component
class RateLimitFilter(
    private val stringRedisTemplate: StringRedisTemplate?
) : OncePerRequestFilter() {

    companion object {
        private const val MAX_REQUESTS = 15
        private val WINDOW = Duration.ofMinutes(1)
        private val RATE_LIMITED_PATHS = setOf("/api/auth/login", "/api/auth/register")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI
        if (path in RATE_LIMITED_PATHS && stringRedisTemplate != null) {
            try {
                val ip = getClientIp(request)
                val key = "rate_limit:$path:$ip"
                val ops = stringRedisTemplate.opsForValue()
                val current = ops.increment(key) ?: 1L
                if (current == 1L) {
                    stringRedisTemplate.expire(key, WINDOW)
                }
                if (current > MAX_REQUESTS) {
                    response.status = 429
                    response.contentType = "application/json"
                    response.writer.write(
                        """{"success":false,"error":{"code":"RATE_LIMITED","message":"Too many requests. Please try again later."}}"""
                    )
                    return
                }
                response.setHeader("X-RateLimit-Limit", MAX_REQUESTS.toString())
                response.setHeader("X-RateLimit-Remaining", maxOf(0, MAX_REQUESTS - current.toInt()).toString())
            } catch (e: Exception) {
                // If Redis is down during dev/test, fail open so authentication still works
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun getClientIp(request: HttpServletRequest): String {
        return request.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
            ?: request.remoteAddr
    }
}

