package com.beat.infra.external.notification.sms

import com.beat.application.frontoffice.ticket.maker.command.TicketPaymentConfirmedEvent
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.mockito.Mockito
import org.springframework.scheduling.annotation.Async
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

class TicketPaymentConfirmedEventListenerTest : FunSpec({
    test("sends exact recipient and confirmation message") {
        val coolSmsAdapter = Mockito.mock(CoolSmsAdapter::class.java)
        val listener = TicketPaymentConfirmedEventListener(coolSmsAdapter)

        listener.sendConfirmation(event())

        Mockito.verify(coolSmsAdapter).send(PHONE_NUMBER, MESSAGE)
    }

    test("swallows runtime exception from sms adapter") {
        val coolSmsAdapter = Mockito.mock(CoolSmsAdapter::class.java)
        val listener = TicketPaymentConfirmedEventListener(coolSmsAdapter)
        Mockito.doThrow(RuntimeException("SMS provider unavailable"))
            .`when`(coolSmsAdapter)
            .send(PHONE_NUMBER, MESSAGE)

        shouldNotThrowAny { listener.sendConfirmation(event()) }
    }

    test("listens after commit on beat async executor") {
        val method =
            TicketPaymentConfirmedEventListener::class.java.getDeclaredMethod(
                "sendConfirmation",
                TicketPaymentConfirmedEvent::class.java,
            )
        val transactionalEventListener =
            requireNotNull(method.getAnnotation(TransactionalEventListener::class.java))
        val async = requireNotNull(method.getAnnotation(Async::class.java))

        transactionalEventListener.phase shouldBe TransactionPhase.AFTER_COMMIT
        async.value shouldBe "beatAsyncExecutor"
    }

})

private fun event() =
    TicketPaymentConfirmedEvent(
        bookingId = 1L,
        bookerName = "booker",
        bookerPhoneNumber = PHONE_NUMBER,
        performanceTitle = "performance",
    )

private const val PHONE_NUMBER = "010-0000-0000"
private const val MESSAGE = "[BEAT] booker님 performance 예매 확정되었습니다."
