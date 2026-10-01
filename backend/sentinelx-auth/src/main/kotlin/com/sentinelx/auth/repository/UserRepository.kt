package com.sentinelx.auth.repository

import com.sentinelx.auth.domain.User
import com.sentinelx.shared.domain.UserRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserRepository : JpaRepository<User, UUID> {
    fun findByEmail(email: String): User?
    fun existsByEmail(email: String): Boolean
    
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isActive = true")
    fun findByRoleAndActive(role: UserRole): List<User>

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.isActive = true")
    fun findActiveByEmail(@Param("email") email: String): User?
}