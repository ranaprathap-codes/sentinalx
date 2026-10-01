package com.sentinelx.community.repository

import com.sentinelx.community.domain.Comment
import com.sentinelx.community.domain.CommunityHeadline
import com.sentinelx.community.domain.ProblemRelationship
import com.sentinelx.community.domain.ProblemReport
import com.sentinelx.shared.domain.ReportStatus
import com.sentinelx.shared.domain.ReportType
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
interface ProblemReportRepository : JpaRepository<ProblemReport, UUID> {
    fun findByType(type: ReportType): List<ProblemReport>

    fun findByStatus(status: ReportStatus): List<ProblemReport>

    fun findByReporterId(reporterId: UUID): List<ProblemReport>

    fun findByComponent(component: String): List<ProblemReport>

    @Query("SELECT r FROM ProblemReport r WHERE :tag = ANY(r.tags)")
    fun findByTag(@Param("tag") tag: String): List<ProblemReport>

    @Query("SELECT r FROM ProblemReport r WHERE r.status IN :statuses ORDER BY r.createdAt DESC")
    fun findByStatusIn(@Param("statuses") statuses: List<ReportStatus>, pageable: Pageable): Page<ProblemReport>

    @Query("SELECT r FROM ProblemReport r WHERE r.type = :type AND r.status IN :statuses ORDER BY r.createdAt DESC")
    fun findByTypeAndStatusIn(@Param("type") type: ReportType, @Param("statuses") statuses: List<ReportStatus>, pageable: Pageable): Page<ProblemReport>

    @Query("SELECT r FROM ProblemReport r WHERE r.reporterId = :reporterId ORDER BY r.createdAt DESC")
    fun findByReporterIdOrderByCreatedAtDesc(@Param("reporterId") reporterId: UUID, pageable: Pageable): Page<ProblemReport>

    @Query("SELECT r FROM ProblemReport r WHERE r.assigneeId = :assigneeId ORDER BY r.createdAt DESC")
    fun findByAssigneeIdOrderByCreatedAtDesc(@Param("assigneeId") assigneeId: UUID, pageable: Pageable): Page<ProblemReport>

    @Query("SELECT r FROM ProblemReport r WHERE r.createdAt >= :since ORDER BY r.createdAt DESC")
    fun findCreatedSince(@Param("since") since: Instant): List<ProblemReport>
}

@Repository
interface CommentRepository : JpaRepository<Comment, UUID> {
    @Query("SELECT c FROM Comment c WHERE c.reportId = :reportId ORDER BY c.createdAt ASC")
    fun findByReportIdOrderByCreatedAtAsc(@Param("reportId") reportId: UUID): List<Comment>

    @Query("SELECT c FROM Comment c WHERE c.reportId = :reportId AND c.parentCommentId IS NULL ORDER BY c.createdAt ASC")
    fun findTopLevelByReportIdOrderByCreatedAtAsc(@Param("reportId") reportId: UUID): List<Comment>

    @Query("SELECT c FROM Comment c WHERE c.parentCommentId = :parentId ORDER BY c.createdAt ASC")
    fun findByParentCommentIdOrderByCreatedAtAsc(@Param("parentId") parentId: UUID): List<Comment>
}

@Repository
interface ProblemRelationshipRepository : JpaRepository<ProblemRelationship, UUID> {
    @Query("SELECT r FROM ProblemRelationship r WHERE r.reportId1 = :reportId OR r.reportId2 = :reportId")
    fun findByReportId(@Param("reportId") reportId: UUID): List<ProblemRelationship>

    @Query("SELECT r FROM ProblemRelationship r WHERE r.similarityScore >= :threshold ORDER BY r.similarityScore DESC")
    fun findBySimilarityThreshold(@Param("threshold") threshold: Double): List<ProblemRelationship>

    @Query("SELECT r FROM ProblemRelationship r WHERE r.detectedBy = :method")
    fun findByDetectionMethod(@Param("method") method: String): List<ProblemRelationship>
}

@Repository
interface CommunityHeadlineRepository : JpaRepository<CommunityHeadline, UUID> {
    fun findByStatus(status: String): List<CommunityHeadline>

    @Query("SELECT h FROM CommunityHeadline h WHERE h.status = :status ORDER BY h.createdAt DESC")
    fun findByStatusOrderByCreatedAtDesc(@Param("status") status: String, pageable: Pageable): Page<CommunityHeadline>

    @Query("SELECT h FROM CommunityHeadline h WHERE :reportId = ANY(h.evidenceReportIds)")
    fun findByEvidenceReportId(@Param("reportId") reportId: UUID): List<CommunityHeadline>
}