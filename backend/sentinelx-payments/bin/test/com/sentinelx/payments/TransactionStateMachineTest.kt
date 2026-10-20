package com.sentinelx.payments

import com.sentinelx.payments.state.TransactionStateMachine
import com.sentinelx.shared.domain.TransactionStatus
import com.sentinelx.shared.kernel.InvalidStateTransitionException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TransactionStateMachineTest {

    @Test
    fun `CREATED can transition to PROCESSING`() {
        assertTrue(TransactionStateMachine.canTransition(TransactionStatus.CREATED, TransactionStatus.PROCESSING))
    }

    @Test
    fun `CREATED can transition to CANCELLED`() {
        assertTrue(TransactionStateMachine.canTransition(TransactionStatus.CREATED, TransactionStatus.CANCELLED))
    }

    @Test
    fun `CREATED cannot transition to COMPLETED directly`() {
        assertFalse(TransactionStateMachine.canTransition(TransactionStatus.CREATED, TransactionStatus.COMPLETED))
    }

    @Test
    fun `PROCESSING can transition to RISK_REVIEW`() {
        assertTrue(TransactionStateMachine.canTransition(TransactionStatus.PROCESSING, TransactionStatus.RISK_REVIEW))
    }

    @Test
    fun `PROCESSING can transition to AUTHORIZED`() {
        assertTrue(TransactionStateMachine.canTransition(TransactionStatus.PROCESSING, TransactionStatus.AUTHORIZED))
    }

    @Test
    fun `PROCESSING can transition to BLOCKED`() {
        assertTrue(TransactionStateMachine.canTransition(TransactionStatus.PROCESSING, TransactionStatus.BLOCKED))
    }

    @Test
    fun `RISK_REVIEW can transition to AUTHORIZED`() {
        assertTrue(TransactionStateMachine.canTransition(TransactionStatus.RISK_REVIEW, TransactionStatus.AUTHORIZED))
    }

    @Test
    fun `RISK_REVIEW can transition to BLOCKED`() {
        assertTrue(TransactionStateMachine.canTransition(TransactionStatus.RISK_REVIEW, TransactionStatus.BLOCKED))
    }

    @Test
    fun `AUTHORIZED can transition to COMPLETED`() {
        assertTrue(TransactionStateMachine.canTransition(TransactionStatus.AUTHORIZED, TransactionStatus.COMPLETED))
    }

    @Test
    fun `COMPLETED can transition to REFUNDED`() {
        assertTrue(TransactionStateMachine.canTransition(TransactionStatus.COMPLETED, TransactionStatus.REFUNDED))
    }

    @Test
    fun `BLOCKED is terminal state`() {
        assertTrue(TransactionStateMachine.isTerminal(TransactionStatus.BLOCKED))
        assertTrue(TransactionStateMachine.getValidNextStates(TransactionStatus.BLOCKED).isEmpty())
    }

    @Test
    fun `COMPLETED is terminal state`() {
        assertTrue(TransactionStateMachine.isTerminal(TransactionStatus.COMPLETED))
    }

    @Test
    fun `FAILED is terminal state`() {
        assertTrue(TransactionStateMachine.isTerminal(TransactionStatus.FAILED))
    }

    @Test
    fun `CANCELLED is terminal state`() {
        assertTrue(TransactionStateMachine.isTerminal(TransactionStatus.CANCELLED))
    }

    @Test
    fun `validateTransition throws for invalid transition`() {
        assertThrows<InvalidStateTransitionException> {
            TransactionStateMachine.validateTransition(TransactionStatus.CREATED, TransactionStatus.COMPLETED)
        }
    }

    @Test
    fun `validateTransition throws for backward transition`() {
        assertThrows<InvalidStateTransitionException> {
            TransactionStateMachine.validateTransition(TransactionStatus.AUTHORIZED, TransactionStatus.PROCESSING)
        }
    }

    @Test
    fun `isProcessing returns true for active states`() {
        assertTrue(TransactionStateMachine.isProcessing(TransactionStatus.CREATED))
        assertTrue(TransactionStateMachine.isProcessing(TransactionStatus.PROCESSING))
        assertTrue(TransactionStateMachine.isProcessing(TransactionStatus.RISK_REVIEW))
        assertTrue(TransactionStateMachine.isProcessing(TransactionStatus.AUTHORIZED))
    }

    @Test
    fun `isProcessing returns false for terminal states`() {
        assertFalse(TransactionStateMachine.isProcessing(TransactionStatus.COMPLETED))
        assertFalse(TransactionStateMachine.isProcessing(TransactionStatus.BLOCKED))
        assertFalse(TransactionStateMachine.isProcessing(TransactionStatus.FAILED))
        assertFalse(TransactionStateMachine.isProcessing(TransactionStatus.CANCELLED))
        assertFalse(TransactionStateMachine.isProcessing(TransactionStatus.REFUNDED))
    }
}