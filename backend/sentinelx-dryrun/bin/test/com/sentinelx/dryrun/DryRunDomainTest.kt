package com.sentinelx.dryrun

import com.sentinelx.dryrun.domain.DryRun
import com.sentinelx.shared.domain.DryRunStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class DryRunDomainTest {

    @Test
    fun `DryRun created with all fields`() {
        val dryRun = DryRun(
            name = "Stricter Velocity Rule",
            proposedRuleJson = """{"name": "Velocity 1h", "weight": 40}""",
            baselineRuleVersion = 1,
            createdBy = UUID.randomUUID()
        ).apply {
            description = "Test stricter velocity threshold"
            reportId = UUID.randomUUID()
        }

        assertEquals("Stricter Velocity Rule", dryRun.name)
        assertEquals("Test stricter velocity threshold", dryRun.description)
        assertEquals(1, dryRun.baselineRuleVersion)
        assertEquals("""{"name": "Velocity 1h", "weight": 40}""", dryRun.proposedRuleJson)
        assertEquals(DryRunStatus.PENDING, dryRun.status)
        assertEquals(0, dryRun.transactionsTested)
        assertEquals(0, dryRun.falsePositiveDelta)
        assertEquals(0, dryRun.falseNegativeDelta)
        assertEquals(0L, dryRun.latencyDeltaMs)
        assertNotNull(dryRun.id)
        assertNotNull(dryRun.createdAt)
        assertNull(dryRun.completedAt)
    }

    @Test
    fun `DryRun status transitions`() {
        val dryRun = DryRun(
            name = "Test",
            proposedRuleJson = "{}",
            baselineRuleVersion = 1,
            createdBy = UUID.randomUUID()
        )

        assertEquals(DryRunStatus.PENDING, dryRun.status)

        dryRun.status = DryRunStatus.RUNNING
        assertEquals(DryRunStatus.RUNNING, dryRun.status)

        dryRun.status = DryRunStatus.COMPLETED
        dryRun.completedAt = java.time.Instant.now()
        dryRun.transactionsTested = 1000
        dryRun.falsePositiveDelta = 5
        dryRun.falseNegativeDelta = -2
        dryRun.latencyDeltaMs = 3
        assertEquals(DryRunStatus.COMPLETED, dryRun.status)
        assertNotNull(dryRun.completedAt)

        val failedRun = DryRun(
            name = "Failed Test",
            proposedRuleJson = "{}",
            baselineRuleVersion = 1,
            createdBy = UUID.randomUUID()
        )
        failedRun.status = DryRunStatus.FAILED
        assertEquals(DryRunStatus.FAILED, failedRun.status)
    }

    @Test
    fun `DryRun result JSON fields`() {
        val dryRun = DryRun(
            name = "Test",
            proposedRuleJson = "{}",
            baselineRuleVersion = 1,
            createdBy = UUID.randomUUID()
        ).apply {
            baselineResultJson = """{"transactions": [{"id": "1", "decision": "ALLOW"}]}"""
            proposedResultJson = """{"transactions": [{"id": "1", "decision": "REVIEW"}]}"""
            diffJson = """{"changed": 1, "total": 1}"""
        }

        assertEquals("""{"transactions": [{"id": "1", "decision": "ALLOW"}]}""", dryRun.baselineResultJson)
        assertEquals("""{"transactions": [{"id": "1", "decision": "REVIEW"}]}""", dryRun.proposedResultJson)
        assertEquals("""{"changed": 1, "total": 1}""", dryRun.diffJson)
    }
}