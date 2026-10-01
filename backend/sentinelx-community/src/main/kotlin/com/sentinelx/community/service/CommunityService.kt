package com.sentinelx.community.service

import com.sentinelx.auth.domain.User
import com.sentinelx.community.domain.Comment
import com.sentinelx.community.domain.CommunityHeadline
import com.sentinelx.community.domain.ProblemReport
import com.sentinelx.community.domain.ProblemRelationship
import com.sentinelx.community.dto.CommentResponse
import com.sentinelx.community.dto.CommunityHeadlineResponse
import com.sentinelx.community.dto.CreateCommentRequest
import com.sentinelx.community.dto.CreateReportRequest
import com.sentinelx.community.dto.ReportListResponse
import com.sentinelx.community.dto.ReportResponse
import com.sentinelx.community.dto.UpdateReportRequest
import com.sentinelx.community.repository.CommentRepository
import com.sentinelx.community.repository.CommunityHeadlineRepository
import com.sentinelx.community.repository.ProblemRelationshipRepository
import com.sentinelx.community.repository.ProblemReportRepository
import com.sentinelx.shared.domain.HeadlineDetectionMethod
import com.sentinelx.shared.domain.HeadlineStatus
import com.sentinelx.shared.domain.ReportStatus
import com.sentinelx.shared.domain.ReportType
import com.sentinelx.shared.event.DomainEvent
import com.sentinelx.shared.event.EventPublisher
import com.sentinelx.shared.event.ReportCreatedEvent
import com.sentinelx.shared.event.ReportStatusChangedEvent
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
class CommunityService(
    private val reportRepository: ProblemReportRepository,
    private val commentRepository: CommentRepository,
    private val relationshipRepository: ProblemRelationshipRepository,
    private val headlineRepository: CommunityHeadlineRepository,
    private val eventPublisher: EventPublisher
) {

    // Report CRUD
    fun createReport(request: CreateReportRequest, user: User): ReportResponse {
        val report = ProblemReport(
            type = request.type,
            title = request.title,
            description = request.description,
            reporterId = user.id
        ).apply {
            severity = request.severity
            component = request.component
            evidenceJson = com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request.evidenceJson)
            reproductionSteps = request.reproductionSteps
            expectedBehavior = request.expectedBehavior
            actualBehavior = request.actualBehavior
            tags = request.tags.toTypedArray()
            relatedTransactionIds = request.relatedTransactionIds.toTypedArray()
            relatedSimulationIds = request.relatedSimulationIds.toTypedArray()
        }

        val saved = reportRepository.save(report)

        // Publish event
        eventPublisher.publish(ReportCreatedEvent(
            correlationId = UUID.randomUUID(),
            reportId = saved.id,
            type = request.type.name,
            severity = request.severity?.name,
            component = request.component,
            reporterId = user.id
        ))

        // Trigger relationship detection asynchronously
        detectRelationships(saved)

        return toReportResponse(saved, user.fullName, user.fullName, 0)
    }

    fun getReport(reportId: UUID): ReportResponse {
        val report = reportRepository.findById(reportId)
            .orElseThrow { NotFoundException("Report not found: $reportId") }
        val commentCount = commentRepository.countByReportId(reportId)
        return toReportResponse(report, null, null, commentCount)
    }

    fun updateReport(reportId: UUID, request: UpdateReportRequest, user: User): ReportResponse {
        val report = reportRepository.findById(reportId)
            .orElseThrow { NotFoundException("Report not found: $reportId") }

        val oldStatus = report.status

        request.title?.let { report.title = it }
        request.description?.let { report.description = it }
        request.severity?.let { report.severity = it }
        request.component?.let { report.component = it }
        request.evidenceJson?.let { report.evidenceJson = com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(it) }
        request.reproductionSteps?.let { report.reproductionSteps = it }
        request.expectedBehavior?.let { report.expectedBehavior = it }
        request.actualBehavior?.let { report.actualBehavior = it }
        request.tags?.let { report.tags = it.toTypedArray() }
        request.assigneeId?.let { report.assigneeId = it }
        request.resolutionSummary?.let { report.resolutionSummary = it }

        if (request.status != null && request.status != report.status) {
            report.status = request.status
            if (request.status == ReportStatus.RESOLVED) {
                report.resolvedAt = Instant.now()
            }
            // Publish status change event
            eventPublisher.publish(ReportStatusChangedEvent(
                correlationId = UUID.randomUUID(),
                reportId = report.id,
                fromStatus = oldStatus.name,
                toStatus = request.status.name,
                changedBy = user.id
            ))
        }

        val saved = reportRepository.save(report)

        // Re-detect relationships if content changed
        if (request.title != null || request.description != null || request.tags != null) {
            detectRelationships(saved)
        }

        return toReportResponse(saved, null, null, commentRepository.countByReportId(saved.id))
    }

    fun listReports(
        type: ReportType? = null,
        status: ReportStatus? = null,
        component: String? = null,
        tag: String? = null,
        reporterId: UUID? = null,
        page: Int = 0,
        size: Int = 20
    ): ReportListResponse {
        val pageable = PageRequest.of(page, size.coerceAtMost(100))
        
        val pageResult = when {
            type != null && status != null -> {
                val statuses = listOf(status)
                reportRepository.findByTypeAndStatusIn(type, statuses, pageable)
            }
            type != null -> reportRepository.findByType(type).map { reportRepository.findByType(type) }.getOrElse { emptyList() }?.let { pageResultFromList(it, pageable) }
            status != null -> reportRepository.findByStatusIn(listOf(status), pageable)
            tag != null -> pageResultFromList(reportRepository.findByTag(tag), pageable)
            reporterId != null -> reportRepository.findByReporterIdOrderByCreatedAtDesc(reporterId, pageable)
            component != null -> pageResultFromList(reportRepository.findByComponent(component), pageable)
            else -> reportRepository.findAll(pageable)
        }

        val reports = pageResult.content.map { report ->
            val reporter = getUserById(report.reporterId)
            val assignee = report.assigneeId?.let { getUserById(it) }
            val commentCount = commentRepository.countByReportId(report.id)
            toReportResponse(report, reporter?.fullName, assignee?.fullName, commentCount)
        }

        return ReportListResponse(
            reports = reports,
            page = pageResult.number,
            size = pageResult.size,
            totalElements = pageResult.totalElements,
            totalPages = pageResult.totalPages
        )
    }

    fun deleteReport(reportId: UUID, user: User) {
        val report = reportRepository.findById(reportId)
            .orElseThrow { NotFoundException("Report not found: $reportId") }
        
        if (report.reporterId != user.id && user.role != com.sentinelx.shared.domain.UserRole.ADMIN) {
            throw ForbiddenException("You can only delete your own reports")
        }
        
        reportRepository.delete(report)
    }

    // Comments
    fun addComment(reportId: UUID, request: CreateCommentRequest, user: User): CommentResponse {
        val report = reportRepository.findById(reportId)
            .orElseThrow { NotFoundException("Report not found: $reportId") }

        val comment = Comment(
            reportId = reportId,
            userId = user.id,
            content = request.content,
            parentCommentId = request.parentCommentId
        )

        val saved = commentRepository.save(comment)
        return toCommentResponse(saved, user.fullName, user.role.name, emptyList())
    }

    fun getComments(reportId: UUID): List<CommentResponse> {
        val topLevelComments = commentRepository.findTopLevelByReportIdOrderByCreatedAtAsc(reportId)
        return topLevelComments.map { comment ->
            val user = getUserById(comment.userId)
            val replies = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(comment.id)
                .map { reply ->
                    val replyUser = getUserById(reply.userId)
                    toCommentResponse(reply, replyUser?.fullName ?: "Unknown", replyUser?.role?.name ?: "VIEWER", emptyList())
                }
            toCommentResponse(comment, user?.fullName ?: "Unknown", user?.role?.name ?: "VIEWER", replies)
        }
    }

    // Relationships & Headlines
    private fun detectRelationships(report: ProblemReport) {
        // Simple tag-based similarity detection
        val recentReports = reportRepository.findCreatedSince(Instant.now().minusSeconds(86400 * 30))
        
        val reportTags = report.tags.toSet()
        if (reportTags.isEmpty()) return

        recentReports.forEach { otherReport ->
            if (otherReport.id == report.id) return@forEach

            val otherTags = otherReport.tags.toSet()
            if (otherTags.isEmpty()) return@forEach

            val intersection = reportTags.intersect(otherTags).size
            val union = reportTags.union(otherTags).size
            val similarity = if (union > 0) intersection.toDouble() / union else 0.0

            if (similarity >= 0.3) { // 30% Jaccard similarity threshold
                val existingRelationship = relationshipRepository.findByReportId(report.id)
                    .firstOrNull { it.reportId1 == otherReport.id || it.reportId2 == otherReport.id }

                if (existingRelationship == null) {
                    val relationship = ProblemRelationship(
                        reportId1 = min(report.id, otherReport.id),
                        reportId2 = max(report.id, otherReport.id),
                        similarityScore = similarity,
                        detectedBy = com.sentinelx.shared.domain.RelationshipDetectionMethod.TAGS
                    ).apply {
                        evidence = com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(mapOf(
                            "commonTags" to reportTags.intersect(otherTags),
                            "report1Tags" to reportTags,
                            "report2Tags" to otherTags
                        ))
                    }
                    relationshipRepository.save(relationship)
                }
            }
        }

        // Check if we should create/update a headline
        checkAndCreateHeadline(report)
    }

    private fun checkAndCreateHeadline(report: ProblemReport) {
        val relationships = relationshipRepository.findByReportId(report.id)
            .filter { it.similarityScore >= 0.3 && (it.confirmed == null || it.confirmed == true) }

        val relatedReportIds = relationships.flatMap { listOf(it.reportId1, it.reportId2) }.distinct()
        val clusterSize = relatedReportIds.size + 1 // +1 for the current report

        if (clusterSize >= 3) { // At least 3 related reports
            val existingHeadline = headlineRepository.findByEvidenceReportId(report.id)
                .firstOrNull { it.status != HeadlineStatus.RESOLVED }

            if (existingHeadline != null) {
                // Update existing headline
                val allReportIds = (existingHeadline.evidenceReportIds.toSet() + report.id).toTypedArray()
                existingHeadline.evidenceReportIds = allReportIds
                existingHeadline.reportCount = allReportIds.size
                existingHeadline.updatedAt = Instant.now()
                headlineRepository.save(existingHeadline)
            } else {
                // Create new headline
                val component = report.component ?: "Unknown"
                val headline = CommunityHeadline(
                    title = "Community Issue: ${report.title}",
                    summary = "$clusterSize related reports detected around ${report.component ?: "this area"}. " +
                             "Common themes: ${report.tags.joinToString(", ")}",
                    evidenceReportIds = (relatedReportIds + report.id).toTypedArray(),
                    detectionMethod = HeadlineDetectionMethod.TAGS
                ).apply {
                    affectedComponent = component
                    reportCount = clusterSize
                    severity = report.severity
                }
                headlineRepository.save(headline)
            }
        }
    }

    fun listHeadlines(status: String? = null, page: Int = 0, size: Int = 20): HeadlineListResponse {
        val pageable = PageRequest.of(page, size.coerceAtMost(100))
        val pageResult = status?.let { headlineRepository.findByStatusOrderByCreatedAtDesc(it, pageable) }
            ?: headlineRepository.findAll(pageable)

        val headlines = pageResult.content.map { h ->
            CommunityHeadlineResponse(
                id = h.id,
                title = h.title,
                summary = h.summary,
                reportCount = h.reportCount,
                simulationCount = h.simulationCount,
                affectedComponent = h.affectedComponent,
                status = h.status.name,
                severity = h.severity,
                evidenceReportIds = h.evidenceReportIds.toList(),
                evidenceSimulationIds = h.evidenceSimulationIds.toList(),
                detectionMethod = h.detectionMethod.name,
                createdAt = h.createdAt.toString(),
                updatedAt = h.updatedAt.toString(),
                resolvedAt = h.resolvedAt?.toString()
            )
        }

        return HeadlineListResponse(
            headlines = headlines,
            page = pageResult.number,
            size = pageResult.size,
            totalElements = pageResult.totalElements,
            totalPages = pageResult.totalPages
        )
    }

    fun getHeadline(headlineId: UUID): CommunityHeadlineResponse {
        val h = headlineRepository.findById(headlineId)
            .orElseThrow { NotFoundException("Headline not found: $headlineId") }
        return CommunityHeadlineResponse(
            id = h.id,
            title = h.title,
            summary = h.summary,
            reportCount = h.reportCount,
            simulationCount = h.simulationCount,
            affectedComponent = h.affectedComponent,
            status = h.status.name,
            severity = h.severity,
            evidenceReportIds = h.evidenceReportIds.toList(),
            evidenceSimulationIds = h.evidenceSimulationIds.toList(),
            detectionMethod = h.detectionMethod.name,
            createdAt = h.createdAt.toString(),
            updatedAt = h.updatedAt.toString(),
            resolvedAt = h.resolvedAt?.toString()
        )
    }

    // Helper methods
    private fun toReportResponse(
        report: ProblemReport,
        reporterName: String?,
        assigneeName: String?,
        commentCount: Int
    ): ReportResponse {
        return ReportResponse(
            id = report.id,
            type = report.type,
            title = report.title,
            description = report.description,
            severity = report.severity,
            component = report.component,
            status = report.status,
            reporterId = report.reporterId,
            reporterName = reporterName,
            assigneeId = report.assigneeId,
            assigneeName = assigneeName,
            evidenceJson = com.fasterxml.jackson.databind.ObjectMapper().readValue(report.evidenceJson),
            reproductionSteps = report.reproductionSteps,
            expectedBehavior = report.expectedBehavior,
            actualBehavior = report.actualBehavior,
            tags = report.tags.toList(),
            relatedTransactionIds = report.relatedTransactionIds.toList(),
            relatedSimulationIds = report.relatedSimulationIds.toList(),
            resolutionSummary = report.resolutionSummary,
            resolvedAt = report.resolvedAt?.toString(),
            createdAt = report.createdAt.toString(),
            updatedAt = report.updatedAt.toString(),
            commentCount = commentCount
        )
    }

    private fun toCommentResponse(
        comment: Comment,
        userName: String,
        userRole: String,
        replies: List<CommentResponse>
    ): CommentResponse {
        return CommentResponse(
            id = comment.id,
            reportId = comment.reportId,
            userId = comment.userId,
            userName = userName,
            userRole = userRole,
            content = comment.content,
            parentCommentId = comment.parentCommentId,
            isSystemMessage = comment.isSystemMessage,
            createdAt = comment.createdAt.toString(),
            updatedAt = comment.updatedAt.toString(),
            replies = replies
        )
    }

    private fun pageResultFromList(list: List<ProblemReport>, pageable: Pageable): org.springframework.data.domain.Page<ProblemReport> {
        val start = pageable.offset.toInt()
        val end = (start + pageable.pageSize).coerceAtMost(list.size)
        val pageContent = list.subList(start, end)
        return org.springframework.data.domain.PageImpl(pageContent, pageable, list.size)
    }

    private fun getUserById(userId: UUID): User? {
        // This would typically call a UserService or UserRepository
        // For now, return a mock - in production, inject UserRepository
        return null
    }

    private fun min(a: UUID, b: UUID): UUID = if (a.toString() < b.toString()) a else b
    private fun max(a: UUID, b: UUID): UUID = if (a.toString() > b.toString()) a else b
}