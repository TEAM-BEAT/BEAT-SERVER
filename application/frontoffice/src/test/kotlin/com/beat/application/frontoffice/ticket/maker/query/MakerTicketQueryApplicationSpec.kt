package com.beat.application.frontoffice.ticket.maker.query

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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.mockito.Mockito
import java.time.LocalDate
import java.time.LocalDateTime

class MakerTicketQueryApplicationSpec : FunSpec() {

    private lateinit var makerTicketReader: MakerTicketReader

    private lateinit var performanceRepository: PerformanceRepository

    private lateinit var memberRepository: MemberRepository

    init {
        beforeTest {
            makerTicketReader = Mockito.mock(MakerTicketReader::class.java)
            performanceRepository = Mockito.mock(PerformanceRepository::class.java)
            memberRepository = Mockito.mock(MemberRepository::class.java)
        }

    test("maps explicit schedule and booking filters and returns non-null result") {
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

        result.bookingList.size shouldBe 1
        result.bookingList.single().bookingStatus shouldBe "CHECKING_PAYMENT"
        result.bookingList.single().scheduleNumber shouldBe "FIRST"
        result.bookingList.single().deletable shouldBe true
        result.performanceTitle shouldBe "title"
        result.performanceTeamName shouldBe "team"
        result.totalPerformanceTicketCount shouldBe 100
        result.totalPerformanceSoldTicketCount shouldBe 99
        Mockito.verify(makerTicketReader).findTickets(
            100L,
            listOf(MakerTicketScheduleNumber.FIRST),
            listOf(MakerTicketBookingStatus.CHECKING_PAYMENT),
        )
    }

    test("searches with all schedules and active maker statuses when filters are omitted") {
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

        result.bookingList shouldBe emptyList<TicketDetailResult>()
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

    test("rejects null blank and one-character search words before dependencies") {
        listOf(null, "", "a").forEach { searchWord ->
            shouldThrow<FrontofficeApplicationException> {
                service().searchAllTicketsByConditions(1L, 100L, TicketListQuery(searchWord = searchWord))
            }
        }

        Mockito.verifyNoInteractions(makerTicketReader, performanceRepository, memberRepository)
    }

    test("rejects deleted booking status for list and search") {
        val listException = shouldThrow<FrontofficeApplicationException> {
            service().findAllTicketsByConditions(
                1L,
                100L,
                TicketListQuery(bookingStatuses = listOf("BOOKING_DELETED")),
            )
        }
        val searchException = shouldThrow<FrontofficeApplicationException> {
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

    test("authorizes query through authoritative PerformanceRepository") {
        Mockito.`when`(memberRepository.findById(1L)).thenReturn(member(userId = 10L))
        Mockito.`when`(performanceRepository.findById(100L)).thenReturn(performance(userId = 11L))

        shouldThrow<FrontofficeApplicationException> {
            service().findAllTicketsByConditions(1L, 100L, TicketListQuery())
        }

        Mockito.verify(performanceRepository).findById(100L)
        Mockito.verifyNoInteractions(makerTicketReader)
    }

    }

    private fun service() = TicketQueryService(
        makerTicketReader = makerTicketReader,
        performanceRepository = performanceRepository,
        memberRepository = memberRepository,
    )

    private fun stubOwner() {
        Mockito.`when`(memberRepository.findById(1L)).thenReturn(member())
        Mockito.`when`(performanceRepository.findById(100L)).thenReturn(performance())
    }

    private fun assertTicketValidationFailure(exception: FrontofficeApplicationException) {
        exception.errorCode.type shouldBe FrontofficeApplicationErrorType.INVALID_INPUT
        exception.errorCode.message shouldBe "삭제된 예매자를 조회할 수 없습니다."
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
