package com.sentinelx.threatlab.service

import com.sentinelx.auth.domain.User
import com.sentinelx.threatlab.domain.SimulationRun
import com.sentinelx.threatlab.domain.ThreatScenario
import com.sentinelx.threatlab.dto.ReproductionRequest
import com.sentinelx.threatlab.dto.ReproductionResponse
import com.sentinelx.threatlab.dto.RunSimulationRequest
import com.sentinelx.threatlab.dto.SimulationListResponse
import com.sentinelx.threatlab.dto.SimulationRunResponse
import com.sentinelx.threatlab.dto.ThreatScenarioRequest
import com.sentinelx.threatlab.dto.ThreatScenarioResponse
import com.sentinelx.threatlab.repository.SimulationRunRepository
import com.sentinelx.threatlab.repository.ThreatScenarioRepository
import com.sentinelx.threatlab.runner.SimulationRunner
import com.sentinelx.shared.domain.SimulationStatus
import com.sentinelx.shared.kernel.NotFoundException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class ThreatLabService(
    private val scenarioRepository: ThreatScenarioRepository,
    private val simulationRepository: SimulationRunRepository,
    private val simulationRunner: SimulationRunner
) {

    // Scenario management
    fun createScenario(request: ThreatScenarioRequest, userId: UUID): ThreatScenarioResponse {
        val scenario = ThreatScenario(
            name = request.name,
            category = request.category,
            inputTemplate = request.inputTemplate,
            expectedResult = request.expectedResult,
            description = request.description
        ).apply {
            tags = request.tags.toTypedArray()
            enabled = request.enabled
            createdBy = userId
        }
        return toScenarioResponse(scenarioRepository.save(scenario))
    }

    fun updateScenario(scenarioId: UUID, request: ThreatScenarioRequest, userId: UUID): ThreatScenarioResponse {
        val scenario = scenarioRepository.findById(scenarioId)
            .orElseThrow { NotFoundException("Scenario not found: $scenarioId") }
        
        scenario.name = request.name
        scenario.description = request.description
        scenario.category = request.category
        scenario.inputTemplate = request.inputTemplate
        scenario.expectedResult = request.expectedResult
        scenario.tags = request.tags.toTypedArray()
        scenario.enabled = request.enabled
        
        return toScenarioResponse(scenarioRepository.save(scenario))
    }

    fun getScenario(scenarioId: UUID): ThreatScenarioResponse {
        val scenario = scenarioRepository.findById(scenarioId)
            .orElseThrow { NotFoundException("Scenario not found: $scenarioId") }
        return toScenarioResponse(scenario)
    }

    fun listScenarios(enabledOnly: Boolean = false): List<ThreatScenarioResponse> {
        val scenarios = if (enabledOnly) scenarioRepository.findByEnabledTrue() else scenarioRepository.findAll()
        return scenarios.map(::toScenarioResponse)
    }

    fun getScenariosByCategory(category: String): List<ThreatScenarioResponse> {
        val cat = com.sentinelx.shared.domain.ScenarioCategory.valueOf(category.toUpperCase())
        return scenarioRepository.findByCategory(cat).map(::toScenarioResponse)
    }

    fun deleteScenario(scenarioId: UUID) {
        scenarioRepository.deleteById(scenarioId)
    }

    // Simulation execution
    fun runSimulation(request: RunSimulationRequest, userId: UUID): SimulationRunResponse {
        val scenario = scenarioRepository.findById(request.scenarioId)
            .orElseThrow { NotFoundException("Scenario not found: ${request.scenarioId}") }
        
        val simulation = simulationRunner.runSimulation(scenario, userId, request.inputOverrides)
        val saved = simulationRepository.save(simulation)
        return toSimulationResponse(saved, scenario.name)
    }

    fun getSimulation(simulationId: UUID): SimulationRunResponse {
        val simulation = simulationRepository.findById(simulationId)
            .orElseThrow { NotFoundException("Simulation not found: $simulationId") }
        val scenario = scenarioRepository.findById(simulation.scenarioId).orElseThrow()
        return toSimulationResponse(simulation, scenario.name)
    }

    fun listSimulations(userId: UUID, page: Int, size: Int): SimulationListResponse {
        val pageable = PageRequest.of(page, size.coerceAtMost(100))
        val pageResult = simulationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
        
        val scenarios = pageResult.content.map { sim ->
            val scenario = scenarioRepository.findById(sim.scenarioId).orElseThrow()
            toSimulationResponse(sim, scenario.name)
        }

        return SimulationListResponse(
            simulations = scenarios,
            page = pageResult.number,
            size = pageResult.size,
            totalElements = pageResult.totalElements,
            totalPages = pageResult.totalPages
        )
    }

    // Reproduction
    fun reproduce(request: ReproductionRequest): ReproductionResponse {
        val original = simulationRepository.findById(request.simulationId)
            .orElseThrow { NotFoundException("Simulation not found: ${request.simulationId}") }
        
        val scenario = scenarioRepository.findById(original.scenarioId)
            .orElseThrow { NotFoundException("Scenario not found: ${original.scenarioId}") }

        // Re-run with same input
        val reproduction = simulationRunner.runSimulation(scenario, original.userId, objectMapper.readValue(original.inputJson))
        val savedReproduction = simulationRepository.save(reproduction)

        val originalResponse = toSimulationResponse(original, scenario.name)
        val reproductionResponse = toSimulationResponse(savedReproduction, scenario.name)

        val matched = original.detected == savedReproduction.detected
        val differences = mutableListOf<String>()
        
        if (!matched) {
            differences.add("Detection result differs: original=${original.detected}, reproduction=${savedReproduction.detected}")
        }
        if (original.durationMs != savedReproduction.durationMs) {
            differences.add("Duration differs: original=${original.durationMs}ms, reproduction=${savedReproduction.durationMs}ms")
        }

        return ReproductionResponse(
            originalSimulation = originalResponse,
            reproductionResult = reproductionResponse,
            matched = matched,
            differences = differences
        )
    }

    // Initialize default scenarios
    fun initializeDefaultScenarios(userId: UUID) {
        val defaultScenarios = listOf(
            ThreatScenarioRequest(
                name = "Rapid Fire Transactions",
                description = "Simulate 20 transactions in rapid succession from same user",
                category = com.sentinelx.shared.domain.ScenarioCategory.VELOCITY_ATTACK,
                inputTemplate = """{"transactionCount": 20, "baseAmountCents": 10000, "merchantId": "RAPID_MERCHANT", "deviceId": "device_123", "locationCountry": "US"}""",
                expectedResult = """{"detected": true}""",
                tags = listOf("velocity", "rapid-fire", "default")
            ),
            ThreatScenarioRequest(
                name = "New Device High Value",
                description = "High-value transaction from previously unseen device",
                category = com.sentinelx.shared.domain.ScenarioCategory.FRAUD_PATTERN,
                inputTemplate = """{"transactionCount": 1, "baseAmountCents": 500000, "merchantId": "HIGH_VALUE_MERCHANT", "deviceId": "new_device_456", "locationCountry": "US"}""",
                expectedResult = """{"detected": true}""",
                tags = listOf("new-device", "high-value", "default")
            ),
            ThreatScenarioRequest(
                name = "Geographic Anomaly",
                description = "Transaction from high-risk country for user with US history",
                category = com.sentinelx.shared.domain.ScenarioCategory.FRAUD_PATTERN,
                inputTemplate = """{"transactionCount": 1, "baseAmountCents": 25000, "merchantId": "INTL_MERCHANT", "deviceId": "device_789", "locationCountry": "RU"}""",
                expectedResult = """{"detected": true}""",
                tags = listOf("geo-anomaly", "location", "default")
            ),
            ThreatScenarioRequest(
                name = "Account Takeover Pattern",
                description = "Multiple failed logins followed by successful transaction from new device",
                category = com.sentinelx.shared.domain.ScenarioCategory.ACCOUNT_TAKEOVER,
                inputTemplate = """{"transactionCount": 3, "baseAmountCents": 10000, "merchantId": "ATO_MERCHANT", "deviceId": "compromised_device", "locationCountry": "CN"}""",
                expectedResult = """{"detected": true}""",
                tags = listOf("account-takeover", "credential-stuffing", "default")
            ),
            ThreatScenarioRequest(
                name = "API Abuse - Enumeration",
                description = "Rapid API calls to enumerate valid accounts",
                category = com.sentinelx.shared.domain.ScenarioCategory.API_ABUSE,
                inputTemplate = """{"transactionCount": 100, "baseAmountCents": 100, "merchantId": "ENUM_MERCHANT", "deviceId": "bot_device", "locationCountry": "US"}""",
                expectedResult = """{"detected": true}""",
                tags = listOf("api-abuse", "enumeration", "bot", "default")
            )
        )

        defaultScenarios.forEach { scenarioRequest ->
            if (scenarioRepository.findAll().none { it.name == scenarioRequest.name }) {
                createScenario(scenarioRequest, userId)
            }
        }
    }

    private fun toScenarioResponse(scenario: ThreatScenario): ThreatScenarioResponse {
        return ThreatScenarioResponse(
            id = scenario.id,
            name = scenario.name,
            description = scenario.description,
            category = scenario.category,
            inputTemplate = scenario.inputTemplate,
            expectedResult = scenario.expectedResult,
            tags = scenario.tags.toList(),
            enabled = scenario.enabled,
            createdAt = scenario.createdAt.toString(),
            updatedAt = scenario.updatedAt.toString()
        )
    }

    private fun toSimulationResponse(simulation: SimulationRun, scenarioName: String): SimulationRunResponse {
        return SimulationRunResponse(
            id = simulation.id,
            scenarioId = simulation.scenarioId,
            scenarioName = scenarioName,
            inputJson = simulation.inputJson,
            actualResult = simulation.actualResult,
            expectedResult = simulation.expectedResult,
            detected = simulation.detected,
            detectionDetails = simulation.detectionDetails,
            durationMs = simulation.durationMs,
            logs = simulation.logs,
            status = simulation.status,
            errorMessage = simulation.errorMessage,
            createdAt = simulation.createdAt.toString()
        )
    }
}