package com.sentinelx.community.dto

import com.sentinelx.shared.domain.ReportSeverity
import com.sentinelx.shared.domain.ReportStatus
import com.sentinelx.shared.domain.ReportType
import jakarta.validation.constraints.*
import java.util.UUID

data class CreateReportRequest(
    @NotNull val type: ReportType,
    @NotBlank @Size(max = 500) val title: String,
    @NotBlank @Size(max = 10000) val description: String,
    val severity: ReportSeverity? = null,
    @Size(max = 255) val component: String? = null,
    val evidenceJson: Map<String, Any> = emptyMap(),
    val reproductionSteps: String? = null,
    val expectedBehavior: String? = null,
    val actualBehavior: String? = null,
    val tags: List<String> = emptyList(),
    val relatedTransactionIds: List<UUID> = emptyList(),
    val relatedSimulationIds: List<UUID> = emptyList()
)

data class UpdateReportRequest(
    @Size(max = 500) val title: String? = null,
    @Size(max = 10000) val description: String? = null,
    val severity: ReportSeverity? = null,
    @Size(max = 255) val component: String? = null,
    val evidenceJson: Map<String, Any>? = null,
    val reproductionSteps: String? = null,
    val expectedBehavior: String? = null,
    val actualBehavior: String? = null,
    val tags: List<String>? = null,
    val status: ReportStatus? = null,
    val assigneeId: UUID? = null,
    val resolutionSummary: String? = null
)

data class ReportResponse(
    val id: UUID,
    val type: ReportType,
    val title: String,
    val description: String,
    val severity: ReportSeverity?,
    val component: String?,
    val status: ReportStatus,
    val reporterId: UUID,
    val reporterName: String?,
    val assigneeId: UUID?,
    val assigneeName: String?,
    val evidenceJson: Map<String, Any>,
    val reproductionSteps: String?,
    val expectedBehavior: String?,
    val actualBehavior: String?,
    val tags: List<String>,
    val relatedTransactionIds: List<UUID>,
    val relatedSimulationIds: List<UUID>,
    val resolutionSummary: String?,
    val resolvedAt: String?,
    val createdAt: String,
    val updatedAt: String,
    val commentCount: Int
)

data class ReportListResponse(
    val reports: List<ReportResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class CreateCommentRequest(
    @NotBlank @Size(max = 10000) val content: String,
    val parentCommentId: UUID? = null
)

data class CommentResponse(
    val id: UUID,
    val reportId: UUID,
    val userId: UUID,
    val userName: String,
    val userRole: String,
    val content: String,
    val parentCommentId: UUID?,
    val isSystemMessage: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val replies: List<CommentResponse> = emptyList()
)

data class CommunityHeadlineResponse(
    val id: UUID,
    val title: String,
    val summary: String,
    val reportCount: Int,
    val simulationCount: Int,
    val affectedComponent: String?,
    val status: String,
    val severity: ReportSeverity?,
    val evidenceReportIds: List<UUID>,
    val evidenceSimulationIds: List<UUID>,
    val detectionMethod: String,
    val createdAt: String,
    val updatedAt: String,
    val resolvedAt: String?
)

data class HeadlineListResponse(
    val headlines: List<CommunityHeadlineResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class RelationshipResponse(
    val id: UUID,
    val reportId1: UUID,
    val reportId2: UUID,
    val similarityScore: Double,
    val detectedBy: String,
    val evidence: Map<String, Any>,
    val confirmed: Boolean?,
    val confirmedBy: UUID?,
    val confirmedAt: String?,
    val createdAt: String
)

data class RelatedReportsResponse(
    val report: ReportResponse,
    val relationships: List<RelationshipResponse>
)