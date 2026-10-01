package com.sentinelx.risk.engine

import com.sentinelx.risk.domain.RiskRule
import com.sentinelx.risk.dto.RiskEvaluationRequest
import com.sentinelx.risk.dto.TriggeredRuleDto
import com.sentinelx.shared.domain.RiskDecision
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class DeterministicRiskEngine(
    private val objectMapper: ObjectMapper
) {

    data class EvaluationContext(
        val transactionId: java.util.UUID,
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

    data class RuleEvaluationResult(
        val rule: RiskRule,
        val matched: Boolean,
        val score: Int
    )

    fun evaluate(request: RiskEvaluationRequest, rules: List<RiskRule>): EvaluationResult {
        val startTime = System.currentTimeMillis()
        
        val context = EvaluationContext(
            transactionId = request.transactionId,
            amountCents = request.amountCents,
            merchantId = request.merchantId,
            deviceId = request.deviceId,
            deviceFingerprint = request.deviceFingerprint,
            locationCountry = request.locationCountry,
            locationRegion = request.locationRegion,
            locationCity = request.locationCity,
            userTransactionCount1h = request.userTransactionCount1h,
            userTransactionCount24h = request.userTransactionCount24h,
            userAvgAmountCents = request.userAvgAmountCents,
            userMaxAmountCents = request.userMaxAmountCents,
            isNewDevice = request.isNewDevice,
            isNewLocation = request.isNewLocation,
            timeSinceLastTxMinutes = request.timeSinceLastTxMinutes
        )

        val enabledRules = rules.filter { it.enabled }
        val results = enabledRules.map { rule ->
            evaluateRule(rule, context)
        }

        val triggeredRules = results.filter { it.matched }
        val totalScore = calculateTotalScore(triggeredRules)
        val decision = determineDecision(totalScore)
        val ruleScores = triggeredRules.associateBy({ it.rule.id.toString() }, { it.score })
        val triggeredRulesDto = triggeredRules.map { 
            TriggeredRuleDto(
                ruleId = it.rule.id,
                ruleName = it.rule.name,
                weight = it.rule.weight,
                matched = true,
                score = it.score
            )
        }

        val evaluationDurationMs = System.currentTimeMillis() - startTime
        val explanation = generateExplanation(triggeredRules, totalScore, decision)

        return EvaluationResult(
            score = totalScore,
            decision = decision,
            triggeredRules = triggeredRulesDto,
            ruleScores = ruleScores,
            evaluationDurationMs = evaluationDurationMs,
            explanation = explanation
        )
    }

    private fun evaluateRule(rule: RiskRule, context: EvaluationContext): RuleEvaluationResult {
        return try {
            val condition = objectMapper.readTree(rule.conditionJson)
            val matched = evaluateCondition(condition, context)
            val score = if (matched) rule.weight else 0
            RuleEvaluationResult(rule, matched, score)
        } catch (e: Exception) {
            RuleEvaluationResult(rule, false, 0)
        }
    }

    private fun evaluateCondition(node: com.fasterxml.jackson.databind.JsonNode, context: EvaluationContext): Boolean {
        return when (node["operator"]?.asText()) {
            "AND" -> node["conditions"]?.elements()?.all { evaluateCondition(it, context) } == true
            "OR" -> node["conditions"]?.elements()?.any { evaluateCondition(it, context) } == true
            "NOT" -> !evaluateCondition(node["condition"]!!, context)
            else -> evaluateLeafCondition(node, context)
        }
    }

    private fun evaluateLeafCondition(node: com.fasterxml.jackson.databind.JsonNode, context: EvaluationContext): Boolean {
        val field = node["field"]?.asText() ?: return false
        val operator = node["operator"]?.asText() ?: return false
        val value = node["value"]

        return when (field) {
            "amountCents" -> compareNumeric(context.amountCents, operator, value?.asLong() ?: 0L)
            "userTransactionCount1h" -> compareNumeric(context.userTransactionCount1h.toLong(), operator, value?.asLong() ?: 0L)
            "userTransactionCount24h" -> compareNumeric(context.userTransactionCount24h.toLong(), operator, value?.asLong() ?: 0L)
            "userAvgAmountCents" -> compareNumeric(context.userAvgAmountCents, operator, value?.asLong() ?: 0L)
            "userMaxAmountCents" -> compareNumeric(context.userMaxAmountCents, operator, value?.asLong() ?: 0L)
            "isNewDevice" -> context.isNewDevice == (value?.asBoolean() ?: false)
            "isNewLocation" -> context.isNewLocation == (value?.asBoolean() ?: false)
            "timeSinceLastTxMinutes" -> context.timeSinceLastTxMinutes?.let { compareNumeric(it, operator, value?.asLong() ?: 0L) } ?: false
            "merchantId" -> context.merchantId == value?.asText()
            "locationCountry" -> context.locationCountry == value?.asText()
            else -> false
        }
    }

    private fun compareNumeric(actual: Long, operator: String, expected: Long): Boolean {
        return when (operator) {
            ">" -> actual > expected
            ">=" -> actual >= expected
            "<" -> actual < expected
            "<=" -> actual <= expected
            "==" -> actual == expected
            "!=" -> actual != expected
            else -> false
        }
    }

    private fun calculateTotalScore(triggeredRules: List<RuleEvaluationResult>): Int {
        // Weighted sum, capped at 100
        val rawScore = triggeredRules.sumOf { it.score }
        return rawScore.coerceAtMost(100)
    }

    private fun determineDecision(score: Int): RiskDecision {
        return when {
            score >= 70 -> RiskDecision.BLOCK
            score >= 40 -> RiskDecision.REVIEW
            else -> RiskDecision.ALLOW
        }
    }

    private fun generateExplanation(
        triggeredRules: List<RuleEvaluationResult>,
        totalScore: Int,
        decision: RiskDecision
    ): String {
        val sb = StringBuilder()
        sb.append("Risk Score: $totalScore/100\n\n")
        sb.append("Decision: ${decision.name}\n\n")
        
        if (triggeredRules.isNotEmpty()) {
            sb.append("Triggered Rules:\n")
            triggeredRules.forEach { result ->
                sb.append("✓ ${result.rule.name} (weight: ${result.rule.weight})\n")
            }
        } else {
            sb.append("No rules triggered.\n")
        }
        
        return sb.toString()
    }

    data class EvaluationResult(
        val score: Int,
        val decision: RiskDecision,
        val triggeredRules: List<TriggeredRuleDto>,
        val ruleScores: Map<String, Int>,
        val evaluationDurationMs: Long,
        val explanation: String
    )
}