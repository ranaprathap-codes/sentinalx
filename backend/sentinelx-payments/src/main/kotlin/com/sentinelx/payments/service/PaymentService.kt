package com.sentinelx.payments.service

import com.sentinelx.auth.domain.User
import com.sentinelx.payments.domain.PaymentStateHistory
import com.sentinelx.payments.domain.Transaction
import com.sentinelx.payments.dto.CreatePaymentRequest
import com.sentinelx.payments.dto.PaymentResponse
import com.sentinelx.payments.dto.PaymentStateHistoryResponse
import com.sentinelx.payments.repository.PaymentStateHistoryRepository
import com.sentinelx.payments.repository.TransactionRepository
import com.sentinelx.payments.state.TransactionStateMachine
import com.sentinelx.shared.domain.TransactionStatus
import com.sentinelx.shared.event.DomainEvent
import com.sentinelx.shared.event.EventPublisher
import com.sentinelx.shared.event.TransactionCreatedEvent
import com.sentinelx.shared.event.TransactionStateChangedEvent
import com.sentinelx.shared.kernel.ApiResponse
import com.sentinelx.shared.kernel.BadRequestException
import com.sentinelx.shared.kernel.ConflictException
import com.sentinelx.shared.kernel.IdempotencyConflictException
import com.sentinelx.shared.kernel.NotFoundException
import com.sentinelx.shared.util.IdempotencyService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class PaymentService(
    private val transactionRepository: TransactionRepository,
    private val stateHistoryRepository: PaymentStateHistoryRepository,
    private val idempotencyService: IdempotencyService,
    private val eventPublisher: EventPublisher
) {

    fun createPayment(request: CreatePaymentRequest, user: User, correlationId: UUID): PaymentResponse {
        // Idempotency check
        if (!idempotencyService.tryAcquire(request.idempotencyKey)) {
            val existingResult = idempotencyService.getResult(request.idempotencyKey)
            if (existingResult != null) {
                val existingTransaction = transactionRepository.findByIdempotencyKey(request.idempotencyKey)
                    ?: throw IdempotencyConflictException()
                return toPaymentResponse(existingTransaction)
            }
            throw IdempotencyConflictException()
        }

        val transaction = Transaction(
            idempotencyKey = request.idempotencyKey,
            userId = user.id,
            amountCents = request.amountCents,
            merchantId = request.merchantId,
            correlationId = correlationId
        ).apply {
            merchantName = request.merchantName
            deviceId = request.deviceId
            deviceFingerprint = request.deviceFingerprint
            locationCountry = request.locationCountry
            locationRegion = request.locationRegion
            locationCity = request.locationCity
            ipAddress = request.ipAddress
            metadata = request.metadata.toString()
        }

        val savedTransaction = transactionRepository.save(transaction)
        recordStateChange(savedTransaction, null, TransactionStatus.CREATED, "Payment created", "SYSTEM")

        // Publish domain event
        eventPublisher.publish(TransactionCreatedEvent(
            correlationId = correlationId,
            transactionId = savedTransaction.id,
            userId = user.id,
            amountCents = savedTransaction.amountCents,
            merchantId = savedTransaction.merchantId
        ))

        // Complete idempotency
        idempotencyService.complete(request.idempotencyKey, savedTransaction.id.toString())

        return toPaymentResponse(savedTransaction)
    }

    fun getPayment(transactionId: UUID): PaymentResponse {
        val transaction = transactionRepository.findById(transactionId)
            .orElseThrow { NotFoundException("Payment not found: $transactionId") }
        return toPaymentResponse(transaction)
    }

    fun getPaymentByIdempotencyKey(idempotencyKey: String): PaymentResponse {
        val transaction = transactionRepository.findByIdempotencyKey(idempotencyKey)
            .orElseThrow { NotFoundException("Payment not found for idempotency key: $idempotencyKey") }
        return toPaymentResponse(transaction)
    }

    fun getUserPayments(userId: UUID, page: Int, size: Int): Page<PaymentResponse> {
        val pageable = PageRequest.of(page, size.coerceAtMost(100))
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .map { toPaymentResponse(it) }
    }

    fun getPaymentHistory(transactionId: UUID): List<PaymentStateHistoryResponse> {
        return stateHistoryRepository.findByTransactionIdOrderByCreatedAtAsc(transactionId)
            .map { toHistoryResponse(it) }
    }

    fun transitionState(
        transactionId: UUID,
        toStatus: TransactionStatus,
        reason: String? = null,
        triggeredBy: String = "SYSTEM",
        correlationId: UUID = UUID.randomUUID()
    ): PaymentResponse {
        val transaction = transactionRepository.findById(transactionId)
            .orElseThrow { NotFoundException("Payment not found: $transactionId") }

        val fromStatus = transaction.status
        TransactionStateMachine.validateTransition(fromStatus, toStatus)

        transaction.status = toStatus
        if (toStatus == TransactionStatus.COMPLETED) {
            transaction.completedAt = Instant.now()
        }
        
        val savedTransaction = transactionRepository.save(transaction)
        recordStateChange(savedTransaction, fromStatus, toStatus, reason, triggeredBy)

        // Publish domain event
        eventPublisher.publish(TransactionStateChangedEvent(
            correlationId = correlationId,
            transactionId = transactionId,
            fromStatus = fromStatus.name,
            toStatus = toStatus.name,
            reason = reason
        ))

        return toPaymentResponse(savedTransaction)
    }

    fun applyRiskDecision(
        transactionId: UUID,
        riskScore: Int,
        decision: String,
        correlationId: UUID
    ): PaymentResponse {
        val transaction = transactionRepository.findById(transactionId)
            .orElseThrow { NotFoundException("Payment not found: $transactionId") }

        transaction.riskScore = riskScore
        transaction.riskDecision = decision

        val newStatus = when (decision) {
            "ALLOW" -> TransactionStatus.AUTHORIZED
            "REVIEW" -> TransactionStatus.RISK_REVIEW
            "BLOCK" -> TransactionStatus.BLOCKED
            else -> throw BadRequestException("Invalid risk decision: $decision")
        }

        TransactionStateMachine.validateTransition(transaction.status, newStatus)
        transaction.status = newStatus
        
        val savedTransaction = transactionRepository.save(transaction)
        recordStateChange(savedTransaction, transaction.status, newStatus, "Risk engine decision: $decision (score: $riskScore)", "RISK_ENGINE")

        return toPaymentResponse(savedTransaction)
    }

    private fun recordStateChange(
        transaction: Transaction,
        fromStatus: TransactionStatus?,
        toStatus: TransactionStatus,
        reason: String?,
        triggeredBy: String
    ) {
        val history = PaymentStateHistory(
            transactionId = transaction.id,
            toStatus = toStatus,
            fromStatus = fromStatus,
            reason = reason,
            triggeredBy = triggeredBy
        )
        stateHistoryRepository.save(history)
    }

    private fun toPaymentResponse(transaction: Transaction): PaymentResponse {
        return PaymentResponse(
            id = transaction.id,
            idempotencyKey = transaction.idempotencyKey,
            amountCents = transaction.amountCents,
            currency = transaction.currency,
            merchantId = transaction.merchantId,
            merchantName = transaction.merchantName,
            status = transaction.status,
            riskScore = transaction.riskScore,
            riskDecision = transaction.riskDecision,
            correlationId = transaction.correlationId,
            createdAt = transaction.createdAt.toString(),
            updatedAt = transaction.updatedAt.toString(),
            completedAt = transaction.completedAt?.toString()
        )
    }

    private fun toHistoryResponse(history: PaymentStateHistory): PaymentStateHistoryResponse {
        return PaymentStateHistoryResponse(
            id = history.id,
            transactionId = history.transactionId,
            fromStatus = history.fromStatus,
            toStatus = history.toStatus,
            reason = history.reason,
            triggeredBy = history.triggeredBy,
            createdAt = history.createdAt.toString()
        )
    }
}