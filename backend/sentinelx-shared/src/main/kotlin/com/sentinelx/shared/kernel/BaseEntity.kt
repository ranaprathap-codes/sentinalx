package com.sentinelx.shared.kernel

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import java.time.Instant
import java.util.UUID

@MappedSuperclass
abstract class BaseEntity(
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "UUID")
    var id: UUID = UUID.randomUUID(),

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    var updatedAt: Instant = Instant.now()
) {
    @PrePersist
    fun prePersist() {
        createdAt = Instant.now()
        updatedAt = Instant.now()
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = Instant.now()
    }
}