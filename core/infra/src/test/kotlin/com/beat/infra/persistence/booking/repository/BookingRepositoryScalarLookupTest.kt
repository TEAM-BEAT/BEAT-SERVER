package com.beat.infra.persistence.booking.repository

import com.beat.infra.persistence.booking.mapper.BookingPersistenceMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class BookingRepositoryScalarLookupTest {

    private val bookingJpaRepository = Mockito.mock(BookingJpaRepository::class.java)
    private val bookingPersistenceMapper = Mockito.mock(BookingPersistenceMapper::class.java)
    private val repository = BookingRepositoryImpl(bookingJpaRepository, bookingPersistenceMapper)

    @Test
    fun `findScheduleIdsByIds delegates to scalar query without mapping booking entities`() {
        val bookingIds = listOf(101L, 202L)
        Mockito.`when`(bookingJpaRepository.findScheduleIdsByIds(bookingIds)).thenReturn(listOf(11L, 22L))

        assertEquals(listOf(11L, 22L), repository.findScheduleIdsByIds(bookingIds))

        Mockito.verify(bookingJpaRepository).findScheduleIdsByIds(bookingIds)
        Mockito.verifyNoInteractions(bookingPersistenceMapper)
    }

    @Test
    fun `findScheduleIdsByIds skips JPA query for empty ids`() {
        assertEquals(emptyList<Long>(), repository.findScheduleIdsByIds(emptyList()))

        Mockito.verifyNoInteractions(bookingJpaRepository, bookingPersistenceMapper)
    }
}
