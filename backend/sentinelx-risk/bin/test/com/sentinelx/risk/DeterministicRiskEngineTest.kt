package com.sentinelx.risk

import com.sentinelx.risk.dto.RiskEvaluationRequest
import com.sentinelx.risk.dto.TriggeredRuleDto
import com.sentinelx.risk.engine.DeterministicRiskEngine
import com.sentinelx.risk.domain.RiskRule
import com.sentinelx.shared.domain.RiskDecision
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class DeterministicRiskEngineTest {

    private lateinit var engine: DeterministicRiskEngine
    private lateinit var rules: List<RiskRule>
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        objectMapper = ObjectMapper()
        engine = DeterministicRiskEngine(objectMapper)
        
        rules = listOf(
            createRule("High Amount", """{"operator": ">", "field": "amountCents", "value": 500000}""", 25),
            createRule("Velocity 1h", """{"operator": ">", "field": "userTransactionCount1h", "value": 10}""", 30),
            createRule("Velocity 24h", """{"operator": ">", "field": "userTransactionCount24h", "value": 50}""", 20),
            createRule("New Device", """{"operator": "==", "field": "isNewDevice", "value": true}""", 20),
            createRule("New Location", """{"operator": "==", "field": "isNewLocation", "value": true}""", 15),
            createRule("Rapid Succession", """{"operator": "<", "field": "timeSinceLastTxMinutes", "value": 1}""", 25),
            createRule("Amount Exceeds History", """{"operator": ">", "field": "amountCents", "value": 0}""", 15)
        )
    }

    private fun createRule(name: String, condition: String, weight: Int): RiskRule {
        return RiskRule(name, condition, weight).apply { id = UUID.randomUUID() }
    }

    @Test
    fun `low risk transaction gets ALLOW decision`() {
        val request = RiskEvaluationRequest(
            transactionId = UUID.randomUUID(),
            amountCents = 10000,
            merchantId = "MERCHANT_001",
            deviceId = "device_123",
            deviceFingerprint = "fp_123",
            locationCountry = "US",
            locationRegion = "CA",
            locationCity = "San Francisco",
            userTransactionCount1h = 1,
            userTransactionCount24h = 5,
            userAvgAmountCents = 15000,
            userMaxAmountCents = 20000,
            isNewDevice = false,
            isNewLocation = false,
            timeSinceLastTxMinutes = 60L
        )

        val result = engine.evaluate(request, rules)

        assertEquals(RiskDecision.ALLOW, result.decision)
        assertTrue(result.score < 40)
        assertTrue(result.triggeredRules.isEmpty())
    }

    @Test
    fun `high amount triggers High Amount rule`() {
        val request = RiskEvaluationRequest(
            transactionId = UUID.randomUUID(),
            amountCents = 600000,
            merchantId = "MERCHANT_001",
            deviceId = "device_123",
            deviceFingerprint = "fp_123",
            locationCountry = "US",
            locationRegion = "CA",
            locationCity = "San Francisco",
            userTransactionCount1h = 1,
            userTransactionCount24h = 5,
            userAvgAmountCents = 15000,
            userMaxAmountCents = 20000,
            isNewDevice = false,
            isNewLocation = false,
            timeSinceLastTxMinutes = 60L
        )

        val result = engine.evaluate(request, rules)

        assertTrue(result.triggeredRules.any { it.ruleName == "High Amount" })
        assertTrue(result.score >= 25)
    }

    @Test
    fun `high velocity triggers velocity rules`() {
        val request = RiskEvaluationRequest(
            transactionId = UUID.randomUUID(),
            amountCents = 10000,
            merchantId = "MERCHANT_001",
            deviceId = "device_123",
            deviceFingerprint = "fp_123",
            locationCountry = "US",
            locationRegion = "CA",
            locationCity = "San Francisco",
            userTransactionCount1h = 15,
            userTransactionCount24h = 60,
            userAvgAmountCents = 15000,
            userMaxAmountCents = 20000,
            isNewDevice = false,
            isNewLocation = false,
            timeSinceLastTxMinutes = 60L
        )

        val result = engine.evaluate(request, rules)

        assertTrue(result.triggeredRules.any { it.ruleName == "Velocity 1h" })
        assertTrue(result.triggeredRules.any { it.ruleName == "Velocity 24h" })
        assertTrue(result.score >= 50)
    }

    @Test
    fun `new device triggers New Device rule`() {
        val request = RiskEvaluationRequest(
            transactionId = UUID.randomUUID(),
            amountCents = 10000,
            merchantId = "MERCHANT_001",
            deviceId = "new_device",
            deviceFingerprint = "fp_new",
            locationCountry = "US",
            locationRegion = "CA",
            locationCity = "San Francisco",
            userTransactionCount1h = 1,
            userTransactionCount24h = 5,
            userAvgAmountCents = 15000,
            userMaxAmountCents = 20000,
            isNewDevice = true,
            isNewLocation = false,
            timeSinceLastTxMinutes = 60L
        )

        val result = engine.evaluate(request, rules)

        assertTrue(result.triggeredRules.any { it.ruleName == "New Device" })
        assertTrue(result.score >= 20)
    }

    @Test
    fun `rapid succession triggers Rapid Succession rule`() {
        val request = RiskEvaluationRequest(
            transactionId = UUID.randomUUID(),
            amountCents = 10000,
            merchantId = "MERCHANT_001",
            deviceId = "device_123",
            deviceFingerprint = "fp_123",
            locationCountry = "US",
            locationRegion = "CA",
            locationCity = "San Francisco",
            userTransactionCount1h = 1,
            userTransactionCount24h = 5,
            userAvgAmountCents = 15000,
            userMaxAmountCents = 20000,
            isNewDevice = false,
            isNewLocation = false,
            timeSinceLastTxMinutes = 0L
        )

        val result = engine.evaluate(request, rules)

        assertTrue(result.triggeredRules.any { it.ruleName == "Rapid Succession" })
        assertTrue(result.score >= 25)
    }

    @Test
    fun `multiple rules combine for BLOCK decision`() {
        val request = RiskEvaluationRequest(
            transactionId = UUID.randomUUID(),
            amountCents = 600000,
            merchantId = "MERCHANT_001",
            deviceId = "new_device",
            deviceFingerprint = "fp_new",
            locationCountry = "RU",
            locationRegion = "Moscow",
            locationCity = "Moscow",
            userTransactionCount1h = 15,
            userTransactionCount24h = 60,
            userAvgAmountCents = 15000,
            userMaxAmountCents = 20000,
            isNewDevice = true,
            isNewLocation = true,
            timeSinceLastTxMinutes = 0L
        )

        val result = engine.evaluate(request, rules)

        assertEquals(RiskDecision.BLOCK, result.decision)
        assertTrue(result.score >= 70)
        assertTrue(result.triggeredRules.size >= 5)
    }

    @Test
    fun `score capped at 100`() {
        // Create rules with very high weights
        val highWeightRules = listOf(
            createRule("Rule 1", """{"operator": "==", "field": "amountCents", "value": 10000}""", 50),
            createRule("Rule 2", """{"operator": "==", "field": "userTransactionCount1h", "value": 1}""", 50),
            createRule("Rule 3", """{"operator": "==", "field": "isNewDevice", "value": false}""", 50)
        )

        val request = RiskEvaluationRequest(
            transactionId = UUID.randomUUID(),
            amountCents = 10000,
            merchantId = "MERCHANT_001",
            deviceId = "device_123",
            deviceFingerprint = "fp_123",
            locationCountry = "US",
            locationRegion = "CA",
            locationCity = "San Francisco",
            userTransactionCount1h = 1,
            userTransactionCount24h = 5,
            userAvgAmountCents = 15000,
            userMaxAmountCents = 20000,
            isNewDevice = false,
            isNewLocation = false,
            timeSinceLastTxMinutes = 60L
        )

        val result = engine.evaluate(request, highWeightRules)

        assertEquals(100, result.score)
        assertEquals(RiskDecision.BLOCK, result.decision)
    }

    @Test
    fun `explanation contains key information`() {
        val request = RiskEvaluationRequest(
            transactionId = UUID.randomUUID(),
            amountCents = 600000,
            merchantId = "MERCHANT_001",
            deviceId = "new_device",
            deviceFingerprint = "fp_new",
            locationCountry = "US",
            locationRegion = "CA",
            locationCity = "San Francisco",
            userTransactionCount1h = 1,
            userTransactionCount24h = 5,
            userAvgAmountCents = 15000,
            userMaxAmountCents = 20000,
            isNewDevice = true,
            isNewLocation = false,
            timeSinceLastTxMinutes = 60L
        )

        val result = engine.evaluate(request, rules)

        assertNotNull(result.explanation)
        assertTrue(result.explanation!!.contains("Risk Score"))
        assertTrue(result.explanation!!.contains("Decision"))
        assertTrue(result.explanation!!.contains("High Amount"))
        assertTrue(result.explanation!!.contains("New Device"))
    }

    @Test
    fun `AND condition works correctly`() {
        val andRule = createRule("High Amount AND New Device", """{
            "operator": "AND",
            "conditions": [
                {"operator": ">", "field": "amountCents", "value": 500000},
                {"operator": "==", "field": "isNewDevice", "value": true}
            ]
        }""", 30)

        val requestBoth = RiskEvaluationRequest(
            transactionId = UUID.randomUUID(),
            amountCents = 600000,
            merchantId = "MERCHANT_001",
            deviceId = "new_device",
            deviceFingerprint = "fp_new",
            locationCountry = "US",
            locationRegion = "CA",
            locationCity = "San Francisco",
            userTransactionCount1h = 1,
            userTransactionCount24h = 5,
            userAvgAmountCents = 15000,
            userMaxAmountCents = 20000,
            isNewDevice = true,
            isNewLocation = false,
            timeSinceLastTxMinutes = 60L
        )

        val resultBoth = engine.evaluate(requestBoth, listOf(andRule))
        assertTrue(resultBoth.triggeredRules.any { it.ruleName == "High Amount AND New Device" })

        val requestOnlyAmount = requestBoth.copy(isNewDevice = false)
        val resultOnlyAmount = engine.evaluate(requestOnlyAmount, listOf(andRule))
        assertFalse(resultOnlyAmount.triggeredRules.any { it.ruleName == "High Amount AND New Device" })
    }

    @Test
    fun `OR condition works correctly`() {
        val orRule = createRule("High Amount OR New Device", """{
            "operator": "OR",
            "conditions": [
                {"operator": ">", "field": "amountCents", "value": 500000},
                {"operator": "==", "field": "isNewDevice", "value": true}
            ]
        }""", 25)

        val requestAmount = RiskEvaluationRequest(
            transactionId = UUID.randomUUID(),
            amountCents = 600000,
            merchantId = "MERCHANT_001",
            deviceId = "device_123",
            deviceFingerprint = "fp_123",
            locationCountry = "US",
            locationRegion = "CA",
            locationCity = "San Francisco",
            userTransactionCount1h = 1,
            userTransactionCount24h = 5,
            userAvgAmountCents = 15000,
            userMaxAmountCents = 20000,
            isNewDevice = false,
            isNewLocation = false,
            timeSinceLastTxMinutes = 60L
        )

        val resultAmount = engine.evaluate(requestAmount, listOf(orRule))
        assertTrue(resultAmount.triggeredRules.any { it.ruleName == "High Amount OR New Device" })

        val requestDevice = requestAmount.copy(amountCents = 10000, isNewDevice = true)
        val resultDevice = engine.evaluate(requestDevice, listOf(orRule))
        assertTrue(resultDevice.triggeredRules.any { it.ruleName == "High Amount OR New Device" })
    }
}