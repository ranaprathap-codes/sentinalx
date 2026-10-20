package com.sentinelx.payments.controller

import com.sentinelx.auth.domain.User
import com.sentinelx.auth.repository.UserRepository
import com.sentinelx.payments.dto.CreatePaymentRequest
import com.sentinelx.payments.dto.PaymentListResponse
import com.sentinelx.payments.dto.PaymentResponse
import com.sentinelx.payments.dto.PaymentStateHistoryResponse
import com.sentinelx.payments.service.PaymentService
import com.sentinelx.shared.domain.UserRole
import com.sentinelx.shared.kernel.ApiResponse
import com.sentinelx.shared.kernel.CorrelationContext
import com.sentinelx.shared.kernel.UnauthorizedException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Payment processing and management")
class PaymentController(
    private val paymentService: PaymentService,
    private val userRepository: UserRepository
) {

    @PostMapping
    @Operation(summary = "Create a new payment")
    fun createPayment(
        @Valid @RequestBody request: CreatePaymentRequest,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<PaymentResponse>> {
        val correlationId = CorrelationContext.getCorrelationId()
        val user = getCurrentUser(authentication)
        val response = paymentService.createPayment(request, user, correlationId)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID")
    fun getPayment(
        @PathVariable id: UUID,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<PaymentResponse>> {
        val user = getCurrentUser(authentication)
        val isAdmin = user.role == UserRole.ADMIN
        val response = paymentService.getPayment(id, user.id, isAdmin)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @GetMapping("/idempotency/{key}")
    @Operation(summary = "Get payment by idempotency key")
    fun getPaymentByIdempotencyKey(@PathVariable key: String): ResponseEntity<ApiResponse<PaymentResponse>> {
        val response = paymentService.getPaymentByIdempotencyKey(key)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @GetMapping
    @Operation(summary = "List user payments")
    fun listPayments(
        authentication: Authentication,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<PaymentListResponse>> {
        val user = getCurrentUser(authentication)
        val pageResult = paymentService.getUserPayments(user.id, page, size)
        
        val response = PaymentListResponse(
            payments = pageResult.content,
            page = pageResult.number,
            size = pageResult.size,
            totalElements = pageResult.totalElements,
            totalPages = pageResult.totalPages
        )
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Get payment state history")
    fun getPaymentHistory(
        @PathVariable id: UUID,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<List<PaymentStateHistoryResponse>>> {
        val user = getCurrentUser(authentication)
        val isAdmin = user.role == UserRole.ADMIN
        val history = paymentService.getPaymentHistory(id, user.id, isAdmin)
        return ResponseEntity.ok(ApiResponse.ok(history))
    }

    private fun getCurrentUser(authentication: Authentication): User {
        return userRepository.findActiveByEmail(authentication.name)
            ?: throw UnauthorizedException("User not authenticated or account inactive")
    }
}