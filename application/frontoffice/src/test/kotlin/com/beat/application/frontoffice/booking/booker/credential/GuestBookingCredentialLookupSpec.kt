package com.beat.application.frontoffice.booking.booker.credential

import com.beat.domain.booking.repository.BookingRepository
import com.beat.support.security.password.PasswordHasher
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.Called
import io.mockk.verify

class GuestBookingCredentialLookupSpec : FunSpec({
    isolationMode = IsolationMode.SingleInstance

    test("비밀번호가 일치하는 credential이 없으면 userId를 반환하지 않는다") {
        val bookingRepository = mockk<BookingRepository>(relaxed = true)
        val passwordHasher = mockk<PasswordHasher>(relaxed = true)
        val credentialRepository = mockk<GuestBookingCredentialRepository>(relaxed = true)
        every { credentialRepository.findCandidates("booker", "010", "990101") } returns
            listOf(GuestBookingCredential(1L, "encoded"))
        every { passwordHasher.matches("secret", "encoded") } returns false

        val result = authenticator(bookingRepository, passwordHasher, credentialRepository)
            .findUserId("booker", "010", "990101", "secret")

        result shouldBe null
        verify { bookingRepository wasNot Called }
    }

    test("정확히 한 user만 password가 일치하면 해당 userId를 반환한다") {
        val bookingRepository = mockk<BookingRepository>(relaxed = true)
        val passwordHasher = mockk<PasswordHasher>(relaxed = true)
        val credentialRepository = mockk<GuestBookingCredentialRepository>(relaxed = true)
        every { credentialRepository.findCandidates("booker", "010", "990101") } returns
            listOf(GuestBookingCredential(7L, "encoded"))
        every { passwordHasher.matches("secret", "encoded") } returns true
        every { passwordHasher.needsUpgrade("encoded") } returns false

        val result = authenticator(bookingRepository, passwordHasher, credentialRepository)
            .findUserId("booker", "010", "990101", "secret")

        result shouldBe 7L
        verify { bookingRepository wasNot Called }
    }

    test("한 user의 여러 credential이 일치하면 upgrade를 한 번만 수행한다") {
        val bookingRepository = mockk<BookingRepository>(relaxed = true)
        val passwordHasher = mockk<PasswordHasher>(relaxed = true)
        val credentialRepository = mockk<GuestBookingCredentialRepository>(relaxed = true)
        every { credentialRepository.findCandidates("booker", "010", "990101") } returns
            listOf(
                GuestBookingCredential(7L, "legacy"),
                GuestBookingCredential(7L, "encoded"),
            )
        every { passwordHasher.matches("secret", "legacy") } returns true
        every { passwordHasher.matches("secret", "encoded") } returns true
        every { passwordHasher.needsUpgrade("legacy") } returns true
        every { passwordHasher.encode("secret") } returns "upgraded"

        val result = authenticator(bookingRepository, passwordHasher, credentialRepository)
            .findUserId("booker", "010", "990101", "secret")

        result shouldBe 7L
        verify { bookingRepository.replaceGuestPassword(7L, "upgraded") }
        verify { passwordHasher.encode("secret") }
        confirmVerified(bookingRepository)
    }

    test("서로 다른 user가 일치하면 모호한 인증으로 처리하고 upgrade하지 않는다") {
        val bookingRepository = mockk<BookingRepository>(relaxed = true)
        val passwordHasher = mockk<PasswordHasher>(relaxed = true)
        val credentialRepository = mockk<GuestBookingCredentialRepository>(relaxed = true)
        every { credentialRepository.findCandidates("booker", "010", "990101") } returns
            listOf(
                GuestBookingCredential(7L, "first"),
                GuestBookingCredential(8L, "second"),
            )
        every { passwordHasher.matches("secret", "first") } returns true
        every { passwordHasher.matches("secret", "second") } returns true

        val result = authenticator(bookingRepository, passwordHasher, credentialRepository)
            .findUserId("booker", "010", "990101", "secret")

        result shouldBe null
        verify { bookingRepository wasNot Called }
        verify(exactly = 0) { passwordHasher.needsUpgrade(any<String>()) }
    }
})

private fun authenticator(
    bookingRepository: BookingRepository,
    passwordHasher: PasswordHasher,
    credentialRepository: GuestBookingCredentialRepository,
): GuestBookingCredentialAuthenticator = GuestBookingCredentialAuthenticator(
    bookingRepository = bookingRepository,
    passwordHasher = passwordHasher,
    guestBookingCredentialRepository = credentialRepository,
)
