package com.sentinelx.risk.service

import com.sentinelx.payments.domain.Transaction
import com.sentinelx.payments.repository.TransactionRepository
import com.sentinelx.risk.domain.RiskEvaluation
import com.sentinelx.risk.domain.RiskRule
import com.sentinelx.risk.dto.EvaluateRiskRequest
import com.sentinelx.risk.dto.RiskEvaluationResponse
import com.sentinelx.risk.dto.RiskRuleRequest
import com.sentinelx.risk.dto.RiskRuleResponse
import com.sentinelx.risk.dto.TriggeredRuleDto
import com.sentinelx.risk.engine.DeterministicRiskEngine
import com.sentinelx.risk.repository.RiskEvaluationRepository
import com.sentinelx.risk.repository.RiskRuleRepository
import com.sentinelx.shared.domain.RiskDecision
import com.sentinelx.shared.event.DomainEvent
import com.sentinelx.shared.event.EventPublisher
import com.sentinelx.shared.event.RiskEvaluationCompletedEvent
import com.sentinelx.shared.kernel.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class RiskService(
    private val riskRuleRepository: RiskRuleRepository,
    private val riskEvaluationRepository: RiskEvaluationRepository,
    private val transactionRepository: TransactionRepository,
    private val riskEngine: DeterministicRiskEngine,
    private val eventPublisher: EventPublisher
) {

    fun evaluateRisk(request: EvaluateRiskRequest): RiskEvaluationResponse {
        val transaction = transactionRepository.findById(request.transactionId)
            .orElseThrow { NotFoundException("Transaction not found: ${request.transactionId}") }

        // Check if already evaluated
        val existingEvaluation = riskEvaluationRepository.findByTransactionId(transaction.id)
        if (existingEvaluation != null && !request.forceReevaluation) {
            return toResponse(existingEvaluation)
        }

        // Build context from transaction and user history
        val context = buildEvaluationContext(transaction)
        
        // Get active rules
        val rules = riskRuleRepository.findByEnabledTrue()
        
        // Evaluate
        val result = riskEngine.evaluate(
            com.sentinelx.risk.dto.RiskEvaluationRequest(
                transactionId = transaction.id,
                amountCents = transaction.amountCents,
                merchantId = transaction.merchantId,
                deviceId = transaction.deviceId,
                deviceFingerprint = transaction.deviceFingerprint,
                locationCountry = transaction.locationCountry,
                locationRegion = transaction.locationRegion,
                locationCity = transaction.locationCity,
                userTransactionCount1h = context.userTransactionCount1h,
                userTransactionCount24h = context.userTransactionCount24h,
                userAvgAmountCents = context.userAvgAmountCents,
                userMaxAmountCents = context.userMaxAmountCents,
                isNewDevice = context.isNewDevice,
                isNewLocation = context.isNewLocation,
                timeSinceLastTxMinutes = context.timeSinceLastTxMinutes
            ),
            rules
        )

        // Save evaluation
        val evaluation = RiskEvaluation(
            transactionId = transaction.id,
            score = result.score,
            decision = result.decision,
            triggeredRules = com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(result.triggeredRules),
            ruleScores = com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(result.ruleScores)
        ).apply {
            evaluationDurationMs = result.evaluationDurationMs
            explanation = result.explanation
        }

        val savedEvaluation = riskEvaluationRepository.save(evaluation)

        // Publish event
        eventPublisher.publish(RiskEvaluationCompletedEvent(
            correlationId = transaction.correlationId,
            transactionId = transaction.id,
            riskScore = result.score,
            decision = result.decision.name,
            triggeredRules = result.triggeredRules.map { it.toJson() }
        ))

        return toResponse(savedEvaluation)
    }

    fun getEvaluation(transactionId: UUID): RiskEvaluationResponse {
        val evaluation = riskEvaluationRepository.findByTransactionId(transactionId)
            .orElseThrow { NotFoundException("Risk evaluation not found for transaction: $transactionId") }
        return toResponse(evaluation)
    }

    // Rule management
    fun createRule(request: RiskRuleRequest): RiskRuleResponse {
        val rule = RiskRule(
            name = request.name,
            conditionJson = request.conditionJson,
            weight = request.weight,
            description = request.description
        ).apply {
            enabled = request.enabled
        }
        return toRuleResponse(riskRuleRepository.save(rule))
    }

    fun updateRule(ruleId: UUID, request: RiskRuleRequest): RiskRuleResponse {
        val rule = riskRuleRepository.findById(ruleId)
            .orElseThrow { NotFoundException("Risk rule not found: $ruleId") }
        
        rule.name = request.name
        rule.description = request.description
        rule.conditionJson = request.conditionJson
        rule.weight = request.weight
        rule.enabled = request.enabled
        rule.version++
        
        return toRuleResponse(riskRuleRepository.save(rule))
    }

    fun getRule(ruleId: UUID): RiskRuleResponse {
        val rule = riskRuleRepository.findById(ruleId)
            .orElseThrow { NotFoundException("Risk rule not found: $ruleId") }
        return toRuleResponse(rule)
    }

    fun listRules(): List<RiskRuleResponse> {
        return riskRuleRepository.findAll().map(::toRuleResponse)
    }

    fun deleteRule(ruleId: UUID) {
        riskRuleRepository.deleteById(ruleId)
    }

    // Default rules initialization
    fun initializeDefaultRules() {
        val defaultRules = listOf(
            RiskRuleRequest(
                name = "Unusual Amount",
                description = "Transaction amount significantly higher than user's average",
                conditionJson = """{"operator": ">", "field": "amountCents", "value": 500000}""",
                weight = 25
            ),
            RiskRuleRequest(
                name = "High Velocity 1h",
                description = "More than 10 transactions in the last hour",
                conditionJson = """{"operator": ">", "field": "userTransactionCount1h", "value": 10}""",
                weight = 30
            ),
            RiskRuleRequest(
                name = "High Velocity 24h",
                description = "More than 50 transactions in the last 24 hours",
                conditionJson = """{"operator": ">", "field": "userTransactionCount24h", "value": 50}""",
                weight = 20
            ),
            RiskRuleRequest(
                name = "New Device",
                description = "Transaction from a previously unseen device",
                conditionJson = """{"operator": "==", "field": "isNewDevice", "value": true}""",
                weight = 20
            ),
            RiskRuleRequest(
                name = "New Location",
                description = "Transaction from a previously unseen country",
                conditionJson = """{"operator": "==", "field": "isNewLocation", "value": true}""",
                weight = 15
            ),
            RiskRuleRequest(
                name = "Rapid Succession",
                description = "Transaction within 1 minute of previous transaction",
                conditionJson = """{"operator": "<", "field": "timeSinceLastTxMinutes", "value": 1}""",
                weight = 25
            ),
            RiskRuleRequest(
                name = "Amount Exceeds History",
                description = "Transaction amount exceeds user's historical maximum",
                conditionJson = """{"operator": ">", "field": "amountCents", "value": 0}""",
                weight = 15
            )
        )

        defaultRules.forEach { ruleRequest ->
            if (riskRuleRepository.findByName(ruleRequest.name) == null) {
                createRule(ruleRequest)
            }
        }
    }

    private data class EvaluationContext(
        val userTransactionCount1h: Int,
        val userTransactionCount24h: Int,
        val userAvgAmountCents: Long,
        val userMaxAmountCents: Long,
        val isNewDevice: Boolean,
        val isNewLocation: Boolean,
        val timeSinceLastTxMinutes: Long?
    )

    private fun buildEvaluationContext(transaction: Transaction): EvaluationContext {
        val userTransactions = transactionRepository.findByUserIdOrderByCreatedAtDesc(transaction.userId)
        
        val now = Instant.now()
        val oneHourAgo = now.minusSeconds(3600)
        val twentyFourHoursAgo = now.minusSeconds(86400)

        val recent1h = userTransactions.count { it.createdAt.isAfter(oneHourAgo) }
        val recent24h = userTransactions.count { it.createdAt.isAfter(twentyFourHoursAgo) }
        
        val amounts = userTransactions.map { it.amountCents }
        val avgAmount = if (amounts.isNotEmpty()) amounts.average().toLong() else transaction.amountCents
        val maxAmount = if (amounts.isNotEmpty()) amounts.maxOrNull() ?: transaction.amountCents else transaction.amountCents

        val isNewDevice = transaction.deviceId?.let { deviceId ->
            !userTransactions.any { it.deviceId == deviceId }
        } ?: false

        val isNewLocation = transaction.locationCountry?.let { country ->
            !userTransactions.any { it.locationCountry == country }
        } ?: false

        val lastTx = userTransactions.firstOrNull()
        val timeSinceLastTx = lastTx?.let { 
            java.time.Duration.between(it.createdAt, now).toMinutes() 
        }

        return EvaluationContext(
            userTransactionCount1h = recent1h,
            userTransactionCount24h = recent24h,
            userAvgAmountCents = avgAmount,
            userMaxAmountCents = maxAmount,
            isNewDevice = isNewDevice,
            isNewLocation = isNewLocation,
            timeSinceLastTxMinutes = timeSinceLastTx
        )
    }

    private fun toResponse(evaluation: RiskEvaluation): RiskEvaluationResponse {
        val triggeredRules = com.fasterxml.jackson.databind.ObjectMapper()
            .readValue(evaluation.triggeredRules, com.fasterxml.jackson.core.type.TypeReference<List<TriggeredRuleDto>>())
        val ruleScores = com.fasterxml.jackson.databind.ObjectMapper()
            .readValue(evaluation.ruleScores, com.fasterxml.jackson.core.type.TypeReference<Map<String, Int>>())

        return RiskEvaluationResponse(
            id = evaluation.id,
            transactionId = evaluation.transactionId,
            score = evaluation.score,
            decision = evaluation.decision,
            triggeredRules = triggeredRules,
            ruleScores = ruleScores,
            mlScore = evaluation.mlScore,
            mlModelVersion = evaluation.mlModelVersion,
            evaluationDurationMs = evaluation.evaluationDurationMs,
            explanation = evaluation.explanation,
            createdAt = evaluation.createdAt.toString()
        )
    }

    private fun toRuleResponse(rule: RiskRule): RiskRuleResponse {
        return RiskRuleResponse(
            id = rule.id,
            name = rule.name,
            description = rule.description,
            conditionJson = rule.conditionJson,
            weight = rule.weight,
            enabled = rule.enabled,
            version = rule.version,
            createdAt = rule.createdAt.toString(),
            updatedAt = rule.updatedAt.toString()
        )
    }
}