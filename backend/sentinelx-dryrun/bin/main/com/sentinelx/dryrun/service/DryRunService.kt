package com.sentinelx.dryrun.service

import com.sentinelx.auth.domain.User
import com.sentinelx.dryrun.domain.DryRun
import com.sentinelx.dryrun.dto.CreateDryRunRequest
import com.sentinelx.dryrun.dto.DryRunListResponse
import com.sentinelx.dryrun.dto.DryRunResponse
import com.sentinelx.dryrun.engine.DryRunEngine
import com.sentinelx.dryrun.repository.DryRunRepository
import com.sentinelx.shared.domain.DryRunStatus
import com.sentinelx.shared.event.DomainEvent
import com.sentinelx.shared.event.EventPublisher
import com.sentinelx.shared.event.DryRunCompletedEvent
import com.sentinelx.shared.kernel.NotFoundException
import com.sentinelx.shared.kernel.ForbiddenException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class DryRunService(
    private val dryRunRepository: DryRunRepository,
    private val dryRunEngine: DryRunEngine,
    private val eventPublisher: EventPublisher
) {

    fun createDryRun(request: CreateDryRunRequest, user: User): DryRunResponse {
        val dryRun = DryRun(
            name = request.name,
            proposedRuleJson = request.proposedRuleJson,
            baselineRuleVersion = request.baselineRuleVersion,
            createdBy = user.id
        ).apply {
            description = request.description
            reportId = request.reportId
        }

        val saved = dryRunRepository.save(dryRun)
        return toResponse(saved)
    }

    fun executeDryRun(dryRunId: UUID, user: User): DryRunResponse {
        val dryRun = dryRunRepository.findById(dryRunId)
            .orElseThrow { NotFoundException("Dry run not found: $dryRunId") }

        if (dryRun.createdBy != user.id && user.role != com.sentinelx.shared.domain.UserRole.ADMIN) {
            throw ForbiddenException("You can only execute your own dry runs")
        }

        if (dryRun.status == DryRunStatus.RUNNING || dryRun.status == DryRunStatus.COMPLETED) {
            throw IllegalStateException("Dry run already executed or running")
        }

        dryRun.status = DryRunStatus.RUNNING
        dryRunRepository.save(dryRun)

        try {
            val summary = dryRunEngine.executeDryRun(dryRun)
            val saved = dryRunRepository.save(dryRun)

            // Publish event
            eventPublisher.publish(DryRunCompletedEvent(
                correlationId = UUID.randomUUID(),
                dryRunId = dryRunId,
                reportId = dryRun.reportId,
                transactionsTested = summary.totalTransactions,
                falsePositiveDelta = summary.falsePositiveDelta,
                falseNegativeDelta = summary.falseNegativeDelta
            ))

            return toResponse(saved)
        } catch (e: Exception) {
            dryRun.status = DryRunStatus.FAILED
            dryRunRepository.save(dryRun)
            throw e
        }
    }

    fun getDryRun(dryRunId: UUID): DryRunResponse {
        val dryRun = dryRunRepository.findById(dryRunId)
            .orElseThrow { NotFoundException("Dry run not found: $dryRunId") }
        return toResponse(dryRun)
    }

    fun listDryRuns(
        reportId: UUID? = null,
        userId: UUID? = null,
        page: Int = 0,
        size: Int = 20
    ): DryRunListResponse {
        val pageable = PageRequest.of(page, size.coerceAtMost(100))
        
        val pageResult = when {
            reportId != null -> dryRunRepository.findByReportIdOrderByCreatedAtDesc(reportId, pageable)
            userId != null -> dryRunRepository.findByCreatedByOrderByCreatedAtDesc(userId, pageable)
            else -> dryRunRepository.findAll(pageable)
        }

        val dryRuns = pageResult.content.map(::toResponse)

        return DryRunListResponse(
            dryRuns = dryRuns,
            page = pageResult.number,
            size = pageResult.size,
            totalElements = pageResult.totalElements,
            totalPages = pageResult.totalPages
        )
    }

    fun deleteDryRun(dryRunId: UUID, user: User) {
        val dryRun = dryRunRepository.findById(dryRunId)
            .orElseThrow { NotFoundException("Dry run not found: $dryRunId") }

        if (dryRun.createdBy != user.id && user.role != com.sentinelx.shared.domain.UserRole.ADMIN) {
            throw ForbiddenException("You can only delete your own dry runs")
        }

        dryRunRepository.delete(dryRun)
    }

    private fun toResponse(dryRun: DryRun): DryRunResponse {
        return DryRunResponse(
            id = dryRun.id,
            reportId = dryRun.reportId,
            name = dryRun.name,
            description = dryRun.description,
            baselineRuleVersion = dryRun.baselineRuleVersion,
            proposedRuleJson = dryRun.proposedRuleJson,
            baselineResultJson = dryRun.baselineResultJson,
            proposedResultJson = dryRun.proposedResultJson,
            diffJson = dryRun.diffJson,
            transactionsTested = dryRun.transactionsTested,
            falsePositiveDelta = dryRun.falsePositiveDelta,
            falseNegativeDelta = dryRun.falseNegativeDelta,
            latencyDeltaMs = dryRun.latencyDeltaMs,
            status = dryRun.status,
            createdBy = dryRun.createdBy,
            createdAt = dryRun.createdAt.toString(),
            completedAt = dryRun.completedAt?.toString()
        )
    }
}