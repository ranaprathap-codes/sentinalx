package com.sentinelx.ops.monitor

import com.sentinelx.ops.domain.Incident
import com.sentinelx.ops.domain.IncidentSeverity
import com.sentinelx.ops.domain.IncidentStatus
import com.sentinelx.ops.domain.IncidentType
import com.sentinelx.ops.repository.IncidentRepository
import com.sentinelx.ops.service.OpsService
import com.sentinelx.shared.event.EventPublisher
import com.sentinelx.shared.event.IncidentCreatedEvent
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Component
class OpsMonitor(
    private val incidentRepository: IncidentRepository,
    private val opsService: OpsService,
    private val eventPublisher: EventPublisher,
    private val meterRegistry: MeterRegistry
) {

    private val errorCount = AtomicLong(0)
    private val requestCount = AtomicLong(0)
    private val latencySum = AtomicLong(0)
    private val latencyCount = AtomicLong(0)
    private val lastIncidentCheck = AtomicLong(0)

    // Track metrics for anomaly detection
    private val recentErrorRates = ConcurrentHashMap<Long, Double>()
    private val recentLatencies = ConcurrentHashMap<Long, Double>()

    @Scheduled(fixedRate = 30000) // Every 30 seconds
    fun checkHealth() {
        val now = System.currentTimeMillis()
        
        // Calculate current metrics
        val totalRequests = requestCount.getAndSet(0)
        val totalErrors = errorCount.getAndSet(0)
        val totalLatency = latencySum.getAndSet(0)
        val latencySamples = latencyCount.getAndSet(0)

        val errorRate = if (totalRequests > 0) totalErrors.toDouble() / totalRequests else 0.0
        val avgLatency = if (latencySamples > 0) totalLatency.toDouble() / latencySamples else 0.0

        recentErrorRates[now] = errorRate
        recentLatencies[now] = avgLatency

        // Clean old metrics (keep last 10 minutes)
        val cutoff = now - 600000
        recentErrorRates.keys().filter { it < cutoff }.forEach { recentErrorRates.remove(it) }
        recentLatencies.keys().filter { it < cutoff }.forEach { recentLatencies.remove(it) }

        // Check for anomalies
        detectErrorSpike(errorRate, now)
        detectLatencySpike(avgLatency, now)
        
        lastIncidentCheck.set(now)
    }

    fun recordRequest(latencyMs: Long, isError: Boolean) {
        requestCount.incrementAndGet()
        if (isError) errorCount.incrementAndGet()
        latencySum.addAndGet(latencyMs)
        latencyCount.incrementAndGet()
    }

    private fun detectErrorSpike(currentErrorRate: Double, now: Long) {
        if (recentErrorRates.size < 5) return // Need baseline

        val baselineErrorRate = recentErrorRates.values.dropLast(1).average()
        val threshold = baselineErrorRate * 5 // 5x baseline
        val minThreshold = 0.05 // At least 5% error rate

        if (currentErrorRate > threshold && currentErrorRate > minThreshold) {
            // Check if we already have an active error spike incident
            val existingIncident = incidentRepository.findByStatusIn(
                listOf(IncidentStatus.DETECTED, IncidentStatus.INVESTIGATING),
                org.springframework.data.domain.PageRequest.of(0, 10)
            ).content.firstOrNull { it.type == IncidentType.ERROR_SPIKE }

            if (existingIncident == null) {
                createIncident(
                    type = IncidentType.ERROR_SPIKE,
                    severity = if (currentErrorRate > 0.2) IncidentSeverity.SEV1 else IncidentSeverity.SEV2,
                    title = "Error Rate Spike Detected",
                    evidence = mapOf(
                        "currentErrorRate" to currentErrorRate,
                        "baselineErrorRate" to baselineErrorRate,
                        "threshold" to threshold,
                        "timestamp" to Instant.now().toString()
                    ),
                    affectedComponents = listOf("api", "payments", "risk-engine")
                )
            }
        }
    }

    private fun detectLatencySpike(currentLatency: Double, now: Long) {
        if (recentLatencies.size < 5) return

        val baselineLatency = recentLatencies.values.dropLast(1).average()
        val threshold = baselineLatency * 3 // 3x baseline
        val minThreshold = 500.0 // At least 500ms

        if (currentLatency > threshold && currentLatency > minThreshold) {
            val existingIncident = incidentRepository.findByStatusIn(
                listOf(IncidentStatus.DETECTED, IncidentStatus.INVESTIGATING),
                org.springframework.data.domain.PageRequest.of(0, 10)
            ).content.firstOrNull { it.type == IncidentType.LATENCY_SPIKE }

            if (existingIncident == null) {
                createIncident(
                    type = IncidentType.LATENCY_SPIKE,
                    severity = if (currentLatency > 2000) IncidentSeverity.SEV1 else IncidentSeverity.SEV2,
                    title = "API Latency Spike Detected",
                    evidence = mapOf(
                        "currentLatencyMs" to currentLatency,
                        "baselineLatencyMs" to baselineLatency,
                        "threshold" to threshold,
                        "timestamp" to Instant.now().toString()
                    ),
                    affectedComponents = listOf("api", "database", "cache")
                )
            }
        }
    }

    private fun createIncident(
        type: IncidentType,
        severity: IncidentSeverity,
        title: String,
        evidence: Map<String, Any>,
        affectedComponents: List<String>
    ) {
        val incident = Incident(
            type = type,
            severity = severity,
            title = title,
            evidenceJson = com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(evidence)
        ).apply {
            affectedComponents = affectedComponents.toTypedArray()
            description = "Automatically detected by OpsSentinel monitoring"
        }

        val saved = incidentRepository.save(incident)

        // Publish event
        eventPublisher.publish(IncidentCreatedEvent(
            correlationId = UUID.randomUUID(),
            incidentId = saved.id,
            type = type.name,
            severity = severity.name,
            title = title
        ))
    }

    fun getHealthMetrics(): com.sentinelx.ops.dto.HealthMetricsResponse {
        val totalRequests = requestCount.get()
        val totalErrors = errorCount.get()
        val totalLatency = latencySum.get()
        val latencySamples = latencyCount.get()

        return com.sentinelx.ops.dto.HealthMetricsResponse(
            apiLatencyP50Ms = 0.0, // Would need percentile tracking
            apiLatencyP95Ms = 0.0,
            apiLatencyP99Ms = 0.0,
            errorRate = if (totalRequests > 0) totalErrors.toDouble() / totalRequests else 0.0,
            requestsPerSecond = totalRequests / 30.0, // Approximate over 30s window
            paymentProcessingLatencyMs = 0.0,
            riskEngineLatencyMs = 0.0,
            databaseLatencyMs = 0.0,
            activeIncidents = incidentRepository.findByStatusIn(
                listOf(IncidentStatus.DETECTED, IncidentStatus.INVESTIGATING),
                org.springframework.data.domain.PageRequest.of(0, 100)
            ).content.size,
            cpuUsagePercent = 0.0, // Would need system metrics
            memoryUsagePercent = 0.0,
            diskUsagePercent = 0.0
        )
    }
}