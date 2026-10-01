package com.sentinelx.community.domain

import com.sentinelx.shared.domain.ReportSeverity
import com.sentinelx.shared.domain.ReportStatus
import com.sentinelx.shared.domain.ReportType
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
    name = "problem_reports",
    indexes = [
        Index(name = "idx_problem_reports_type", columnList = "type"),
        Index(name = "idx_problem_reports_status", columnList = "status"),
        Index(name = "idx_problem_reports_severity", columnList = "severity"),
        Index(name = "idx_problem_reports_component", columnList = "component"),
        Index(name = "idx_problem_reports_reporter_id", columnList = "reporter_id"),
        Index(name = "idx_problem_reports_created_at", columnList = "created_at"),
        Index(name = "idx_problem_reports_tags", columnList = "tags", columnDefinition = "TEXT[]")
    ]
)
class ProblemReport(
    type: ReportType,
    title: String,
    description: String,
    reporterId: UUID
) : BaseEntity() {

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    var type: ReportType = type

    @Column(name = "title", nullable = false, length = 500)
    var title: String = title

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    var description: String = description

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 20)
    var severity: ReportSeverity? = null

    @Column(name = "component", length = 255)
    var component: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: ReportStatus = ReportStatus.OPEN

    @Column(name = "reporter_id", nullable = false)
    var reporterId: UUID = reporterId

    @Column(name = "assignee_id")
    var assigneeId: UUID? = null

    @Column(name = "evidence_json", columnDefinition = "JSONB")
    var evidenceJson: String = "{}"

    @Column(name = "reproduction_steps", columnDefinition = "TEXT")
    var reproductionSteps: String? = null

    @Column(name = "expected_behavior", columnDefinition = "TEXT")
    var expectedBehavior: String? = null

    @Column(name = "actual_behavior", columnDefinition = "TEXT")
    var actualBehavior: String? = null

    @Column(name = "tags", columnDefinition = "TEXT[]")
    var tags: Array<String> = emptyArray()

    @Column(name = "related_transaction_ids", columnDefinition = "UUID[]")
    var relatedTransactionIds: Array<UUID> = emptyArray()

    @Column(name = "related_simulation_ids", columnDefinition = "UUID[]")
    var relatedSimulationIds: Array<UUID> = emptyArray()

    @Column(name = "resolution_summary", columnDefinition = "TEXT")
    var resolutionSummary: String? = null

    @Column(name = "resolved_at")
    var resolvedAt: Instant? = null

    constructor() : this(ReportType.FLAW_REPORT, "", "", UUID.randomUUID())
}