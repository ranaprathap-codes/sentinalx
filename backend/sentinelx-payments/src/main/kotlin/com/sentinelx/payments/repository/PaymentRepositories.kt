package com.sentinelx.payments.repository

import com.sentinelx.payments.domain.PaymentStateHistory
import com.sentinelx.payments.domain.Transaction
import com.sentinelx.shared.domain.TransactionStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.List
import java.util.UUID

@Repository
interface TransactionRepository : JpaRepository<Transaction, UUID> {
    fun findByIdempotencyKey(idempotencyKey: String): Transaction?

    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId ORDER BY t.createdAt DESC")
    fun findByUserIdOrderByCreatedAtDesc(@Param("userId") userId: UUID): List<Transaction>

    @Query("SELECT t FROM Transaction t WHERE t.correlationId = :correlationId")
    fun findByCorrelationId(@Param("correlationId") correlationId: UUID): List<Transaction>

    @Query("SELECT t FROM Transaction t WHERE t.status IN :statuses")
    fun findByStatusIn(@Param("statuses") statuses: List<TransactionStatus>): List<Transaction>
}

@Repository
interface PaymentStateHistoryRepository : JpaRepository<PaymentStateHistory, UUID> {
    @Query("SELECT h FROM PaymentStateHistory h WHERE h.transactionId = :transactionId ORDER BY h.createdAt ASC")
    fun findByTransactionIdOrderByCreatedAtAsc(@Param("transactionId") transactionId: UUID): List<PaymentStateHistory>
}