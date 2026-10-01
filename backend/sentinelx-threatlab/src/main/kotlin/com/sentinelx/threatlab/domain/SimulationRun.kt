package com.sentinelx.threatlab.domain

import com.sentinelx.shared.domain.SimulationStatus
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
    name = "simulation_runs",
    indexes = [
        Index(name = "idx_simulation_runs_scenario_id", columnList = "scenario_id"),
        Index(name = "idx_simulation_runs_user_id", columnList = "user_id"),
        Index(name = "idx_simulation_runs_created_at", columnList = "created_at"),
        Index(name = "idx_simulation_runs_detected", columnList = "detected")
    ]
)
class SimulationRun(
    scenarioId: UUID,
    userId: UUID,
    inputJson: String,
    actualResult: String,
    expectedResult: String
) : BaseEntity() {

    @Column(name = "scenario_id", nullable = false)
    var scenarioId: UUID = scenarioId

    @Column(name = "user_id", nullable = false)
    var userId: UUID = userId

    @Column(name = "input_json", nullable = false, columnDefinition = "JSONB")
    var inputJson: String = inputJson

    @Column(name = "actual_result", nullable = false, columnDefinition = "JSONB")
    var actualResult: String = actualResult

    @Column(name = "expected_result", nullable = false, columnDefinition = "JSONB")
    var expectedResult: String = expectedResult

    @Column(name = "detected")
    var detected: Boolean? = null

    @Column(name = "detection_details", columnDefinition = "JSONB")
    var detectionDetails: String? = null

    @Column(name = "duration_ms")
    var durationMs: Long? = null

    @Column(name = "logs", columnDefinition = "JSONB")
    var logs: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: SimulationStatus = SimulationStatus.RUNNING

    @Column(name = "error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null

    constructor() : this(UUID.randomUUID(), UUID.randomUUID(), "{}", "{}", "{}")
}