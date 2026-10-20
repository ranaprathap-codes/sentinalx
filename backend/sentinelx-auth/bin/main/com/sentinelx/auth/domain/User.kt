package com.sentinelx.auth.domain

import com.sentinelx.shared.domain.UserRole
import com.sentinelx.shared.kernel.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_users_email", columnList = "email"),
        Index(name = "idx_users_role", columnList = "role")
    ]
)
class User(
    email: String,
    passwordHash: String,
    fullName: String? = null,
    role: UserRole = UserRole.DEVELOPER
) : BaseEntity() {

    @Column(name = "email", nullable = false, unique = true, length = 255)
    var email: String = email

    @Column(name = "password_hash", nullable = false, length = 255)
    var passwordHash: String = passwordHash

    @Column(name = "full_name", length = 255)
    var fullName: String? = fullName

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    var role: UserRole = role

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true

    @Column(name = "email_verified", nullable = false)
    var emailVerified: Boolean = false

    @Column(name = "last_login_at")
    var lastLoginAt: Instant? = null

    constructor() : this("", "")
}