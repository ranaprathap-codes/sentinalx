package com.sentinelx.ops.repository

import com.sentinelx.ops.domain.Incident
import com.sentinelx.shared.domain.IncidentStatus
import com.sentinelx.shared.domain.IncidentType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.List
import java.util.UUID

@Repository
interface IncidentRepository : JpaRepository<Incident, UUID> {
    fun findByStatus(status: IncidentStatus): List<Incident>

    fun findByType(type: IncidentType): List<Incident>

    fun findByAssigneeId(assigneeId: UUID): List<Incident>

    @Query("SELECT i FROM Incident i WHERE i.startedAt >= :since ORDER BY i.startedAt DESC")
    fun findSince(@Param("since") since: Instant, pageable: Pageable): Page<Incident>

    @Query("SELECT i FROM Incident i WHERE i.status IN :statuses ORDER BY i.detectedAt DESC")
    fun findByStatusIn(@Param("statuses") statuses: List<IncidentStatus>, pageable: Pageable): Page<Incident>

    @Query("SELECT i FROM Incident i WHERE i.severity = :severity AND i.status != 'RESOLVED' ORDER BY i.detectedAt DESC")
    fun findUnresolvedBySeverity(@Param("severity") severity: String, pageable: Pageable): Page<Incident>
}