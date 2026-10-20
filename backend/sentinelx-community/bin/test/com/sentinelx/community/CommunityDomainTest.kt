package com.sentinelx.community

import com.sentinelx.community.domain.Comment
import com.sentinelx.community.domain.CommunityHeadline
import com.sentinelx.community.domain.ProblemReport
import com.sentinelx.community.domain.ProblemRelationship
import com.sentinelx.shared.domain.HeadlineDetectionMethod
import com.sentinelx.shared.domain.HeadlineStatus
import com.sentinelx.shared.domain.ReportSeverity
import com.sentinelx.shared.domain.ReportStatus
import com.sentinelx.shared.domain.ReportType
import com.sentinelx.shared.domain.RelationshipDetectionMethod
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class CommunityDomainTest {

    @Test
    fun `ProblemReport created with all fields`() {
        val report = ProblemReport(
            type = ReportType.FLAW_REPORT,
            title = "Duplicate payment on retry",
            description = "When a payment times out and is retried, it creates a duplicate transaction.",
            reporterId = UUID.randomUUID()
        ).apply {
            severity = ReportSeverity.HIGH
            component = "Payment API"
            tags = arrayOf("idempotency", "retry", "duplicate")
            evidenceJson = """{"retryCount": 3, "duplicateCount": 2}"""
            reproductionSteps = "1. Create payment\n2. Timeout\n3. Retry"
            expectedBehavior = "Original payment should complete or fail cleanly"
            actualBehavior = "Both original and retry create transactions"
        }

        assertEquals(ReportType.FLAW_REPORT, report.type)
        assertEquals("Duplicate payment on retry", report.title)
        assertEquals(ReportSeverity.HIGH, report.severity)
        assertEquals("Payment API", report.component)
        assertEquals(ReportStatus.OPEN, report.status)
        assertArrayEquals(arrayOf("idempotency", "retry", "duplicate"), report.tags)
        assertEquals("""{"retryCount": 3, "duplicateCount": 2}""", report.evidenceJson)
        assertNotNull(report.id)
        assertNotNull(report.createdAt)
    }

    @Test
    fun `ProblemReport type defaults`() {
        val flawReport = ProblemReport(
            type = ReportType.FLAW_REPORT,
            title = "Flaw",
            description = "Description",
            reporterId = UUID.randomUUID()
        )
        assertEquals(ReportType.FLAW_REPORT, flawReport.type)

        val coderBlock = ProblemReport(
            type = ReportType.CODER_BLOCK,
            title = "Block",
            description = "Description",
            reporterId = UUID.randomUUID()
        )
        assertEquals(ReportType.CODER_BLOCK, coderBlock.type)

        val featureProposal = ProblemReport(
            type = ReportType.FEATURE_PROPOSAL,
            title = "Feature",
            description = "Description",
            reporterId = UUID.randomUUID()
        )
        assertEquals(ReportType.FEATURE_PROPOSAL, featureProposal.type)
    }

    @Test
    fun `ProblemReport status transitions`() {
        val report = ProblemReport(
            type = ReportType.FLAW_REPORT,
            title = "Test",
            description = "Test",
            reporterId = UUID.randomUUID()
        )

        assertEquals(ReportStatus.OPEN, report.status)

        report.status = ReportStatus.INVESTIGATING
        assertEquals(ReportStatus.INVESTIGATING, report.status)

        report.status = ReportStatus.IN_DEVELOPMENT
        assertEquals(ReportStatus.IN_DEVELOPMENT, report.status)

        report.status = ReportStatus.TESTING
        assertEquals(ReportStatus.TESTING, report.status)

        report.status = ReportStatus.RESOLVED
        report.resolvedAt = java.time.Instant.now()
        assertEquals(ReportStatus.RESOLVED, report.status)
        assertNotNull(report.resolvedAt)
    }

    @Test
    fun `Comment threading`() {
        val reportId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        val parent = Comment(
            reportId = reportId,
            userId = userId,
            content = "Parent comment"
        )

        val child = Comment(
            reportId = reportId,
            userId = userId,
            content = "Reply to parent",
            parentCommentId = parent.id
        )

        assertNull(parent.parentCommentId)
        assertEquals(parent.id, child.parentCommentId)
        assertEquals(reportId, parent.reportId)
        assertEquals(reportId, child.reportId)
    }

    @Test
    fun `ProblemRelationship with similarity`() {
        val report1 = UUID.randomUUID()
        val report2 = UUID.randomUUID()

        val relationship = ProblemRelationship(
            reportId1 = report1,
            reportId2 = report2,
            similarityScore = 0.85,
            detectedBy = RelationshipDetectionMethod.TAGS
        ).apply {
            evidence = """{"commonTags": ["idempotency", "retry"]}"""
            confirmed = true
        }

        assertEquals(report1, relationship.reportId1)
        assertEquals(report2, relationship.reportId2)
        assertEquals(0.85, relationship.similarityScore)
        assertEquals(RelationshipDetectionMethod.TAGS, relationship.detectedBy)
        assertEquals("""{"commonTags": ["idempotency", "retry"]}""", relationship.evidence)
        assertTrue(relationship.confirmed!!)
    }

    @Test
    fun `CommunityHeadline aggregates reports`() {
        val reportIds = (1..5).map { UUID.randomUUID() }.toTypedArray()

        val headline = CommunityHeadline(
            title = "Community Issue: Duplicate Payment Processing",
            summary = "5 reports about duplicate payments after retry",
            evidenceReportIds = reportIds,
            detectionMethod = HeadlineDetectionMethod.TAGS
        ).apply {
            affectedComponent = "Payment API"
            severity = ReportSeverity.HIGH
            reportCount = 5
        }

        assertEquals("Community Issue: Duplicate Payment Processing", headline.title)
        assertEquals(5, headline.reportCount)
        assertEquals("Payment API", headline.affectedComponent)
        assertEquals(ReportSeverity.HIGH, headline.severity)
        assertEquals(HeadlineStatus.DETECTED, headline.status)
        assertArrayEquals(reportIds, headline.evidenceReportIds)
    }
}