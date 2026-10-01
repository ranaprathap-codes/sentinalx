package com.sentinelx.threatlab.domain

import com.sentinelx.shared.domain.ScenarioCategory
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
    name = "threat_scenarios",
    indexes = [
        Index(name = "idx_threat_scenarios_category", columnList = "category"),
        Index(name = "idx_threat_scenarios_enabled", columnList = "enabled")
    ]
)
class ThreatScenario(
    name: String,
    category: ScenarioCategory,
    inputTemplate: String,
    expectedResult: String,
    description: String? = null
) : BaseEntity() {

    @Column(name = "name", nullable = false, length = 255)
    var name: String = name

    @Column(name = "description", length = 2000)
    var description: String? = description

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    var category: ScenarioCategory = category

    @Column(name = "input_template", nullable = false, columnDefinition = "JSONB")
    var inputTemplate: String = inputTemplate

    @Column(name = "expected_result", nullable = false, columnDefinition = "JSONB")
    var expectedResult: String = expectedResult

    @Column(name = "tags", columnDefinition = "TEXT[]")
    var tags: Array<String> = emptyArray()

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = true

    @Column(name = "created_by")
    var createdBy: UUID? = null

    constructor() : this("", ScenarioCategory.CUSTOM, "{}", "{}")
}