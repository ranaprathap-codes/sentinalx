package com.sentinelx.shared.event

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.Instant
import java.util.UUID

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "eventType"
)
@JsonSubTypes(
    JsonSubTypes.Type(
        value = TransactionCreatedEvent::class,
        name = "TransactionCreated"
    ),
    JsonSubTypes.Type(
        value = TransactionStateChangedEvent::class,
        name = "TransactionStateChanged"
    ),
    JsonSubTypes.Type(
        value = RiskEvaluationCompletedEvent::class,
        name = "RiskEvaluationCompleted"
    ),
    JsonSubTypes.Type(
        value = SimulationCompletedEvent::class,
        name = "SimulationCompleted"
    ),
    JsonSubTypes.Type(
        value = ReportCreatedEvent::class,
        name = "ReportCreated"
    ),
    JsonSubTypes.Type(
        value = ReportStatusChangedEvent::class,
        name = "ReportStatusChanged"
    ),
    JsonSubTypes.Type(
        value = DryRunCompletedEvent::class,
        name = "DryRunCompleted"
    ),
    JsonSubTypes.Type(
        value = IncidentCreatedEvent::class,
        name = "IncidentCreated"
    ),
    JsonSubTypes.Type(
        value = HeadlineCreatedEvent::class,
        name = "HeadlineCreated"
    )
)
sealed interface DomainEvent {
    val eventId: UUID
    val eventType: String
    val occurredAt: Instant
    val correlationId: UUID
    val causationId: UUID?
    val payload: Map<String, Any?>
}

abstract class BaseDomainEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredAt: Instant = Instant.now(),
    override val correlationId: UUID,
    override val causationId: UUID? = null,
    override val payload: Map<String, Any?> = emptyMap()
) : DomainEvent {
    override val eventType: String = this.javaClass.simpleName
}

data class TransactionCreatedEvent(
    override val correlationId: UUID,
    override val causationId: UUID? = null,
    val transactionId: UUID,
    val userId: UUID,
    val amountCents: Long,
    val merchantId: String
) : BaseDomainEvent(
    correlationId = correlationId,
    causationId = causationId,
    payload = mapOf(
        "transactionId" to transactionId,
        "userId" to userId,
        "amountCents" to amountCents,
        "merchantId" to merchantId
    )
)

data class TransactionStateChangedEvent(
    override val correlationId: UUID,
    override val causationId: UUID? = null,
    val transactionId: UUID,
    val fromStatus: String?,
    val toStatus: String,
    val reason: String?
) : BaseDomainEvent(
    correlationId = correlationId,
    causationId = causationId,
    payload = mapOf(
        "transactionId" to transactionId,
        "fromStatus" to fromStatus,
        "toStatus" to toStatus,
        "reason" to reason
    )
)

data class RiskEvaluationCompletedEvent(
    override val correlationId: UUID,
    override val causationId: UUID? = null,
    val transactionId: UUID,
    val riskScore: Int,
    val decision: String,
    val triggeredRules: List<Map<String, Any>>
) : BaseDomainEvent(
    correlationId = correlationId,
    causationId = causationId,
    payload = mapOf(
        "transactionId" to transactionId,
        "riskScore" to riskScore,
        "decision" to decision,
        "triggeredRules" to triggeredRules
    )
)

data class SimulationCompletedEvent(
    override val correlationId: UUID,
    override val causationId: UUID? = null,
    val simulationId: UUID,
    val scenarioId: UUID,
    val detected: Boolean,
    val durationMs: Long
) : BaseDomainEvent(
    correlationId = correlationId,
    causationId = causationId,
    payload = mapOf(
        "simulationId" to simulationId,
        "scenarioId" to scenarioId,
        "detected" to detected,
        "durationMs" to durationMs
    )
)

data class ReportCreatedEvent(
    override val correlationId: UUID,
    override val causationId: UUID? = null,
    val reportId: UUID,
    val type: String,
    val severity: String?,
    val component: String?,
    val reporterId: UUID
) : BaseDomainEvent(
    correlationId = correlationId,
    causationId = causationId,
    payload = mapOf(
        "reportId" to reportId,
        "type" to type,
        "severity" to severity,
        "component" to component,
        "reporterId" to reporterId
    )
)

data class ReportStatusChangedEvent(
    override val correlationId: UUID,
    override val causationId: UUID? = null,
    val reportId: UUID,
    val fromStatus: String,
    val toStatus: String,
    val changedBy: UUID
) : BaseDomainEvent(
    correlationId = correlationId,
    causationId = causationId,
    payload = mapOf(
        "reportId" to reportId,
        "fromStatus" to fromStatus,
        "toStatus" to toStatus,
        "changedBy" to changedBy
    )
)

data class DryRunCompletedEvent(
    override val correlationId: UUID,
    override val causationId: UUID? = null,
    val dryRunId: UUID,
    val reportId: UUID?,
    val transactionsTested: Int,
    val falsePositiveDelta: Int,
    val falseNegativeDelta: Int
) : BaseDomainEvent(
    correlationId = correlationId,
    causationId = causationId,
    payload = mapOf(
        "dryRunId" to dryRunId,
        "reportId" to reportId,
        "transactionsTested" to transactionsTested,
        "falsePositiveDelta" to falsePositiveDelta,
        "falseNegativeDelta" to falseNegativeDelta
    )
)

data class IncidentCreatedEvent(
    override val correlationId: UUID,
    override val causationId: UUID? = null,
    val incidentId: UUID,
    val type: String,
    val severity: String,
    val title: String
) : BaseDomainEvent(
    correlationId = correlationId,
    causationId = causationId,
    payload = mapOf(
        "incidentId" to incidentId,
        "type" to type,
        "severity" to severity,
        "title" to title
    )
)

data class HeadlineCreatedEvent(
    override val correlationId: UUID,
    override val causationId: UUID? = null,
    val headlineId: UUID,
    val title: String,
    val reportCount: Int,
    val affectedComponent: String?
) : BaseDomainEvent(
    correlationId = correlationId,
    causationId = causationId,
    payload = mapOf(
        "headlineId" to headlineId,
        "title" to title,
        "reportCount" to reportCount,
        "affectedComponent" to affectedComponent
    )
)