package com.sentinelx.ops.dto

import com.sentinelx.shared.domain.IncidentSeverity
import com.sentinelx.shared.domain.IncidentStatus
import com.sentinelx.shared.domain.IncidentType
import jakarta.validation.constraints.*
import java.util.UUID

data class IncidentResponse(
    val id: UUID,
    val type: IncidentType,
    val severity: IncidentSeverity,
    val title: String,
    val description: String?,
    val status: IncidentStatus,
    val evidenceJson: String,
    val hypothesis: String?,
    val probableCause: String?,
    val aiSummary: String?,
    val affectedComponents: List<String>,
    val startedAt: String,
    val detectedAt: String,
    val acknowledgedAt: String?,
    val resolvedAt: String?,
    val assigneeId: UUID?,
    val assigneeName: String?,
    val createdAt: String,
    val updatedAt: String
)

data class IncidentListResponse(
    val incidents: List<IncidentResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class UpdateIncidentRequest(
    val status: IncidentStatus? = null,
    val hypothesis: String? = null,
    val probableCause: String? = null,
    val assigneeId: UUID? = null
)

data class AcknowledgeIncidentRequest(
    @NotNull val assigneeId: UUID
)

data class ResolveIncidentRequest(
    val probableCause: String? = null
)

data class HealthMetricsResponse(
    val apiLatencyP50Ms: Double,
    val apiLatencyP95Ms: Double,
    val apiLatencyP99Ms: Double,
    val errorRate: Double,
    val requestsPerSecond: Double,
    val paymentProcessingLatencyMs: Double,
    val riskEngineLatencyMs: Double,
    val databaseLatencyMs: Double,
    val activeIncidents: Int,
    val cpuUsagePercent: Double,
    val memoryUsagePercent: Double,
    val diskUsagePercent: Double
)

data class ServiceHealthResponse(
    val service: String,
    val status: String,
    val latencyMs: Double?,
    val errorRate: Double?,
    val lastCheck: String
)