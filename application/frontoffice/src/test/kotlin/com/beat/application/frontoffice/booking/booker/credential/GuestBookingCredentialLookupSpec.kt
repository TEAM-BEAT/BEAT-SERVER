package com.beat.application.frontoffice.booking.booker.credential

import com.beat.domain.booking.repository.BookingRepository
import com.beat.support.security.password.PasswordHasher
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.mockito.Mockito

class GuestBookingCredentialLookupSpec : FunSpec({
    isolationMode = IsolationMode.SingleInstance

    test("비밀번호가 일치하는 credential이 없으면 userId를 반환하지 않는다") {
        val bookingRepository = Mockito.mock(BookingRepository::class.java)
        val passwordHasher = Mockito.mock(PasswordHasher::class.java)
        val credentialRepository = Mockito.mock(GuestBookingCredentialRepository::class.java)
        Mockito.`when`(credentialRepository.findCandidates("booker", "010", "990101"))
            .thenReturn(listOf(GuestBookingCredential(1L, "encoded")))
        Mockito.`when`(passwordHasher.matches("secret", "encoded")).thenReturn(false)

        val result = authenticator(bookingRepository, passwordHasher, credentialRepository)
            .findUserId("booker", "010", "990101", "secret")

        result shouldBe null
        Mockito.verifyNoInteractions(bookingRepository)
    }

    test("정확히 한 user만 password가 일치하면 해당 userId를 반환한다") {
        val bookingRepository = Mockito.mock(BookingRepository::class.java)
        val passwordHasher = Mockito.mock(PasswordHasher::class.java)
        val credentialRepository = Mockito.mock(GuestBookingCredentialRepository::class.java)
        Mockito.`when`(credentialRepository.findCandidates("booker", "010", "990101"))
            .thenReturn(listOf(GuestBookingCredential(7L, "encoded")))
        Mockito.`when`(passwordHasher.matches("secret", "encoded")).thenReturn(true)
        Mockito.`when`(passwordHasher.needsUpgrade("encoded")).thenReturn(false)

        val result = authenticator(bookingRepository, passwordHasher, credentialRepository)
            .findUserId("booker", "010", "990101", "secret")

        result shouldBe 7L
        Mockito.verifyNoInteractions(bookingRepository)
    }

    test("한 user의 여러 credential이 일치하면 upgrade를 한 번만 수행한다") {
        val bookingRepository = Mockito.mock(BookingRepository::class.java)
        val passwordHasher = Mockito.mock(PasswordHasher::class.java)
        val credentialRepository = Mockito.mock(GuestBookingCredentialRepository::class.java)
        Mockito.`when`(credentialRepository.findCandidates("booker", "010", "990101"))
            .thenReturn(
                listOf(
                    GuestBookingCredential(7L, "legacy"),
                    GuestBookingCredential(7L, "encoded"),
                ),
            )
        Mockito.`when`(passwordHasher.matches("secret", "legacy")).thenReturn(true)
        Mockito.`when`(passwordHasher.matches("secret", "encoded")).thenReturn(true)
        Mockito.`when`(passwordHasher.needsUpgrade("legacy")).thenReturn(true)
        Mockito.`when`(passwordHasher.encode("secret")).thenReturn("upgraded")

        val result = authenticator(bookingRepository, passwordHasher, credentialRepository)
            .findUserId("booker", "010", "990101", "secret")

        result shouldBe 7L
        Mockito.verify(bookingRepository).replaceGuestPassword(7L, "upgraded")
        Mockito.verify(passwordHasher).encode("secret")
        Mockito.verifyNoMoreInteractions(bookingRepository)
    }

    test("서로 다른 user가 일치하면 모호한 인증으로 처리하고 upgrade하지 않는다") {
        val bookingRepository = Mockito.mock(BookingRepository::class.java)
        val passwordHasher = Mockito.mock(PasswordHasher::class.java)
        val credentialRepository = Mockito.mock(GuestBookingCredentialRepository::class.java)
        Mockito.`when`(credentialRepository.findCandidates("booker", "010", "990101"))
            .thenReturn(
                listOf(
                    GuestBookingCredential(7L, "first"),
                    GuestBookingCredential(8L, "second"),
                ),
            )
        Mockito.`when`(passwordHasher.matches("secret", "first")).thenReturn(true)
        Mockito.`when`(passwordHasher.matches("secret", "second")).thenReturn(true)

        val result = authenticator(bookingRepository, passwordHasher, credentialRepository)
            .findUserId("booker", "010", "990101", "secret")

        result shouldBe null
        Mockito.verifyNoInteractions(bookingRepository)
        Mockito.verify(passwordHasher, Mockito.never()).needsUpgrade(Mockito.anyString())
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
