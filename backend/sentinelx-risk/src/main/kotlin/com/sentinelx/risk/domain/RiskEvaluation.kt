package com.sentinelx.risk.domain

import com.sentinelx.shared.domain.RiskDecision
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
    name = "risk_evaluations",
    indexes = [
        Index(name = "idx_risk_evaluations_transaction_id", columnList = "transaction_id"),
        Index(name = "idx_risk_evaluations_score", columnList = "score"),
        Index(name = "idx_risk_evaluations_created_at", columnList = "created_at")
    ]
)
class RiskEvaluation(
    transactionId: UUID,
    score: Int,
    decision: RiskDecision,
    triggeredRules: String,
    ruleScores: String
) : BaseEntity() {

    @Column(name = "transaction_id", nullable = false)
    var transactionId: UUID = transactionId

    @Column(name = "score", nullable = false)
    var score: Int = score

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 20)
    var decision: RiskDecision = decision

    @Column(name = "triggered_rules", nullable = false, columnDefinition = "JSONB")
    var triggeredRules: String = triggeredRules

    @Column(name = "rule_scores", nullable = false, columnDefinition = "JSONB")
    var ruleScores: String = ruleScores

    @Column(name = "ml_score", precision = 5, scale = 4)
    var mlScore: Double? = null

    @Column(name = "ml_model_version", length = 50)
    var mlModelVersion: String? = null

    @Column(name = "evaluation_duration_ms")
    var evaluationDurationMs: Long? = null

    @Column(name = "explanation", columnDefinition = "TEXT")
    var explanation: String? = null

    constructor() : this(UUID.randomUUID(), 0, RiskDecision.ALLOW, "[]", "{}")
}