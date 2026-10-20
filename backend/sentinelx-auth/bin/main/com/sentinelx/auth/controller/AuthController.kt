package com.sentinelx.auth.controller

import com.sentinelx.auth.dto.AuthResponse
import com.sentinelx.auth.dto.LoginRequest
import com.sentinelx.auth.dto.RefreshTokenRequest
import com.sentinelx.auth.dto.RegisterRequest
import com.sentinelx.auth.dto.UserDto
import com.sentinelx.auth.service.AuthService
import com.sentinelx.shared.kernel.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication and user management endpoints")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        val response = authService.register(request)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and return tokens")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        val response = authService.login(request)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    fun refreshToken(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        val response = authService.refreshToken(request)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user")
    fun getCurrentUser(authentication: Authentication): ResponseEntity<ApiResponse<UserDto>> {
        val user = authService.getCurrentUser(authentication)
        return ResponseEntity.ok(ApiResponse.ok(user))
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout (client-side token invalidation)")
    fun logout(): ResponseEntity<ApiResponse<Unit>> {
        return ResponseEntity.ok(ApiResponse.ok(Unit))
    }
}