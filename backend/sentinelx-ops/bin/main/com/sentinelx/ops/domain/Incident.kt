package com.sentinelx.ops.domain

import com.sentinelx.shared.domain.IncidentSeverity
import com.sentinelx.shared.domain.IncidentStatus
import com.sentinelx.shared.domain.IncidentType
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
    name = "incidents",
    indexes = [
        Index(name = "idx_incidents_status", columnList = "status"),
        Index(name = "idx_incidents_severity", columnList = "severity"),
        Index(name = "idx_incidents_started_at", columnList = "started_at")
    ]
)
class Incident(
    type: IncidentType,
    severity: IncidentSeverity,
    title: String,
    evidenceJson: String
) : BaseEntity() {

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    var type: IncidentType = type

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 10)
    var severity: IncidentSeverity = severity

    @Column(name = "title", nullable = false, length = 500)
    var title: String = title

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: IncidentStatus = IncidentStatus.DETECTED

    @Column(name = "evidence_json", nullable = false, columnDefinition = "JSONB")
    var evidenceJson: String = evidenceJson

    @Column(name = "hypothesis", columnDefinition = "TEXT")
    var hypothesis: String? = null

    @Column(name = "probable_cause", columnDefinition = "TEXT")
    var probableCause: String? = null

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    var aiSummary: String? = null

    @Column(name = "affected_components", columnDefinition = "TEXT[]")
    var affectedComponents: Array<String> = emptyArray()

    @Column(name = "started_at", nullable = false)
    var startedAt: Instant = Instant.now()

    @Column(name = "detected_at", nullable = false)
    var detectedAt: Instant = Instant.now()

    @Column(name = "acknowledged_at")
    var acknowledgedAt: Instant? = null

    @Column(name = "resolved_at")
    var resolvedAt: Instant? = null

    @Column(name = "assignee_id")
    var assigneeId: UUID? = null

    constructor() : this(IncidentType.SERVICE_FAILURE, IncidentSeverity.SEV3, "", "{}")
}