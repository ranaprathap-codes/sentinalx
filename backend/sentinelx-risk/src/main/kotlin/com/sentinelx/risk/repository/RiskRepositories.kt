package com.sentinelx.risk.repository

import com.sentinelx.risk.domain.RiskEvaluation
import com.sentinelx.risk.domain.RiskRule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.List
import java.util.UUID

@Repository
interface RiskRuleRepository : JpaRepository<RiskRule, UUID> {
    fun findByEnabledTrue(): List<RiskRule>
    fun findByName(name: String): RiskRule?
}

@Repository
interface RiskEvaluationRepository : JpaRepository<RiskEvaluation, UUID> {
    fun findByTransactionId(transactionId: UUID): RiskEvaluation?
}