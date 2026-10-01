package com.sentinelx.dryrun.engine

import com.sentinelx.dryrun.domain.DryRun
import com.sentinelx.dryrun.dto.DryRunDiffSummary
import com.sentinelx.dryrun.dto.ScoreChangeSummary
import com.sentinelx.dryrun.dto.TransactionComparison
import com.sentinelx.payments.domain.Transaction
import com.sentinelx.payments.repository.TransactionRepository
import com.sentinelx.risk.domain.RiskDecision
import com.sentinelx.risk.domain.RiskRule
import com.sentinelx.risk.engine.DeterministicRiskEngine
import com.sentinelx.risk.repository.RiskRuleRepository
import com.sentinelx.shared.domain.RiskDecision
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DryRunEngine(
    private val transactionRepository: TransactionRepository,
    private val riskRuleRepository: RiskRuleRepository,
    private val deterministicRiskEngine: DeterministicRiskEngine,
    private val objectMapper: ObjectMapper
) {

    data class DryRunContext(
        val dryRun: DryRun,
        val baselineRules: List<RiskRule>,
        val proposedRules: List<RiskRule>,
        val testTransactions: List<Transaction>
    )

    fun executeDryRun(dryRun: DryRun): DryRunDiffSummary {
        // Load baseline rules (at the specified version)
        val baselineRules = loadBaselineRules(dryRun.baselineRuleVersion)
        
        // Parse proposed rule
        val proposedRule = parseProposedRule(dryRun.proposedRuleJson)
        
        // Get transactions to test (recent transactions, or from report if linked)
        val testTransactions = getTestTransactions(dryRun)
        
        val context = DryRunContext(
            dryRun = dryRun,
            baselineRules = baselineRules,
            proposedRules = listOf(proposedRule) + baselineRules.filter { it.id != proposedRule.id },
            testTransactions = testTransactions
        )

        // Run baseline evaluation
        val baselineResults = evaluateTransactions(context.testTransactions, context.baselineRules)
        
        // Run proposed evaluation
        val proposedResults = evaluateTransactions(context.testTransactions, context.proposedRules)

        // Compare results
        val comparison = compareResults(baselineResults, proposedResults)
        
        // Calculate summary
        val summary = calculateSummary(comparison)

        // Update dry run entity
        dryRun.baselineResultJson = objectMapper.writeValueAsString(baselineResults.map { it.toJson() })
        dryRun.proposedResultJson = objectMapper.writeValueAsString(proposedResults.map { it.toJson() })
        dryRun.diffJson = objectMapper.writeValueAsString(comparison.map { it.toJson() })
        dryRun.transactionsTested = testTransactions.size
        dryRun.falsePositiveDelta = summary.falsePositiveDelta
        dryRun.falseNegativeDelta = summary.falseNegativeDelta
        dryRun.latencyDeltaMs = summary.avgLatencyDeltaMs
        dryRun.status = com.sentinelx.shared.domain.DryRunStatus.COMPLETED
        dryRun.completedAt = java.time.Instant.now()

        return summary
    }

    private fun loadBaselineRules(version: Int): List<RiskRule> {
        // In a real implementation, this would load rules from a versioned store
        // For now, return current enabled rules
        return riskRuleRepository.findByEnabledTrue()
    }

    private fun parseProposedRule(json: String): RiskRule {
        return objectMapper.readValue(json, RiskRule::class.java)
    }

    private fun getTestTransactions(dryRun: DryRun): List<Transaction> {
        if (dryRun.reportId != null) {
            // If linked to a report, get related transactions
            return transactionRepository.findAll().filter { tx ->
                dryRun.reportId!! in (tx.id::class.java) // Simplified - would use related_transaction_ids
            }
        }
        // Default: last 1000 transactions
        return transactionRepository.findAll().take(1000)
    }

    private fun evaluateTransactions(
        transactions: List<Transaction>,
        rules: List<RiskRule>
    ): List<EvaluationResult> {
        return transactions.map { tx ->
            val request = com.sentinelx.risk.dto.RiskEvaluationRequest(
                transactionId = tx.id,
                amountCents = tx.amountCents,
                merchantId = tx.merchantId,
                deviceId = tx.deviceId,
                deviceFingerprint = tx.deviceFingerprint,
                locationCountry = tx.locationCountry,
                locationRegion = tx.locationRegion,
                locationCity = tx.locationCity,
                userTransactionCount1h = 0, // Would compute from history
                userTransactionCount24h = 0,
                userAvgAmountCents = tx.amountCents,
                userMaxAmountCents = tx.amountCents,
                isNewDevice = false,
                isNewLocation = false,
                timeSinceLastTxMinutes = null
            )
            val result = deterministicRiskEngine.evaluate(request, rules)
            EvaluationResult(
                transactionId = tx.id,
                score = result.score,
                decision = result.decision,
                evaluationDurationMs = result.evaluationDurationMs
            )
        }
    }

    private fun compareResults(
        baseline: List<EvaluationResult>,
        proposed: List<EvaluationResult>
    ): List<TransactionComparison> {
        val proposedMap = proposed.associateBy { it.transactionId }
        
        return baseline.map { base ->
            val prop = proposedMap[base.transactionId]!!
            TransactionComparison(
                transactionId = base.transactionId,
                baselineScore = base.score,
                proposedScore = prop.score,
                baselineDecision = base.decision.name,
                proposedDecision = prop.decision.name,
                scoreDelta = prop.score - base.score,
                decisionChanged = base.decision != prop.decision
            )
        }
    }

    private fun calculateSummary(comparisons: List<TransactionComparison>): DryRunDiffSummary {
        val decisionChanges = comparisons
            .filter { it.decisionChanged }
            .groupBy { "${it.baselineDecision} -> ${it.proposedDecision}" }
            .mapValues { it.value.size }

        val scoreIncreased = comparisons.count { it.scoreDelta > 0 }
        val scoreDecreased = comparisons.count { it.scoreDelta < 0 }
        val scoreUnchanged = comparisons.count { it.scoreDelta == 0 }
        val avgScoreDelta = if (comparisons.isNotEmpty()) comparisons.map { it.scoreDelta.toDouble() }.average() else 0.0

        // False positive: baseline ALLOW, proposed BLOCK/REVIEW
        val falsePositiveDelta = comparisons.count { 
            it.baselineDecision == "ALLOW" && it.proposedDecision in setOf("BLOCK", "REVIEW") 
        }
        
        // False negative: baseline BLOCK/REVIEW, proposed ALLOW
        val falseNegativeDelta = comparisons.count { 
            it.baselineDecision in setOf("BLOCK", "REVIEW") && it.proposedDecision == "ALLOW" 
        }

        val avgLatencyDelta = 0L // Would need to track evaluation duration differences

        return DryRunDiffSummary(
            totalTransactions = comparisons.size,
            decisionChanges = decisionChanges,
            scoreChanges = ScoreChangeSummary(
                increased = scoreIncreased,
                decreased = scoreDecreased,
                unchanged = scoreUnchanged,
                avgScoreDelta = avgScoreDelta
            ),
            falsePositiveDelta = falsePositiveDelta,
            falseNegativeDelta = falseNegativeDelta,
            avgLatencyDeltaMs = avgLatencyDelta,
            affectedTransactionIds = comparisons.filter { it.decisionChanged }.map { it.transactionId }
        )
    }

    private data class EvaluationResult(
        val transactionId: UUID,
        val score: Int,
        val decision: RiskDecision,
        val evaluationDurationMs: Long
    ) {
        fun toJson(): ObjectNode {
            return objectMapper.createObjectNode().apply {
                put("transactionId", transactionId.toString())
                put("score", score)
                put("decision", decision.name)
                put("evaluationDurationMs", evaluationDurationMs)
            }
        }
    }
}