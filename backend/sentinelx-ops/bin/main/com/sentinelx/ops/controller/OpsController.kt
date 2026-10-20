package com.sentinelx.ops.controller

import com.sentinelx.auth.domain.User
import com.sentinelx.ops.dto.AcknowledgeIncidentRequest
import com.sentinelx.ops.dto.IncidentListResponse
import com.sentinelx.ops.dto.IncidentResponse
import com.sentinelx.ops.dto.ResolveIncidentRequest
import com.sentinelx.ops.dto.UpdateIncidentRequest
import com.sentinelx.ops.service.OpsService
import com.sentinelx.shared.domain.IncidentStatus
import com.sentinelx.shared.kernel.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/ops")
@Tag(name = "OpsSentinel", description = "Internal monitoring and incident management")
class OpsController(
    private val opsService: OpsService
) {

    @GetMapping("/health")
    @Operation(summary = "Get system health metrics")
    fun getHealth(): ResponseEntity<ApiResponse<com.sentinelx.ops.dto.HealthMetricsResponse>> {
        // Would inject OpsMonitor to get metrics
        return ResponseEntity.ok(ApiResponse.ok(com.sentinelx.ops.dto.HealthMetricsResponse(
            apiLatencyP50Ms = 0.0,
            apiLatencyP95Ms = 0.0,
            apiLatencyP99Ms = 0.0,
            errorRate = 0.0,
            requestsPerSecond = 0.0,
            paymentProcessingLatencyMs = 0.0,
            riskEngineLatencyMs = 0.0,
            databaseLatencyMs = 0.0,
            activeIncidents = 0,
            cpuUsagePercent = 0.0,
            memoryUsagePercent = 0.0,
            diskUsagePercent = 0.0
        )))
    }

    @GetMapping("/services")
    @Operation(summary = "Get service health status")
    fun getServiceHealth(): ResponseEntity<ApiResponse<List<com.sentinelx.ops.dto.ServiceHealthResponse>>> {
        val response = opsService.getServiceHealth()
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @GetMapping("/incidents")
    @Operation(summary = "List incidents")
    fun listIncidents(
        @RequestParam(required = false) status: IncidentStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<IncidentListResponse>> {
        val response = opsService.listIncidents(status, page, size)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @GetMapping("/incidents/{incidentId}")
    @Operation(summary = "Get incident by ID")
    fun getIncident(@PathVariable incidentId: UUID): ResponseEntity<ApiResponse<IncidentResponse>> {
        val response = opsService.getIncident(incidentId)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @PutMapping("/incidents/{incidentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST')")
    @Operation(summary = "Update incident")
    fun updateIncident(
        @PathVariable incidentId: UUID,
        @Valid @RequestBody request: UpdateIncidentRequest,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<IncidentResponse>> {
        val user = getCurrentUser(authentication)
        val response = opsService.updateIncident(incidentId, request, user)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @PostMapping("/incidents/{incidentId}/acknowledge")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST')")
    @Operation(summary = "Acknowledge an incident")
    fun acknowledgeIncident(
        @PathVariable incidentId: UUID,
        @Valid @RequestBody request: AcknowledgeIncidentRequest,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<IncidentResponse>> {
        val user = getCurrentUser(authentication)
        val response = opsService.acknowledgeIncident(incidentId, request, user)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @PostMapping("/incidents/{incidentId}/resolve")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST')")
    @Operation(summary = "Resolve an incident")
    fun resolveIncident(
        @PathVariable incidentId: UUID,
        @Valid @RequestBody request: ResolveIncidentRequest,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<IncidentResponse>> {
        val user = getCurrentUser(authentication)
        val response = opsService.resolveIncident(incidentId, request, user)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    private fun getCurrentUser(authentication: Authentication): User {
        return User(email = authentication.name, passwordHash = "", fullName = authentication.name)
    }
}