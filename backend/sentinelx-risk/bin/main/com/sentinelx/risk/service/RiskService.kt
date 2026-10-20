package com.sentinelx.risk.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
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
import com.sentinelx.shared.kernel.ForbiddenException
import com.sentinelx.shared.kernel.NotFoundException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
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

        // Build context from transaction using high-performance SQL aggregates
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
            triggeredRules = ObjectMapper().writeValueAsString(result.triggeredRules),
            ruleScores = ObjectMapper().writeValueAsString(result.ruleScores)
        ).apply {
            evaluationDurationMs = result.evaluationDurationMs
            explanation = result.explanation
        }

        val savedEvaluation = riskEvaluationRepository.save(evaluation)

        // Publish event
        eventPublisher.publish(RiskEvaluationCompletedEvent(
            correlationId = transaction.correlationId,
            transactionId = transaction.id,
            riskScore = savedEvaluation.score,
            decision = savedEvaluation.decision.name,
            triggeredRules = result.triggeredRules.map {
                mapOf(
                    "ruleName" to it.ruleName,
                    "score" to it.score
                )
            }
        ))

        return toResponse(savedEvaluation)
    }

    fun getEvaluation(transactionId: UUID, requestingUserId: UUID? = null, isAdmin: Boolean = false): RiskEvaluationResponse {
        val evaluation = riskEvaluationRepository.findByTransactionId(transactionId)
            ?: throw NotFoundException("Risk evaluation not found for transaction: $transactionId")

        if (requestingUserId != null && !isAdmin) {
            val transaction = transactionRepository.findById(transactionId).orElse(null)
            if (transaction != null && transaction.userId != requestingUserId) {
                throw ForbiddenException("Access denied: You do not own this risk evaluation")
            }
        }

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

    /**
     * BUG-03 FIX: High performance SQL aggregate evaluation instead of loading 1,000 entities in-memory.
     */
    private fun buildEvaluationContext(transaction: Transaction): EvaluationContext {
        val now = Instant.now()
        val oneHourAgo = now.minusSeconds(3600)
        val twentyFourHoursAgo = now.minusSeconds(86400)
        val userId = transaction.userId

        val recent1h = transactionRepository.countByUserIdAndCreatedAtAfter(userId, oneHourAgo).toInt()
        val recent24h = transactionRepository.countByUserIdAndCreatedAtAfter(userId, twentyFourHoursAgo).toInt()
        val avgAmount = transactionRepository.avgAmountByUserId(userId).toLong()
        val maxAmount = transactionRepository.maxAmountByUserId(userId)

        val isNewDevice = transaction.deviceId?.let { deviceId ->
            !transactionRepository.existsByUserIdAndDeviceId(userId, deviceId)
        } ?: false

        val isNewLocation = transaction.locationCountry?.let { country ->
            !transactionRepository.existsByUserIdAndLocationCountry(userId, country)
        } ?: false

        val lastTxPage = transactionRepository.findLastTransactionTime(
            userId,
            transaction.id,
            PageRequest.of(0, 1)
        )
        val timeSinceLastTx = lastTxPage.content.firstOrNull()?.let {
            Duration.between(it, now).toMinutes()
        }

        return EvaluationContext(
            userTransactionCount1h = recent1h,
            userTransactionCount24h = recent24h,
            userAvgAmountCents = if (avgAmount > 0) avgAmount else transaction.amountCents,
            userMaxAmountCents = if (maxAmount > 0) maxAmount else transaction.amountCents,
            isNewDevice = isNewDevice,
            isNewLocation = isNewLocation,
            timeSinceLastTxMinutes = timeSinceLastTx
        )
    }

    private fun toResponse(evaluation: RiskEvaluation): RiskEvaluationResponse {
        val triggeredRules = ObjectMapper()
            .readValue(evaluation.triggeredRules, object : TypeReference<List<TriggeredRuleDto>>() {})
        val ruleScores = ObjectMapper()
            .readValue(evaluation.ruleScores, object : TypeReference<Map<String, Int>>() {})

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