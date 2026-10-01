package com.sentinelx.payments.domain

import com.sentinelx.shared.domain.TransactionStatus
import com.sentinelx.shared.kernel.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "transactions",
    indexes = [
        Index(name = "idx_transactions_user_id", columnList = "user_id"),
        Index(name = "idx_transactions_status", columnList = "status"),
        Index(name = "idx_transactions_created_at", columnList = "created_at"),
        Index(name = "idx_transactions_correlation_id", columnList = "correlation_id"),
        Index(name = "idx_transactions_idempotency_key", columnList = "idempotency_key", unique = true)
    ]
)
class Transaction(
    idempotencyKey: String,
    userId: UUID,
    amountCents: Long,
    merchantId: String,
    correlationId: UUID
) : BaseEntity() {

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 255)
    var idempotencyKey: String = idempotencyKey

    @Column(name = "user_id", nullable = false)
    var userId: UUID = userId

    @Column(name = "amount_cents", nullable = false)
    var amountCents: Long = amountCents

    @Column(name = "currency", nullable = false, length = 3)
    var currency: String = "USD"

    @Column(name = "merchant_id", nullable = false, length = 100)
    var merchantId: String = merchantId

    @Column(name = "merchant_name", length = 255)
    var merchantName: String? = null

    @Column(name = "device_id", length = 255)
    var deviceId: String? = null

    @Column(name = "device_fingerprint", length = 500)
    var deviceFingerprint: String? = null

    @Column(name = "location_country", length = 2)
    var locationCountry: String? = null

    @Column(name = "location_region", length = 100)
    var locationRegion: String? = null

    @Column(name = "location_city", length = 100)
    var locationCity: String? = null

    @Column(name = "ip_address", columnDefinition = "INET")
    var ipAddress: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: TransactionStatus = TransactionStatus.CREATED

    @Column(name = "risk_score")
    var riskScore: Int? = null

    @Column(name = "risk_decision", length = 20)
    var riskDecision: String? = null

    @Column(name = "correlation_id", nullable = false)
    var correlationId: UUID = correlationId

    @Column(name = "metadata", columnDefinition = "JSONB")
    var metadata: String = "{}"

    @Column(name = "completed_at")
    var completedAt: Instant? = null

    constructor() : this("", UUID.randomUUID(), 0L, "", UUID.randomUUID())
}