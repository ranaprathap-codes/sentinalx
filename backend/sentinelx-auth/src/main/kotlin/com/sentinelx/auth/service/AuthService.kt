package com.sentinelx.auth.service

import com.sentinelx.auth.domain.User
import com.sentinelx.auth.dto.AuthResponse
import com.sentinelx.auth.dto.LoginRequest
import com.sentinelx.auth.dto.RefreshTokenRequest
import com.sentinelx.auth.dto.RegisterRequest
import com.sentinelx.auth.dto.UserDto
import com.sentinelx.auth.repository.UserRepository
import com.sentinelx.shared.kernel.BadRequestException
import com.sentinelx.shared.kernel.ConflictException
import com.sentinelx.shared.kernel.UnauthorizedException
import com.sentinelx.shared.security.JwtTokenProvider
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val authenticationManager: AuthenticationManager
) {

    fun register(request: RegisterRequest): AuthResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw ConflictException("Email already registered")
        }

        val user = User(
            email = request.email.toLowerCase(),
            passwordHash = passwordEncoder.encode(request.password),
            fullName = request.fullName
        )

        val savedUser = userRepository.save(user)
        return buildAuthResponse(savedUser)
    }

    fun login(request: LoginRequest): AuthResponse {
        val authentication = try {
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(request.email.toLowerCase(), request.password)
            )
        } catch (e: Exception) {
            throw UnauthorizedException("Invalid email or password")
        }

        val user = userRepository.findActiveByEmail(request.email.toLowerCase())
            ?: throw UnauthorizedException("Invalid email or password")

        user.lastLoginAt = Instant.now()
        userRepository.save(user)

        return buildAuthResponse(user, authentication)
    }

    fun refreshToken(request: RefreshTokenRequest): AuthResponse {
        val token = request.refreshToken
        
        if (!jwtTokenProvider.validateToken(token) || !jwtTokenProvider.isRefreshToken(token)) {
            throw UnauthorizedException("Invalid refresh token")
        }

        val username = jwtTokenProvider.getUsername(token)
        val user = userRepository.findActiveByEmail(username)
            ?: throw UnauthorizedException("User not found or inactive")

        val authentication = jwtTokenProvider.getAuthentication(token)
        return buildAuthResponse(user, authentication)
    }

    fun getCurrentUser(authentication: Authentication): UserDto {
        val user = userRepository.findActiveByEmail(authentication.name)
            ?: throw UnauthorizedException("User not found")
        return toUserDto(user)
    }

    private fun buildAuthResponse(user: User, authentication: Authentication? = null): AuthResponse {
        val auth = authentication ?: UsernamePasswordAuthenticationToken(user.email, null, user.role.authority)
        val accessToken = jwtTokenProvider.generateAccessToken(auth)
        val refreshToken = jwtTokenProvider.generateRefreshToken(user.email)
        
        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = jwtTokenProvider.getExpiration(accessToken).toEpochMilli() - System.currentTimeMillis(),
            user = toUserDto(user)
        )
    }

    private fun toUserDto(user: User): UserDto = UserDto(
        id = user.id.toString(),
        email = user.email,
        fullName = user.fullName,
        role = user.role.name,
        isActive = user.isActive,
        emailVerified = user.emailVerified
    )
}