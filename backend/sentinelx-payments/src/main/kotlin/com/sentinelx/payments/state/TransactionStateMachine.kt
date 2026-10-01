package com.sentinelx.payments.state

import com.sentinelx.shared.domain.TransactionStatus
import com.sentinelx.shared.kernel.InvalidStateTransitionException

object TransactionStateMachine {
    
    private val validTransitions: Map<TransactionStatus, Set<TransactionStatus>> = mapOf(
        TransactionStatus.CREATED to setOf(TransactionStatus.PROCESSING, TransactionStatus.CANCELLED, TransactionStatus.FAILED),
        TransactionStatus.PROCESSING to setOf(TransactionStatus.RISK_REVIEW, TransactionStatus.AUTHORIZED, TransactionStatus.BLOCKED, TransactionStatus.FAILED, TransactionStatus.CANCELLED),
        TransactionStatus.RISK_REVIEW to setOf(TransactionStatus.AUTHORIZED, TransactionStatus.BLOCKED, TransactionStatus.REVIEW, TransactionStatus.FAILED),
        TransactionStatus.AUTHORIZED to setOf(TransactionStatus.COMPLETED, TransactionStatus.FAILED, TransactionStatus.CANCELLED),
        TransactionStatus.COMPLETED to setOf(TransactionStatus.REFUNDED),
        TransactionStatus.BLOCKED to setOf(),
        TransactionStatus.FAILED to setOf(),
        TransactionStatus.CANCELLED to setOf(),
        TransactionStatus.REFUNDED to setOf()
    )

    fun canTransition(from: TransactionStatus, to: TransactionStatus): Boolean {
        return validTransitions[from]?.contains(to) == true
    }

    fun validateTransition(from: TransactionStatus, to: TransactionStatus) {
        if (!canTransition(from, to)) {
            throw InvalidStateTransitionException(
                "Invalid state transition from $from to $to. Valid transitions: ${validTransitions[from]?.joinToString(", ") ?: "none"}"
            )
        }
    }

    fun getValidNextStates(from: TransactionStatus): Set<TransactionStatus> {
        return validTransitions[from] ?: emptySet()
    }

    fun isTerminal(state: TransactionStatus): Boolean {
        return validTransitions[state]?.isEmpty() == true
    }

    fun isProcessing(state: TransactionStatus): Boolean {
        return state in setOf(TransactionStatus.CREATED, TransactionStatus.PROCESSING, TransactionStatus.RISK_REVIEW, TransactionStatus.AUTHORIZED)
    }
}