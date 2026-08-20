package com.beat.infra.external.notification.sms

import com.beat.application.frontoffice.ticket.command.TicketPaymentConfirmedEvent
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.scheduling.annotation.Async
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

class TicketPaymentConfirmedEventListenerTest {
    @Test
    fun `sends exact recipient and confirmation message`() {
        val coolSmsAdapter = Mockito.mock(CoolSmsAdapter::class.java)
        val listener = TicketPaymentConfirmedEventListener(coolSmsAdapter)

        listener.sendConfirmation(event())

        Mockito.verify(coolSmsAdapter).send(PHONE_NUMBER, MESSAGE)
    }

    @Test
    fun `swallows runtime exception from sms adapter`() {
        val coolSmsAdapter = Mockito.mock(CoolSmsAdapter::class.java)
        val listener = TicketPaymentConfirmedEventListener(coolSmsAdapter)
        Mockito.doThrow(RuntimeException("SMS provider unavailable"))
            .`when`(coolSmsAdapter)
            .send(PHONE_NUMBER, MESSAGE)

        assertDoesNotThrow { listener.sendConfirmation(event()) }
    }

    @Test
    fun `listens after commit on beat async executor`() {
        val method =
            TicketPaymentConfirmedEventListener::class.java.getDeclaredMethod(
                "sendConfirmation",
                TicketPaymentConfirmedEvent::class.java,
            )
        val transactionalEventListener =
            requireNotNull(method.getAnnotation(TransactionalEventListener::class.java))
        val async = requireNotNull(method.getAnnotation(Async::class.java))

        assertEquals(TransactionPhase.AFTER_COMMIT, transactionalEventListener.phase)
        assertEquals("beatAsyncExecutor", async.value)
    }

    private fun event() =
        TicketPaymentConfirmedEvent(
            bookingId = 1L,
            bookerName = "booker",
            bookerPhoneNumber = PHONE_NUMBER,
            performanceTitle = "performance",
        )

    private companion object {
        const val PHONE_NUMBER = "010-0000-0000"
        const val MESSAGE = "[BEAT] booker님 performance 예매 확정되었습니다."
    }
}
