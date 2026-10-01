package com.sentinelx.risk.dto

import com.sentinelx.shared.domain.RiskDecision
import jakarta.validation.constraints.*
import java.util.UUID

data class RiskRuleRequest(
    @NotBlank @Size(max = 255) val name: String,
    @Size(max = 1000) val description: String? = null,
    @NotBlank val conditionJson: String,
    @Min(0) @Max(100) val weight: Int = 10,
    val enabled: Boolean = true
)

data class RiskRuleResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val conditionJson: String,
    val weight: Int,
    val enabled: Boolean,
    val version: Int,
    val createdAt: String,
    val updatedAt: String
)

data class RiskEvaluationRequest(
    val transactionId: UUID,
    val amountCents: Long,
    val merchantId: String,
    val deviceId: String?,
    val deviceFingerprint: String?,
    val locationCountry: String?,
    val locationRegion: String?,
    val locationCity: String?,
    val userTransactionCount1h: Int,
    val userTransactionCount24h: Int,
    val userAvgAmountCents: Long,
    val userMaxAmountCents: Long,
    val isNewDevice: Boolean,
    val isNewLocation: Boolean,
    val timeSinceLastTxMinutes: Long?
)

data class TriggeredRuleDto(
    val ruleId: UUID,
    val ruleName: String,
    val weight: Int,
    val matched: Boolean,
    val score: Int
)

data class RiskEvaluationResponse(
    val id: UUID,
    val transactionId: UUID,
    val score: Int,
    val decision: RiskDecision,
    val triggeredRules: List<TriggeredRuleDto>,
    val ruleScores: Map<String, Int>,
    val mlScore: Double?,
    val mlModelVersion: String?,
    val evaluationDurationMs: Long?,
    val explanation: String?,
    val createdAt: String
)

data class EvaluateRiskRequest(
    @NotNull val transactionId: UUID,
    val forceReevaluation: Boolean = false
)