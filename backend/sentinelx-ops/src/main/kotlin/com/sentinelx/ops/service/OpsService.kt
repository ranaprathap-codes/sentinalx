package com.sentinelx.ops.service

import com.sentinelx.auth.domain.User
import com.sentinelx.ops.domain.Incident
import com.sentinelx.ops.domain.IncidentStatus
import com.sentinelx.ops.dto.AcknowledgeIncidentRequest
import com.sentinelx.ops.dto.IncidentListResponse
import com.sentinelx.ops.dto.IncidentResponse
import com.sentinelx.ops.dto.ResolveIncidentRequest
import com.sentinelx.ops.dto.UpdateIncidentRequest
import com.sentinelx.ops.repository.IncidentRepository
import com.sentinelx.shared.domain.IncidentStatus
import com.sentinelx.shared.kernel.NotFoundException
import com.sentinelx.shared.kernel.ForbiddenException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class OpsService(
    private val incidentRepository: IncidentRepository
) {

    fun listIncidents(
        status: IncidentStatus? = null,
        page: Int = 0,
        size: Int = 20
    ): IncidentListResponse {
        val pageable = PageRequest.of(page, size.coerceAtMost(100))
        
        val pageResult = status?.let { incidentRepository.findByStatusIn(listOf(it), pageable) }
            ?: incidentRepository.findAll(pageable)

        val incidents = pageResult.content.map(::toResponse)

        return IncidentListResponse(
            incidents = incidents,
            page = pageResult.number,
            size = pageResult.size,
            totalElements = pageResult.totalElements,
            totalPages = pageResult.totalPages
        )
    }

    fun getIncident(incidentId: UUID): IncidentResponse {
        val incident = incidentRepository.findById(incidentId)
            .orElseThrow { NotFoundException("Incident not found: $incidentId") }
        return toResponse(incident)
    }

    fun updateIncident(incidentId: UUID, request: UpdateIncidentRequest, user: User): IncidentResponse {
        val incident = incidentRepository.findById(incidentId)
            .orElseThrow { NotFoundException("Incident not found: $incidentId") }

        request.status?.let { 
            incident.status = it
            if (it == IncidentStatus.INVESTIGATING && incident.acknowledgedAt == null) {
                incident.acknowledgedAt = Instant.now()
            } else if (it == IncidentStatus.RESOLVED) {
                incident.resolvedAt = Instant.now()
            }
        }
        request.hypothesis?.let { incident.hypothesis = it }
        request.probableCause?.let { incident.probableCause = it }
        request.assigneeId?.let { incident.assigneeId = it }

        val saved = incidentRepository.save(incident)
        return toResponse(saved)
    }

    fun acknowledgeIncident(incidentId: UUID, request: AcknowledgeIncidentRequest, user: User): IncidentResponse {
        val incident = incidentRepository.findById(incidentId)
            .orElseThrow { NotFoundException("Incident not found: $incidentId") }

        if (incident.status != IncidentStatus.DETECTED) {
            throw IllegalStateException("Can only acknowledge DETECTED incidents")
        }

        incident.status = IncidentStatus.INVESTIGATING
        incident.acknowledgedAt = Instant.now()
        incident.assigneeId = request.assigneeId

        val saved = incidentRepository.save(incident)
        return toResponse(saved)
    }

    fun resolveIncident(incidentId: UUID, request: ResolveIncidentRequest, user: User): IncidentResponse {
        val incident = incidentRepository.findById(incidentId)
            .orElseThrow { NotFoundException("Incident not found: $incidentId") }

        if (incident.status == IncidentStatus.RESOLVED) {
            throw IllegalStateException("Incident already resolved")
        }

        incident.status = IncidentStatus.RESOLVED
        incident.resolvedAt = Instant.now()
        request.probableCause?.let { incident.probableCause = it }

        val saved = incidentRepository.save(incident)
        return toResponse(saved)
    }

    fun getServiceHealth(): List<com.sentinelx.ops.dto.ServiceHealthResponse> {
        // This would integrate with actual health checks
        return listOf(
            com.sentinelx.ops.dto.ServiceHealthResponse("api", "UP", 45.0, 0.001, Instant.now().toString()),
            com.sentinelx.ops.dto.ServiceHealthResponse("payments", "UP", 32.0, 0.0, Instant.now().toString()),
            com.sentinelx.ops.dto.ServiceHealthResponse("risk-engine", "UP", 12.0, 0.0, Instant.now().toString()),
            com.sentinelx.ops.dto.ServiceHealthResponse("threatlab", "UP", 156.0, 0.0, Instant.now().toString()),
            com.sentinelx.ops.dto.ServiceHealthResponse("database", "UP", 8.0, 0.0, Instant.now().toString()),
            com.sentinelx.ops.dto.ServiceHealthResponse("redis", "UP", 2.0, 0.0, Instant.now().toString()),
            com.sentinelx.ops.dto.ServiceHealthService("kafka", "UP", 5.0, 0.0, Instant.now().toString())
        )
    }

    private fun toResponse(incident: Incident): IncidentResponse {
        return IncidentResponse(
            id = incident.id,
            type = incident.type,
            severity = incident.severity,
            title = incident.title,
            description = incident.description,
            status = incident.status,
            evidenceJson = incident.evidenceJson,
            hypothesis = incident.hypothesis,
            probableCause = incident.probableCause,
            aiSummary = incident.aiSummary,
            affectedComponents = incident.affectedComponents.toList(),
            startedAt = incident.startedAt.toString(),
            detectedAt = incident.detectedAt.toString(),
            acknowledgedAt = incident.acknowledgedAt?.toString(),
            resolvedAt = incident.resolvedAt?.toString(),
            assigneeId = incident.assigneeId,
            assigneeName = null, // Would lookup from UserService
            createdAt = incident.createdAt.toString(),
            updatedAt = incident.updatedAt.toString()
        )
    }
}