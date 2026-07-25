package com.beat.apis.ticket.application.event

import com.beat.contracts.sms.SmsMessage
import com.beat.contracts.sms.SmsPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class TicketPaymentConfirmedEventListener(
    private val smsPort: SmsPort,
) {
    @Async("beatAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun sendConfirmation(event: TicketPaymentConfirmedEvent) {
        val message = "[BEAT] ${event.bookerName}님 ${event.performanceTitle} 예매 확정되었습니다."
        try {
            smsPort.sendSms(
                SmsMessage(
                    to = event.bookerPhoneNumber,
                    text = message,
                ),
            )
        } catch (exception: RuntimeException) {
            log.error(exception) { "SMS 전송 실패 - bookingId=${event.bookingId}, errorType=${exception.javaClass.simpleName}" }
        }
    }

    private companion object {
        val log = KotlinLogging.logger {}
    }
}
