package com.beat.application.frontoffice.booking.booker.command

import com.beat.application.frontoffice.booking.booker.BookingApplicationErrorCode
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class GuestBookingSessionManagerSpec :
    FunSpec({
        test("member actor는 guest session 조회 없이 member id를 사용한다") {
            val store = mockk<GuestSessionStore>(relaxed = true)
            val manager = GuestBookingSessionManager(store)
            val actor = BookingActorCommand(memberId = 7L, guestSessionToken = "guest-session")

            val result = manager.resolveActorUserId(actor)

            result shouldBe 7L
            verify(exactly = 0) { store.findUserId(any()) }
        }

        test("유효한 guest session은 actor user id로 해석한다") {
            val store = mockk<GuestSessionStore>(relaxed = true)
            every { store.findUserId("guest-session") } returns 11L
            val manager = GuestBookingSessionManager(store)
            val actor = BookingActorCommand(memberId = null, guestSessionToken = "guest-session")

            val result = manager.resolveActorUserId(actor)

            result shouldBe 11L
        }

        test("guest session token이 없거나 비어 있으면 인증 필요로 거부한다") {
            val store = mockk<GuestSessionStore>(relaxed = true)
            val manager = GuestBookingSessionManager(store)

            listOf<String?>(null, "", "   ").forEach { token ->
                val exception =
                    shouldThrow<FrontofficeApplicationException> {
                        manager.resolveActorUserId(
                            BookingActorCommand(memberId = null, guestSessionToken = token)
                        )
                    }

                exception.errorCode shouldBe BookingApplicationErrorCode.AUTHENTICATION_REQUIRED
            }

            verify(exactly = 0) { store.findUserId(any()) }
        }

        test("만료된 guest session은 인증 필요로 거부한다") {
            val store = mockk<GuestSessionStore>(relaxed = true)
            every { store.findUserId("expired-session") } returns null
            val manager = GuestBookingSessionManager(store)

            val exception =
                shouldThrow<FrontofficeApplicationException> {
                    manager.resolveActorUserId(
                        BookingActorCommand(memberId = null, guestSessionToken = "expired-session")
                    )
                }

            exception.errorCode shouldBe BookingApplicationErrorCode.AUTHENTICATION_REQUIRED
        }
    })
