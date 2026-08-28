package com.beat.application.frontoffice.booking.booker.command

import com.beat.application.frontoffice.booking.booker.command.credential.GuestBookingCredentialAuthenticator
import com.beat.application.frontoffice.booking.booker.exception.BookingApplicationErrorCode
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class GuestBookingAccessServiceSpec :
    FunSpec({
        test("게스트 인증을 마치면 throttle을 초기화하고 authoritative userId만 반환한다") {
            val dependencies = AccessDependencies()
            dependencies.stubAuthenticatedUser()

            val result = dependencies.service().authenticate(COMMAND, CLIENT_ADDRESS)

            result.userId shouldBe USER_ID
            verify { dependencies.throttle.reset(THROTTLE_KEY) }
            verify(exactly = 0) { dependencies.sessionStore.issue(any()) }
        }

        test("일치하는 게스트 예매가 없으면 실패를 기록하고 session 발급을 수행하지 않는다") {
            val dependencies = AccessDependencies()
            every {
                dependencies.credentialAuthenticator.findUserId(any(), any(), any(), any())
            } returns null

            val exception =
                shouldThrow<FrontofficeApplicationException> {
                    dependencies.service().authenticate(COMMAND, CLIENT_ADDRESS)
                }

            exception.errorCode shouldBe BookingApplicationErrorCode.NO_BOOKING_FOUND
            verify { dependencies.throttle.recordFailure(THROTTLE_KEY) }
            verify(exactly = 0) { dependencies.sessionStore.issue(any()) }
        }

        test("인증 후 별도 session 발급 use-case가 session token을 반환한다") {
            val dependencies = AccessDependencies()
            every { dependencies.sessionStore.issue(USER_ID) } returns SESSION_TOKEN

            val token = dependencies.service().issueSession(USER_ID)

            token shouldBe SESSION_TOKEN
            verify { dependencies.sessionStore.issue(USER_ID) }
        }
    })

private class AccessDependencies {
    val credentialAuthenticator = mockk<GuestBookingCredentialAuthenticator>(relaxed = true)
    val throttle = mockk<GuestAccessThrottle>(relaxed = true)
    val sessionStore = mockk<GuestSessionStore>(relaxed = true)

    fun service(): GuestBookingAccessService =
        GuestBookingAccessService(
            credentialAuthenticator,
            throttle,
            GuestBookingSessionManager(sessionStore),
        )

    fun stubAuthenticatedUser() {
        every {
            credentialAuthenticator.findUserId("booker", "010-0000-0000", "990101", "1234")
        } returns USER_ID
    }
}

private val COMMAND =
    GuestBookingAuthenticationCommand.of(
        bookerName = "booker",
        birthDate = "990101",
        bookerPhoneNumber = "010-0000-0000",
        password = "1234",
    )
private const val USER_ID = 7L
private const val CLIENT_ADDRESS = "127.0.0.1"
private const val SESSION_TOKEN = "guest-session"
private const val THROTTLE_KEY = "$CLIENT_ADDRESS|booker|010-0000-0000|990101"
