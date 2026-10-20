package com.sentinelx.threatlab

import com.sentinelx.threatlab.domain.SimulationRun
import com.sentinelx.threatlab.domain.ThreatScenario
import com.sentinelx.shared.domain.ScenarioCategory
import com.sentinelx.shared.domain.SimulationStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class ThreatLabDomainTest {

    @Test
    fun `ThreatScenario created with all fields`() {
        val scenario = ThreatScenario(
            name = "Test Scenario",
            category = ScenarioCategory.FRAUD_PATTERN,
            inputTemplate = """{"key": "value"}""",
            expectedResult = """{"detected": true}""",
            description = "Test description"
        ).apply {
            tags = arrayOf("tag1", "tag2")
            createdBy = UUID.randomUUID()
        }

        assertEquals("Test Scenario", scenario.name)
        assertEquals(ScenarioCategory.FRAUD_PATTERN, scenario.category)
        assertEquals("""{"key": "value"}""", scenario.inputTemplate)
        assertEquals("""{"detected": true}""", scenario.expectedResult)
        assertEquals("Test description", scenario.description)
        assertArrayEquals(arrayOf("tag1", "tag2"), scenario.tags)
        assertTrue(scenario.enabled)
        assertNotNull(scenario.id)
        assertNotNull(scenario.createdAt)
    }

    @Test
    fun `ThreatScenario defaults`() {
        val scenario = ThreatScenario(
            name = "Minimal",
            category = ScenarioCategory.CUSTOM,
            inputTemplate = "{}",
            expectedResult = "{}"
        )

        assertEquals("Minimal", scenario.name)
        assertEquals(ScenarioCategory.CUSTOM, scenario.category)
        assertNull(scenario.description)
        assertArrayEquals(emptyArray(), scenario.tags)
        assertTrue(scenario.enabled)
    }

    @Test
    fun `SimulationRun created with all fields`() {
        val scenarioId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        val run = SimulationRun(
            scenarioId = scenarioId,
            userId = userId,
            inputJson = """{"test": true}""",
            actualResult = """{"result": "ok"}""",
            expectedResult = """{"detected": true}"""
        ).apply {
            detected = true
            durationMs = 1500L
            logs = """["log1", "log2"]"""
        }

        assertEquals(scenarioId, run.scenarioId)
        assertEquals(userId, run.userId)
        assertEquals("""{"test": true}""", run.inputJson)
        assertEquals("""{"result": "ok"}""", run.actualResult)
        assertEquals("""{"detected": true}""", run.expectedResult)
        assertTrue(run.detected!!)
        assertEquals(1500L, run.durationMs)
        assertEquals("""["log1", "log2"]""", run.logs)
        assertEquals(SimulationStatus.RUNNING, run.status)
        assertNotNull(run.id)
    }

    @Test
    fun `SimulationRun status transitions`() {
        val run = SimulationRun(
            scenarioId = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            inputJson = "{}",
            actualResult = "{}",
            expectedResult = "{}"
        )

        assertEquals(SimulationStatus.RUNNING, run.status)

        run.status = SimulationStatus.COMPLETED
        assertEquals(SimulationStatus.COMPLETED, run.status)

        run.status = SimulationStatus.FAILED
        run.errorMessage = "Something went wrong"
        assertEquals(SimulationStatus.FAILED, run.status)
        assertEquals("Something went wrong", run.errorMessage)
    }

    @Test
    fun `Scenario categories are distinct`() {
        val categories = ScenarioCategory.values()
        assertEquals(8, categories.size)
        assertTrue(categories.contains(ScenarioCategory.FRAUD_PATTERN))
        assertTrue(categories.contains(ScenarioCategory.VELOCITY_ATTACK))
        assertTrue(categories.contains(ScenarioCategory.ACCOUNT_TAKEOVER))
        assertTrue(categories.contains(ScenarioCategory.API_ABUSE))
        assertTrue(categories.contains(ScenarioCategory.TRAFFIC_SPIKE))
        assertTrue(categories.contains(ScenarioCategory.SERVICE_FAILURE))
        assertTrue(categories.contains(ScenarioCategory.DATA_EXFILTRATION))
        assertTrue(categories.contains(ScenarioCategory.CUSTOM))
    }
}