package com.beat.infrastructure.external.notification.sms

import com.beat.application.frontoffice.ticket.maker.command.TicketPaymentConfirmedEvent
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.scheduling.annotation.Async
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

class TicketPaymentConfirmedEventListenerTest :
    FunSpec({
        test("정확한 수신자와 결제 확인 메시지를 전송한다") {
            val coolSmsAdapter = mockk<CoolSmsAdapter>(relaxed = true)
            val listener = TicketPaymentConfirmedEventListener(coolSmsAdapter)

            listener.sendConfirmation(event())

            verify { coolSmsAdapter.send(PHONE_NUMBER, MESSAGE) }
        }

        test("sms adapter의 runtime exception을 삼킨다") {
            val coolSmsAdapter = mockk<CoolSmsAdapter>(relaxed = true)
            val listener = TicketPaymentConfirmedEventListener(coolSmsAdapter)
            every { coolSmsAdapter.send(PHONE_NUMBER, MESSAGE) } throws
                RuntimeException("SMS provider unavailable")

            shouldNotThrowAny { listener.sendConfirmation(event()) }
        }

        test("commit 후 beat async executor에서 수신한다") {
            val method =
                TicketPaymentConfirmedEventListener::class
                    .java
                    .getDeclaredMethod(
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
