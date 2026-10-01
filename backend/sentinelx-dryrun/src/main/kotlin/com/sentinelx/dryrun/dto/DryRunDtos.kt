package com.sentinelx.dryrun.dto

import com.sentinelx.shared.domain.DryRunStatus
import jakarta.validation.constraints.*
import java.util.UUID

data class CreateDryRunRequest(
    @NotBlank @Size(max = 255) val name: String,
    val description: String? = null,
    val reportId: UUID? = null,
    @NotNull val baselineRuleVersion: Int,
    @NotBlank val proposedRuleJson: String
)

data class DryRunResponse(
    val id: UUID,
    val reportId: UUID?,
    val name: String,
    val description: String?,
    val baselineRuleVersion: Int,
    val proposedRuleJson: String,
    val baselineResultJson: String?,
    val proposedResultJson: String?,
    val diffJson: String?,
    val transactionsTested: Int,
    val falsePositiveDelta: Int,
    val falseNegativeDelta: Int,
    val latencyDeltaMs: Long,
    val status: DryRunStatus,
    val createdBy: UUID,
    val createdAt: String,
    val completedAt: String?
)

data class DryRunListResponse(
    val dryRuns: List<DryRunResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class DryRunDiffSummary(
    val totalTransactions: Int,
    val decisionChanges: Map<String, Int>,
    val scoreChanges: ScoreChangeSummary,
    val falsePositiveDelta: Int,
    val falseNegativeDelta: Int,
    val avgLatencyDeltaMs: Long,
    val affectedTransactionIds: List<UUID>
)

data class ScoreChangeSummary(
    val increased: Int,
    val decreased: Int,
    val unchanged: Int,
    val avgScoreDelta: Double
)

data class TransactionComparison(
    val transactionId: UUID,
    val baselineScore: Int,
    val proposedScore: Int,
    val baselineDecision: String,
    val proposedDecision: String,
    val scoreDelta: Int,
    val decisionChanged: Boolean
)