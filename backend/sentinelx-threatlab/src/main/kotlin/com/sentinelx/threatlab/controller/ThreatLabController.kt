package com.sentinelx.threatlab.controller

import com.sentinelx.auth.domain.User
import com.sentinelx.threatlab.dto.ReproductionRequest
import com.sentinelx.threatlab.dto.ReproductionResponse
import com.sentinelx.threatlab.dto.RunSimulationRequest
import com.sentinelx.threatlab.dto.SimulationListResponse
import com.sentinelx.threatlab.dto.SimulationRunResponse
import com.sentinelx.threatlab.dto.ThreatScenarioRequest
import com.sentinelx.threatlab.dto.ThreatScenarioResponse
import com.sentinelx.threatlab.service.ThreatLabService
import com.sentinelx.shared.kernel.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/threatlab")
@Tag(name = "Threat Lab", description = "Security simulation and threat scenario management")
class ThreatLabController(
    private val threatLabService: ThreatLabService
) {

    // Scenario endpoints
    @PostMapping("/scenarios")
    @Operation(summary = "Create a new threat scenario")
    fun createScenario(
        @Valid @RequestBody request: ThreatScenarioRequest,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<ThreatScenarioResponse>> {
        val user = getCurrentUser(authentication)
        val response = threatLabService.createScenario(request, user.id)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @PutMapping("/scenarios/{scenarioId}")
    @Operation(summary = "Update a threat scenario")
    fun updateScenario(
        @PathVariable scenarioId: UUID,
        @Valid @RequestBody request: ThreatScenarioRequest,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<ThreatScenarioResponse>> {
        val user = getCurrentUser(authentication)
        val response = threatLabService.updateScenario(scenarioId, request, user.id)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @GetMapping("/scenarios/{scenarioId}")
    @Operation(summary = "Get a threat scenario by ID")
    fun getScenario(@PathVariable scenarioId: UUID): ResponseEntity<ApiResponse<ThreatScenarioResponse>> {
        val response = threatLabService.getScenario(scenarioId)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @GetMapping("/scenarios")
    @Operation(summary = "List all threat scenarios")
    fun listScenarios(@RequestParam(defaultValue = "false") enabledOnly: Boolean): ResponseEntity<ApiResponse<List<ThreatScenarioResponse>>> {
        val response = threatLabService.listScenarios(enabledOnly)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @GetMapping("/scenarios/category/{category}")
    @Operation(summary = "Get scenarios by category")
    fun getScenariosByCategory(@PathVariable category: String): ResponseEntity<ApiResponse<List<ThreatScenarioResponse>>> {
        val response = threatLabService.getScenariosByCategory(category)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @DeleteMapping("/scenarios/{scenarioId}")
    @Operation(summary = "Delete a threat scenario")
    fun deleteScenario(@PathVariable scenarioId: UUID): ResponseEntity<ApiResponse<Unit>> {
        threatLabService.deleteScenario(scenarioId)
        return ResponseEntity.ok(ApiResponse.ok(Unit))
    }

    // Simulation endpoints
    @PostMapping("/simulations/run")
    @Operation(summary = "Run a threat simulation")
    fun runSimulation(
        @Valid @RequestBody request: RunSimulationRequest,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<SimulationRunResponse>> {
        val user = getCurrentUser(authentication)
        val response = threatLabService.runSimulation(request, user.id)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @GetMapping("/simulations/{simulationId}")
    @Operation(summary = "Get simulation results")
    fun getSimulation(@PathVariable simulationId: UUID): ResponseEntity<ApiResponse<SimulationRunResponse>> {
        val response = threatLabService.getSimulation(simulationId)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @GetMapping("/simulations")
    @Operation(summary = "List user's simulations")
    fun listSimulations(
        authentication: Authentication,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<SimulationListResponse>> {
        val user = getCurrentUser(authentication)
        val response = threatLabService.listSimulations(user.id, page, size)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    // Reproduction endpoint
    @PostMapping("/simulations/reproduce")
    @Operation(summary = "Reproduce a simulation")
    fun reproduce(@Valid @RequestBody request: ReproductionRequest): ResponseEntity<ApiResponse<ReproductionResponse>> {
        val response = threatLabService.reproduce(request)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    // Initialization
    @PostMapping("/initialize")
    @Operation(summary = "Initialize default scenarios")
    fun initializeDefaultScenarios(authentication: Authentication): ResponseEntity<ApiResponse<Unit>> {
        val user = getCurrentUser(authentication)
        threatLabService.initializeDefaultScenarios(user.id)
        return ResponseEntity.ok(ApiResponse.ok(Unit))
    }

    private fun getCurrentUser(authentication: Authentication): User {
        return User(email = authentication.name, passwordHash = "", fullName = null)
    }
}