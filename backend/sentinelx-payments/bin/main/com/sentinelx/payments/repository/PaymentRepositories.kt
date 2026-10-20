package com.sentinelx.payments.repository

import com.sentinelx.payments.domain.PaymentStateHistory
import com.sentinelx.payments.domain.Transaction
import com.sentinelx.shared.domain.TransactionStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface TransactionRepository : JpaRepository<Transaction, UUID> {
    fun findByIdempotencyKey(idempotencyKey: String): Transaction?

    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId ORDER BY t.createdAt DESC")
    fun findByUserIdOrderByCreatedAtDesc(@Param("userId") userId: UUID, pageable: Pageable): Page<Transaction>

    @Query("SELECT t FROM Transaction t WHERE t.correlationId = :correlationId")
    fun findByCorrelationId(@Param("correlationId") correlationId: UUID): List<Transaction>

    @Query("SELECT t FROM Transaction t WHERE t.status IN :statuses")
    fun findByStatusIn(@Param("statuses") statuses: List<TransactionStatus>): List<Transaction>

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.userId = :userId AND t.createdAt > :since")
    fun countByUserIdAndCreatedAtAfter(@Param("userId") userId: UUID, @Param("since") since: Instant): Long

    @Query("SELECT COALESCE(AVG(t.amountCents), 0.0) FROM Transaction t WHERE t.userId = :userId")
    fun avgAmountByUserId(@Param("userId") userId: UUID): Double

    @Query("SELECT COALESCE(MAX(t.amountCents), 0L) FROM Transaction t WHERE t.userId = :userId")
    fun maxAmountByUserId(@Param("userId") userId: UUID): Long

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM Transaction t WHERE t.userId = :userId AND t.deviceId = :deviceId")
    fun existsByUserIdAndDeviceId(@Param("userId") userId: UUID, @Param("deviceId") deviceId: String): Boolean

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM Transaction t WHERE t.userId = :userId AND t.locationCountry = :country")
    fun existsByUserIdAndLocationCountry(@Param("userId") userId: UUID, @Param("country") country: String): Boolean

    @Query("SELECT t.createdAt FROM Transaction t WHERE t.userId = :userId AND t.id != :excludeId ORDER BY t.createdAt DESC")
    fun findLastTransactionTime(@Param("userId") userId: UUID, @Param("excludeId") excludeId: UUID, pageable: Pageable): Page<Instant>
}

@Repository
interface PaymentStateHistoryRepository : JpaRepository<PaymentStateHistory, UUID> {
    @Query("SELECT h FROM PaymentStateHistory h WHERE h.transactionId = :transactionId ORDER BY h.createdAt ASC")
    fun findByTransactionIdOrderByCreatedAtAsc(@Param("transactionId") transactionId: UUID): List<PaymentStateHistory>
}