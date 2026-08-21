package com.beat.infra.external.notification.sms

import com.beat.application.frontoffice.ticket.maker.command.TicketPaymentConfirmedEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
internal class TicketPaymentConfirmedEventListener(
    private val coolSmsAdapter: CoolSmsAdapter,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("beatAsyncExecutor")
    fun sendConfirmation(event: TicketPaymentConfirmedEvent) {
        val message = "[BEAT] ${event.bookerName}님 ${event.performanceTitle} 예매 확정되었습니다."
        try {
            coolSmsAdapter.send(
                to = event.bookerPhoneNumber,
                text = message,
            )
        } catch (exception: RuntimeException) {
            log.error(
                "SMS 전송 실패 - bookingId=${event.bookingId}, errorType=${exception.javaClass.simpleName}",
                exception,
            )
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(TicketPaymentConfirmedEventListener::class.java)
    }
}
