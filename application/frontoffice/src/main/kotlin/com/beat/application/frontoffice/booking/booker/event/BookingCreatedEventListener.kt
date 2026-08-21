package com.beat.application.frontoffice.booking.booker.event

import com.beat.contracts.notification.BookingNotification
import com.beat.contracts.notification.BookingNotificationPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class BookingCreatedEventListener(
    private val bookingNotificationPort: BookingNotificationPort,
) {
    @Async("beatAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun sendSlackNotification(event: BookingCreatedEvent) {
        try {
            bookingNotificationPort.send(
                BookingNotification(
                    bookingDateTime = event.bookingDateTime,
                    performanceTitle = event.performanceTitle,
                    purchaseTicketCount = event.purchaseTicketCount,
                    bookerName = event.bookerName,
                    scheduleDisplayName = event.scheduleDisplayName,
                    currentSoldTicketCount = event.currentSoldTicketCount,
                    totalTicketCount = event.totalTicketCount,
                ),
            )
        } catch (exception: RuntimeException) {
            log.error(exception) { "Booking Slack notification failed: errorType=${exception.javaClass.simpleName}" }
        }
    }

    private companion object {
        val log = KotlinLogging.logger {}
    }
}
