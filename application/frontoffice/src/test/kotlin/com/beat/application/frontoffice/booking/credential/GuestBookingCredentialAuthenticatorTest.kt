package com.beat.application.frontoffice.booking.credential

import com.beat.domain.booking.repository.BookingRepository
import com.beat.support.security.password.PasswordHasher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class GuestBookingCredentialAuthenticatorTest {

    @Mock
    private lateinit var bookingRepository: BookingRepository

    @Mock
    private lateinit var passwordHasher: PasswordHasher

    @Mock
    private lateinit var guestBookingCredentialRepository: GuestBookingCredentialRepository

    @Test
    fun `returns null when no candidate password matches`() {
        val credentials = listOf(GuestBookingCredential(1L, "encoded"))
        Mockito.`when`(guestBookingCredentialRepository.findCandidates("booker", "010", "990101"))
            .thenReturn(credentials)
        Mockito.`when`(passwordHasher.matches("secret", "encoded")).thenReturn(false)

        val result = authenticator().findUserId("booker", "010", "990101", "secret")

        assertNull(result)
        Mockito.verifyNoInteractions(bookingRepository)
    }

    @Test
    fun `returns the user when exactly one user matches`() {
        val credentials = listOf(GuestBookingCredential(7L, "encoded"))
        Mockito.`when`(guestBookingCredentialRepository.findCandidates("booker", "010", "990101"))
            .thenReturn(credentials)
        Mockito.`when`(passwordHasher.matches("secret", "encoded")).thenReturn(true)
        Mockito.`when`(passwordHasher.needsUpgrade("encoded")).thenReturn(false)

        val result = authenticator().findUserId("booker", "010", "990101", "secret")

        assertEquals(7L, result)
        Mockito.verifyNoInteractions(bookingRepository)
    }

    @Test
    fun `allows multiple matching credentials for one user and upgrades once`() {
        val credentials = listOf(
            GuestBookingCredential(7L, "legacy"),
            GuestBookingCredential(7L, "encoded"),
        )
        Mockito.`when`(guestBookingCredentialRepository.findCandidates("booker", "010", "990101"))
            .thenReturn(credentials)
        Mockito.`when`(passwordHasher.matches("secret", "legacy")).thenReturn(true)
        Mockito.`when`(passwordHasher.matches("secret", "encoded")).thenReturn(true)
        Mockito.`when`(passwordHasher.needsUpgrade("legacy")).thenReturn(true)
        Mockito.`when`(passwordHasher.encode("secret")).thenReturn("upgraded")

        val result = authenticator().findUserId("booker", "010", "990101", "secret")

        assertEquals(7L, result)
        Mockito.verify(bookingRepository).replaceGuestPassword(7L, "upgraded")
        Mockito.verify(passwordHasher).encode("secret")
        Mockito.verifyNoMoreInteractions(bookingRepository)
    }

    @Test
    fun `returns null and does not upgrade when different users match`() {
        val credentials = listOf(
            GuestBookingCredential(7L, "first"),
            GuestBookingCredential(8L, "second"),
        )
        Mockito.`when`(guestBookingCredentialRepository.findCandidates("booker", "010", "990101"))
            .thenReturn(credentials)
        Mockito.`when`(passwordHasher.matches("secret", "first")).thenReturn(true)
        Mockito.`when`(passwordHasher.matches("secret", "second")).thenReturn(true)

        val result = authenticator().findUserId("booker", "010", "990101", "secret")

        assertNull(result)
        Mockito.verifyNoInteractions(bookingRepository)
        Mockito.verify(passwordHasher, Mockito.never()).needsUpgrade(Mockito.anyString())
    }

    private fun authenticator() = GuestBookingCredentialAuthenticator(
        bookingRepository = bookingRepository,
        passwordHasher = passwordHasher,
        guestBookingCredentialRepository = guestBookingCredentialRepository,
    )
}
