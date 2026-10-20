package com.sentinelx.risk.domain

import com.sentinelx.shared.kernel.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "risk_rules",
    indexes = [Index(name = "idx_risk_rules_enabled", columnList = "enabled")]
)
class RiskRule(
    name: String,
    conditionJson: String,
    weight: Int = 10,
    description: String? = null
) : BaseEntity() {

    @Column(name = "name", nullable = false, unique = true, length = 255)
    var name: String = name

    @Column(name = "description", length = 1000)
    var description: String? = description

    @Column(name = "condition_json", nullable = false, columnDefinition = "JSONB")
    var conditionJson: String = conditionJson

    @Column(name = "weight", nullable = false)
    var weight: Int = weight

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = true

    @Column(name = "version", nullable = false)
    var version: Int = 1

    @Column(name = "created_by")
    var createdBy: UUID? = null

    constructor() : this("", "")
}