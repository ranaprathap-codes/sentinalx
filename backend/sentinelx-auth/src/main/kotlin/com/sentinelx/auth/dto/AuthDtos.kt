package com.sentinelx.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @Email @NotBlank val email: String,
    @NotBlank @Size(min = 8, max = 128) val password: String,
    @Size(max = 255) val fullName: String? = null
)

data class LoginRequest(
    @Email @NotBlank val email: String,
    @NotBlank val password: String
)

data class RefreshTokenRequest(
    @NotBlank val refreshToken: String
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val user: UserDto
)

data class UserDto(
    val id: String,
    val email: String,
    val fullName: String?,
    val role: String,
    val isActive: Boolean,
    val emailVerified: Boolean
)

data class UpdateProfileRequest(
    @Size(max = 255) val fullName: String? = null,
    @Size(max = 128) val currentPassword: String? = null,
    @Size(min = 8, max = 128) val newPassword: String? = null
)

data class ChangePasswordRequest(
    @NotBlank val currentPassword: String,
    @NotBlank @Size(min = 8, max = 128) val newPassword: String
)

data class ForgotPasswordRequest(
    @Email @NotBlank val email: String
)

data class ResetPasswordRequest(
    @NotBlank val token: String,
    @NotBlank @Size(min = 8, max = 128) val newPassword: String
)