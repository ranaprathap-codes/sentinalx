package com.sentinelx.shared.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${sentinelx.jwt.secret}")
    private val secret: String,

    @Value("\${sentinelx.jwt.expiration-ms:86400000}")
    private val expirationMs: Long,

    @Value("\${sentinelx.jwt.refresh-expiration-ms:604800000}")
    private val refreshExpirationMs: Long
) {

    private lateinit var key: SecretKey

    @PostConstruct
    fun init() {
        key = Keys.hmacShaKeyFor(
            secret.toByteArray(StandardCharsets.UTF_8)
        )
    }

    fun generateAccessToken(
        authentication: Authentication
    ): String {
        val authorities = authentication.authorities
            .map { it.authority }
            .joinToString(",")

        val now = Instant.now()
        val expiry = now.plusMillis(expirationMs)

        return Jwts.builder()
            .subject(authentication.name)
            .claim("authorities", authorities)
            .claim("type", "access")
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(key)
            .compact()
    }

    fun generateRefreshToken(
        username: String
    ): String {
        val now = Instant.now()
        val expiry = now.plusMillis(refreshExpirationMs)

        return Jwts.builder()
            .subject(username)
            .claim("type", "refresh")
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(key)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)

            true
        } catch (e: Exception) {
            false
        }
    }

    fun getUsername(token: String): String {
        return getClaims(token).subject
    }

    fun getAuthorities(
        token: String
    ): List<GrantedAuthority> {
        val authorities = getClaims(token)
            .get("authorities", String::class.java)
            ?: ""

        return authorities
            .split(",")
            .filter { it.isNotBlank() }
            .map { SimpleGrantedAuthority(it) }
    }

    fun getTokenType(token: String): String {
        return getClaims(token)
            .get("type", String::class.java)
            ?: "access"
    }

    fun isAccessToken(token: String): Boolean =
        getTokenType(token) == "access"

    fun isRefreshToken(token: String): Boolean =
        getTokenType(token) == "refresh"

    fun getExpiration(token: String): Instant {
        return getClaims(token).expiration.toInstant()
    }

    private fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    fun getAuthentication(
        token: String
    ): UsernamePasswordAuthenticationToken {
        val username = getUsername(token)
        val authorities = getAuthorities(token)

        return UsernamePasswordAuthenticationToken(
            username,
            null,
            authorities
        )
    }
}