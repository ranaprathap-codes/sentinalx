package com.sentinelx.threatlab.repository

import com.sentinelx.threatlab.domain.SimulationRun
import com.sentinelx.threatlab.domain.ThreatScenario
import com.sentinelx.shared.domain.ScenarioCategory
import com.sentinelx.shared.domain.SimulationStatus
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
interface ThreatScenarioRepository : JpaRepository<ThreatScenario, UUID> {
    fun findByEnabledTrue(): List<ThreatScenario>
    fun findByCategory(category: ScenarioCategory): List<ThreatScenario>
    fun findByTagsContaining(tag: String): List<ThreatScenario>
}

@Repository
interface SimulationRunRepository : JpaRepository<SimulationRun, UUID> {
    fun findByScenarioId(scenarioId: UUID): List<SimulationRun>

    @Query("SELECT s FROM SimulationRun s WHERE s.userId = :userId ORDER BY s.createdAt DESC")
    fun findByUserIdOrderByCreatedAtDesc(@Param("userId") userId: UUID, pageable: Pageable): Page<SimulationRun>

    @Query("SELECT s FROM SimulationRun s WHERE s.scenarioId = :scenarioId AND s.status = :status")
    fun findByScenarioIdAndStatus(@Param("scenarioId") scenarioId: UUID, @Param("status") status: SimulationStatus): List<SimulationRun>

    @Query("SELECT s FROM SimulationRun s WHERE s.detected = true AND s.createdAt >= :since ORDER BY s.createdAt DESC")
    fun findDetectedSince(@Param("since") since: Instant): List<SimulationRun>
}