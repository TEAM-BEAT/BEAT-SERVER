package com.beat.application.frontoffice.ticket.query

import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorType
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.domain.member.model.Member
import com.beat.domain.member.model.SocialType
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.member.vo.SocialIdentity
import com.beat.domain.performance.model.Genre
import com.beat.domain.performance.model.Performance
import com.beat.domain.performance.repository.PerformanceRepository
import com.beat.domain.performance.vo.PerformancePeriod
import com.beat.domain.performance.vo.RunningTime
import com.beat.domain.performance.vo.TicketPrice
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class TicketQueryServiceTest {

    @Mock
    private lateinit var makerTicketReader: MakerTicketReader

    @Mock
    private lateinit var performanceRepository: PerformanceRepository

    @Mock
    private lateinit var memberRepository: MemberRepository

    @Test
    fun `maps explicit schedule and booking filters and returns non-null result`() {
        stubOwner()
        val schedules = listOf(schedule(200L, "FIRST", totalTickets = 100, soldTickets = 99))
        val ticket = ticket()
        Mockito.`when`(makerTicketReader.findSchedules(100L)).thenReturn(schedules)
        Mockito.`when`(
            makerTicketReader.findTickets(
                100L,
                listOf(MakerTicketScheduleNumber.FIRST),
                listOf(MakerTicketBookingStatus.CHECKING_PAYMENT),
            ),
        ).thenReturn(listOf(ticket))

        val result = service().findAllTicketsByConditions(
            1L,
            100L,
            TicketListQuery(
                scheduleNumbers = listOf("FIRST"),
                bookingStatuses = listOf("CHECKING_PAYMENT"),
            ),
        )

        assertNotNull(result)
        assertEquals(1, result.bookingList.size)
        assertEquals("CHECKING_PAYMENT", result.bookingList.single().bookingStatus)
        assertEquals("FIRST", result.bookingList.single().scheduleNumber)
        assertTrue(result.bookingList.single().deletable)
        assertEquals("title", result.performanceTitle)
        assertEquals("team", result.performanceTeamName)
        assertEquals(100, result.totalPerformanceTicketCount)
        assertEquals(99, result.totalPerformanceSoldTicketCount)
        Mockito.verify(makerTicketReader).findTickets(
            100L,
            listOf(MakerTicketScheduleNumber.FIRST),
            listOf(MakerTicketBookingStatus.CHECKING_PAYMENT),
        )
    }

    @Test
    fun `searches with all schedules and active maker statuses when filters are omitted`() {
        stubOwner()
        Mockito.`when`(makerTicketReader.findSchedules(100L)).thenReturn(
            listOf(
                schedule(200L, "FIRST", totalTickets = 100, soldTickets = 1),
                schedule(201L, "SECOND", totalTickets = 100, soldTickets = 2),
            ),
        )
        Mockito.`when`(
            makerTicketReader.searchTickets(
                100L,
                "booker",
                listOf(MakerTicketScheduleNumber.FIRST, MakerTicketScheduleNumber.SECOND),
                listOf(
                    MakerTicketBookingStatus.REFUND_REQUESTED,
                    MakerTicketBookingStatus.CHECKING_PAYMENT,
                    MakerTicketBookingStatus.BOOKING_CONFIRMED,
                    MakerTicketBookingStatus.BOOKING_CANCELLED,
                ),
            ),
        ).thenReturn(emptyList())

        val result = service().searchAllTicketsByConditions(
            1L,
            100L,
            TicketListQuery(searchWord = "booker"),
        )

        assertNotNull(result)
        assertEquals(emptyList<TicketDetailResult>(), result.bookingList)
        Mockito.verify(makerTicketReader).searchTickets(
            100L,
            "booker",
            listOf(MakerTicketScheduleNumber.FIRST, MakerTicketScheduleNumber.SECOND),
            listOf(
                MakerTicketBookingStatus.REFUND_REQUESTED,
                MakerTicketBookingStatus.CHECKING_PAYMENT,
                MakerTicketBookingStatus.BOOKING_CONFIRMED,
                MakerTicketBookingStatus.BOOKING_CANCELLED,
            ),
        )
    }

    @Test
    fun `rejects null blank and one-character search words before dependencies`() {
        listOf(null, "", "a").forEach { searchWord ->
            assertThrows<FrontofficeApplicationException> {
                service().searchAllTicketsByConditions(1L, 100L, TicketListQuery(searchWord = searchWord))
            }
        }

        Mockito.verifyNoInteractions(makerTicketReader, performanceRepository, memberRepository)
    }

    @Test
    fun `rejects deleted booking status for list and search`() {
        val listException = assertThrows<FrontofficeApplicationException> {
            service().findAllTicketsByConditions(
                1L,
                100L,
                TicketListQuery(bookingStatuses = listOf("BOOKING_DELETED")),
            )
        }
        val searchException = assertThrows<FrontofficeApplicationException> {
            service().searchAllTicketsByConditions(
                1L,
                100L,
                TicketListQuery(searchWord = "ab", bookingStatuses = listOf("BOOKING_DELETED")),
            )
        }

        assertTicketValidationFailure(listException)
        assertTicketValidationFailure(searchException)
        Mockito.verifyNoInteractions(makerTicketReader, performanceRepository, memberRepository)
    }

    @Test
    fun `authorizes query through authoritative PerformanceRepository`() {
        Mockito.`when`(memberRepository.findById(1L)).thenReturn(Optional.of(member(userId = 10L)))
        Mockito.`when`(performanceRepository.findById(100L)).thenReturn(Optional.of(performance(userId = 11L)))

        assertThrows<FrontofficeApplicationException> {
            service().findAllTicketsByConditions(1L, 100L, TicketListQuery())
        }

        Mockito.verify(performanceRepository).findById(100L)
        Mockito.verifyNoInteractions(makerTicketReader)
    }

    private fun service() = TicketQueryService(
        makerTicketReader = makerTicketReader,
        performanceRepository = performanceRepository,
        memberRepository = memberRepository,
    )

    private fun stubOwner() {
        Mockito.`when`(memberRepository.findById(1L)).thenReturn(Optional.of(member()))
        Mockito.`when`(performanceRepository.findById(100L)).thenReturn(Optional.of(performance()))
    }

    private fun assertTicketValidationFailure(exception: FrontofficeApplicationException) {
        assertEquals(FrontofficeApplicationErrorType.INVALID_INPUT, exception.errorCode.type)
        assertEquals("삭제된 예매자를 조회할 수 없습니다.", exception.errorCode.message)
    }

    private fun member(userId: Long = 10L) = Member.rehydrate(
        1L,
        "maker",
        null,
        null,
        userId,
        SocialIdentity.of(SocialType.KAKAO, 123L),
    )

    private fun performance(userId: Long = 10L) = Performance.rehydrate(
        100L,
        "title",
        Genre.BAND,
        RunningTime.of(120),
        "description",
        "attention",
        null,
        "poster",
        "team",
        "venue",
        "road",
        "detail",
        "37.0",
        "127.0",
        "contact",
        PerformancePeriod.of(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1)),
        TicketPrice.of(10_000),
        2,
        userId,
    )

    private fun schedule(
        id: Long,
        scheduleNumber: String,
        totalTickets: Int,
        soldTickets: Int,
    ) = MakerTicketScheduleReadModel(
        id,
        totalTickets,
        soldTickets,
        scheduleNumber,
    )

    private fun ticket() = MakerTicketListItemReadModel(
        300L,
        "booker",
        "010-0000-0000",
        200L,
        1,
        LocalDateTime.of(2026, 1, 1, 12, 0),
        MakerTicketBookingStatus.CHECKING_PAYMENT,
        "카카오뱅크",
        "123",
        "holder",
        true,
    )
}
