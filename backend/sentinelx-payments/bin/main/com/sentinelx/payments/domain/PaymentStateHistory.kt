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
    name = "payment_state_history",
    indexes = [
        Index(name = "idx_payment_state_history_transaction_id", columnList = "transaction_id"),
        Index(name = "idx_payment_state_history_created_at", columnList = "created_at")
    ]
)
class PaymentStateHistory(
    transactionId: UUID,
    toStatus: TransactionStatus,
    fromStatus: TransactionStatus? = null,
    reason: String? = null,
    triggeredBy: String? = null
) : BaseEntity() {

    @Column(name = "transaction_id", nullable = false)
    var transactionId: UUID = transactionId

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    var fromStatus: TransactionStatus? = fromStatus

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    var toStatus: TransactionStatus = toStatus

    @Column(name = "reason", length = 500)
    var reason: String? = reason

    @Column(name = "triggered_by", length = 100)
    var triggeredBy: String? = triggeredBy

    @Column(name = "metadata", columnDefinition = "JSONB")
    var metadata: String = "{}"

    constructor() : this(UUID.randomUUID(), TransactionStatus.CREATED)
}