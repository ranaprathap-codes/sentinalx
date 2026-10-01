package com.sentinelx.threatlab.runner

import com.sentinelx.payments.domain.Transaction
import com.sentinelx.payments.repository.TransactionRepository
import com.sentinelx.risk.domain.RiskDecision
import com.sentinelx.risk.service.RiskService
import com.sentinelx.threatlab.domain.SimulationRun
import com.sentinelx.threatlab.domain.ThreatScenario
import com.sentinelx.shared.domain.SimulationStatus
import com.sentinelx.shared.event.DomainEvent
import com.sentinelx.shared.event.EventPublisher
import com.sentinelx.shared.event.SimulationCompletedEvent
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class SimulationRunner(
    private val transactionRepository: TransactionRepository,
    private val riskService: RiskService,
    private val eventPublisher: EventPublisher,
    private val objectMapper: ObjectMapper
) {

    private val runningSimulations = ConcurrentHashMap<UUID, SimulationRun>()

    fun runSimulation(scenario: ThreatScenario, userId: UUID, inputOverrides: Map<String, Any> = emptyMap()): SimulationRun {
        val simulationId = UUID.randomUUID()
        val correlationId = UUID.randomUUID()
        val startTime = System.currentTimeMillis()

        val simulation = SimulationRun(
            scenarioId = scenario.id,
            userId = userId,
            inputJson = objectMapper.writeValueAsString(inputOverrides),
            actualResult = "{}",
            expectedResult = scenario.expectedResult
        ).apply {
            id = simulationId
            status = SimulationStatus.RUNNING
        }

        runningSimulations[simulationId] = simulation

        try {
            val inputTemplate = objectMapper.readTree(scenario.inputTemplate)
            val mergedInput = mergeInputTemplate(inputTemplate, inputOverrides)

            val logs = mutableListOf<String>()
            logs.add("[${Instant.now()}] Starting simulation: ${scenario.name}")
            logs.add("[${Instant.now()}] Input: ${objectMapper.writeValueAsString(mergedInput)}")

            // Execute simulation steps
            val result = executeSimulationSteps(scenario, mergedInput, logs, correlationId)

            val durationMs = System.currentTimeMillis() - startTime
            val expectedResult = objectMapper.readTree(scenario.expectedResult)
            val detected = compareResults(result, expectedResult, logs)

            simulation.actualResult = objectMapper.writeValueAsString(result)
            simulation.detected = detected
            simulation.detectionDetails = objectMapper.writeValueAsString(mapOf(
                "matched" to detected,
                "details" to result
            ))
            simulation.durationMs = durationMs
            simulation.logs = objectMapper.writeValueAsString(logs)
            simulation.status = SimulationStatus.COMPLETED

            logs.add("[${Instant.now()}] Simulation completed in ${durationMs}ms. Detected: $detected")

            // Publish event
            eventPublisher.publish(SimulationCompletedEvent(
                correlationId = correlationId,
                simulationId = simulationId,
                scenarioId = scenario.id,
                detected = detected,
                durationMs = durationMs
            ))

            return simulation

        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            simulation.status = SimulationStatus.FAILED
            simulation.errorMessage = e.message
            simulation.durationMs = durationMs
            simulation.logs = objectMapper.writeValueAsString(listOf(
                "[${Instant.now()}] Simulation failed: ${e.message}"
            ))
            throw e
        } finally {
            runningSimulations.remove(simulationId)
        }
    }

    private fun executeSimulationSteps(
        scenario: ThreatScenario,
        input: JsonNode,
        logs: MutableList<String>,
        correlationId: UUID
    ): ObjectNode {
        val result = objectMapper.createObjectNode()
        val transactions = mutableListOf<JsonNode>()

        // Get simulation parameters
        val transactionCount = input["transactionCount"]?.asInt() ?: 1
        val baseAmount = input["baseAmountCents"]?.asLong() ?: 10000
        val merchantId = input["merchantId"]?.asText() ?: "TEST_MERCHANT"
        val deviceId = input["deviceId"]?.asText()
        val locationCountry = input["locationCountry"]?.asText() ?: "US"

        // Create synthetic user for simulation
        val syntheticUserId = UUID.randomUUID()

        for (i in 0 until transactionCount) {
            val txAmount = (baseAmount * (1 + (i * 0.1))).toLong()
            
            // Create transaction
            val transaction = Transaction(
                idempotencyKey = "sim_${scenario.id}_${simulationId}_$i",
                userId = syntheticUserId,
                amountCents = txAmount,
                merchantId = merchantId,
                correlationId = correlationId
            ).apply {
                deviceId = deviceId
                locationCountry = locationCountry
                metadata = objectMapper.writeValueAsString(mapOf("simulation" to true, "scenarioId" to scenario.id.toString()))
            }

            val savedTx = transactionRepository.save(transaction)
            logs.add("[${Instant.now()}] Created transaction ${savedTx.id} for amount $txAmount cents")

            // Evaluate risk
            val riskEvaluation = riskService.evaluateRisk(
                com.sentinelx.risk.dto.EvaluateRiskRequest(transactionId = savedTx.id, forceReevaluation = true)
            )

            val txResult = objectMapper.createObjectNode().apply {
                put("transactionId", savedTx.id.toString())
                put("amountCents", txAmount)
                put("riskScore", riskEvaluation.score)
                put("decision", riskEvaluation.decision.name)
            }
            transactions.add(txResult)

            logs.add("[${Instant.now()}] Transaction ${savedTx.id} risk score: ${riskEvaluation.score}, decision: ${riskEvaluation.decision}")
        }

        result.put("transactionsProcessed", transactionCount)
        result.set("transactions", objectMapper.valueToTree(transactions))
        result.put("scenarioId", scenario.id.toString())
        result.put("timestamp", Instant.now().toString())

        return result
    }

    private fun mergeInputTemplate(template: JsonNode, overrides: Map<String, Any>): JsonNode {
        val merged = objectMapper.valueToTree(template)
        if (merged is ObjectNode) {
            overrides.forEach { (key, value) ->
                merged.set(key, objectMapper.valueToTree(value))
            }
        }
        return merged
    }

    private fun compareResults(actual: JsonNode, expected: JsonNode, logs: MutableList<String>): Boolean {
        // Simple comparison - in production, use more sophisticated matching
        val actualDetected = actual["transactions"]?.elements()?.any { 
            it["decision"]?.asText() == RiskDecision.BLOCK.name || it["decision"]?.asText() == RiskDecision.REVIEW.name
        } ?: false

        val expectedDetected = expected["detected"]?.asBoolean() ?: false

        logs.add("[${Instant.now()}] Comparison - Actual detected: $actualDetected, Expected detected: $expectedDetected")

        return actualDetected == expectedDetected
    }

    fun getRunningSimulation(simulationId: UUID): SimulationRun? {
        return runningSimulations[simulationId]
    }
}