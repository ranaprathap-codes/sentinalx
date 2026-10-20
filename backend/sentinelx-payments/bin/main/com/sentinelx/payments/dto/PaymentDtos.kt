package com.sentinelx.payments.dto

import com.sentinelx.shared.domain.TransactionStatus
import jakarta.validation.constraints.*
import java.util.UUID

data class CreatePaymentRequest(
    @NotBlank val idempotencyKey: String,
    @NotNull @Positive val amountCents: Long,
    @Size(min = 3, max = 3) val currency: String = "USD",
    @NotBlank @Size(max = 100) val merchantId: String,
    @Size(max = 255) val merchantName: String? = null,
    @Size(max = 255) val deviceId: String? = null,
    @Size(max = 500) val deviceFingerprint: String? = null,
    @Size(max = 2) val locationCountry: String? = null,
    @Size(max = 100) val locationRegion: String? = null,
    @Size(max = 100) val locationCity: String? = null,
    val ipAddress: String? = null,
    val metadata: Map<String, Any> = emptyMap()
)

data class PaymentResponse(
    val id: UUID,
    val idempotencyKey: String,
    val amountCents: Long,
    val currency: String,
    val merchantId: String,
    val merchantName: String?,
    val status: TransactionStatus,
    val riskScore: Int?,
    val riskDecision: String?,
    val correlationId: UUID,
    val createdAt: String,
    val updatedAt: String,
    val completedAt: String?
)

data class PaymentStateHistoryResponse(
    val id: UUID,
    val transactionId: UUID,
    val fromStatus: TransactionStatus?,
    val toStatus: TransactionStatus,
    val reason: String?,
    val triggeredBy: String?,
    val createdAt: String
)

data class PaymentListResponse(
    val payments: List<PaymentResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)