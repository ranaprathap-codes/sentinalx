package com.sentinelx.threatlab.dto

import com.sentinelx.shared.domain.ScenarioCategory
import com.sentinelx.shared.domain.SimulationStatus
import jakarta.validation.constraints.*
import java.util.UUID

data class ThreatScenarioRequest(
    @NotBlank @Size(max = 255) val name: String,
    @Size(max = 2000) val description: String? = null,
    @NotNull val category: ScenarioCategory,
    @NotBlank val inputTemplate: String,
    @NotBlank val expectedResult: String,
    val tags: List<String> = emptyList(),
    val enabled: Boolean = true
)

data class ThreatScenarioResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val category: ScenarioCategory,
    val inputTemplate: String,
    val expectedResult: String,
    val tags: List<String>,
    val enabled: Boolean,
    val createdAt: String,
    val updatedAt: String
)

data class RunSimulationRequest(
    @NotNull val scenarioId: UUID,
    val inputOverrides: Map<String, Any> = emptyMap()
)

data class SimulationRunResponse(
    val id: UUID,
    val scenarioId: UUID,
    val scenarioName: String,
    val inputJson: String,
    val actualResult: String,
    val expectedResult: String,
    val detected: Boolean?,
    val detectionDetails: String?,
    val durationMs: Long?,
    val logs: String?,
    val status: SimulationStatus,
    val errorMessage: String?,
    val createdAt: String
)

data class SimulationListResponse(
    val simulations: List<SimulationRunResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class ReproductionRequest(
    @NotNull val simulationId: UUID
)

data class ReproductionResponse(
    val originalSimulation: SimulationRunResponse,
    val reproductionResult: SimulationRunResponse,
    val matched: Boolean,
    val differences: List<String>
)