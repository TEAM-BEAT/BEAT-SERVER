package com.beat.application.frontoffice.booking.booker.command.event

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
internal class BookingCreatedEventListener(
    private val bookingNotificationSender: BookingNotificationSender
) {
    @Async("beatAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun sendSlackNotification(event: BookingCreatedEvent) {
        try {
            bookingNotificationSender.send(event)
        } catch (exception: RuntimeException) {
            log.error(exception) {
                "Booking Slack notification failed: errorType=${exception.javaClass.simpleName}"
            }
        }
    }

    private companion object {
        val log = KotlinLogging.logger {}
    }
}
