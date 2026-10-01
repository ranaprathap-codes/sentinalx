package com.sentinelx.dryrun.domain

import com.sentinelx.shared.domain.DryRunStatus
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
    name = "dry_runs",
    indexes = [
        Index(name = "idx_dry_runs_report_id", columnList = "report_id"),
        Index(name = "idx_dry_runs_status", columnList = "status"),
        Index(name = "idx_dry_runs_created_at", columnList = "created_at")
    ]
)
class DryRun(
    name: String,
    proposedRuleJson: String,
    baselineRuleVersion: Int,
    createdBy: UUID
) : BaseEntity() {

    @Column(name = "report_id")
    var reportId: UUID? = null

    @Column(name = "name", nullable = false, length = 255)
    var name: String = name

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null

    @Column(name = "baseline_rule_version", nullable = false)
    var baselineRuleVersion: Int = baselineRuleVersion

    @Column(name = "proposed_rule_json", nullable = false, columnDefinition = "JSONB")
    var proposedRuleJson: String = proposedRuleJson

    @Column(name = "baseline_result_json", columnDefinition = "JSONB")
    var baselineResultJson: String? = null

    @Column(name = "proposed_result_json", columnDefinition = "JSONB")
    var proposedResultJson: String? = null

    @Column(name = "diff_json", columnDefinition = "JSONB")
    var diffJson: String? = null

    @Column(name = "transactions_tested")
    var transactionsTested: Int = 0

    @Column(name = "false_positive_delta")
    var falsePositiveDelta: Int = 0

    @Column(name = "false_negative_delta")
    var falseNegativeDelta: Int = 0

    @Column(name = "latency_delta_ms")
    var latencyDeltaMs: Long = 0

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: DryRunStatus = DryRunStatus.PENDING

    @Column(name = "created_by", nullable = false)
    var createdBy: UUID = createdBy

    @Column(name = "completed_at")
    var completedAt: Instant? = null

    constructor() : this("", "", 0, UUID.randomUUID())
}