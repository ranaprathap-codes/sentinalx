package com.sentinelx.dryrun.repository

import com.sentinelx.dryrun.domain.DryRun
import com.sentinelx.shared.domain.DryRunStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.List
import java.util.UUID

@Repository
interface DryRunRepository : JpaRepository<DryRun, UUID> {
    fun findByReportId(reportId: UUID): List<DryRun>

    fun findByStatus(status: DryRunStatus): List<DryRun>

    @Query("SELECT d FROM DryRun d WHERE d.createdBy = :userId ORDER BY d.createdAt DESC")
    fun findByCreatedByOrderByCreatedAtDesc(@Param("userId") userId: UUID, pageable: Pageable): Page<DryRun>

    @Query("SELECT d FROM DryRun d WHERE d.reportId = :reportId ORDER BY d.createdAt DESC")
    fun findByReportIdOrderByCreatedAtDesc(@Param("reportId") reportId: UUID, pageable: Pageable): Page<DryRun>
}