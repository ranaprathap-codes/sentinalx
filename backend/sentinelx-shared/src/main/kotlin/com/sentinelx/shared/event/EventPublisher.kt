package com.sentinelx.shared.event

import org.springframework.context.ApplicationEventPublisher
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class EventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val kafkaTemplate: KafkaTemplate<String, DomainEvent>?
) {

    fun publish(event: DomainEvent) {
        applicationEventPublisher.publishEvent(event)

        kafkaTemplate
            ?.send(
                "sentinelx.events",
                event.eventId.toString(),
                event
            )
            ?.whenComplete { _, _ ->
                // Kafka failures do not prevent local event publication.
            }
    }

    fun publishAll(events: List<DomainEvent>) {
        events.forEach(::publish)
    }
}