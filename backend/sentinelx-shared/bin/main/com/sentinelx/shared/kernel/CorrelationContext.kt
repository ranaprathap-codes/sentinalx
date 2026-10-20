package com.sentinelx.shared.kernel

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object CorrelationContext {
    private val correlationIdHolder = ThreadLocal<UUID>()
    private val baggageHolder = ThreadLocal<ConcurrentHashMap<String, String>>()

    fun getCorrelationId(): UUID = correlationIdHolder.get() ?: UUID.randomUUID()

    fun setCorrelationId(correlationId: UUID) {
        correlationIdHolder.set(correlationId)
    }

    fun clearCorrelationId() {
        correlationIdHolder.remove()
    }

    fun getBaggage(): ConcurrentHashMap<String, String> {
        val baggage = baggageHolder.get()
        if (baggage == null) {
            val newBaggage = ConcurrentHashMap<String, String>()
            baggageHolder.set(newBaggage)
            return newBaggage
        }
        return baggage
    }

    fun setBaggage(key: String, value: String) {
        getBaggage()[key] = value
    }

    fun clearBaggage() {
        baggageHolder.remove()
    }

    fun clearAll() {
        clearCorrelationId()
        clearBaggage()
    }
}