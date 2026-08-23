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
import io.mockk.every
import io.mockk.mockk
import io.mockk.Called
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalDateTime

class MakerTicketQueryApplicationSpec : FunSpec() {

    private lateinit var makerTicketReader: MakerTicketReader

    private lateinit var performanceRepository: PerformanceRepository

    private lateinit var memberRepository: MemberRepository

    init {
        beforeTest {
            makerTicketReader = mockk(relaxed = true)
            performanceRepository = mockk(relaxed = true)
            memberRepository = mockk(relaxed = true)
        }

    test("명시적인 회차와 예매 상태 필터를 매핑하고 null이 아닌 결과를 반환한다") {
        stubOwner()
        val schedules = listOf(schedule(200L, "FIRST", totalTickets = 100, soldTickets = 99))
        val ticket = ticket()
        every { makerTicketReader.findSchedules(100L) } returns schedules
        every {
            makerTicketReader.findTickets(
                100L,
                listOf(MakerTicketScheduleNumber.FIRST),
                listOf(MakerTicketBookingStatus.CHECKING_PAYMENT),
            )
        } returns listOf(ticket)

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
        verify {
            makerTicketReader.findTickets(
                100L,
                listOf(MakerTicketScheduleNumber.FIRST),
                listOf(MakerTicketBookingStatus.CHECKING_PAYMENT),
            )
        }
    }

    test("필터가 없으면 전체 회차와 활성 maker 상태로 검색한다") {
        stubOwner()
        every { makerTicketReader.findSchedules(100L) } returns
            listOf(
                schedule(200L, "FIRST", totalTickets = 100, soldTickets = 1),
                schedule(201L, "SECOND", totalTickets = 100, soldTickets = 2),
            )
        every {
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
            )
        } returns emptyList()

        val result = service().searchAllTicketsByConditions(
            1L,
            100L,
            TicketListQuery(searchWord = "booker"),
        )

        result.bookingList shouldBe emptyList<TicketDetailResult>()
        verify {
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
            )
        }
    }

    test("의존성 호출 전에 null·빈 문자열·한 글자 검색어를 거부한다") {
        listOf(null, "", "a").forEach { searchWord ->
            shouldThrow<FrontofficeApplicationException> {
                service().searchAllTicketsByConditions(1L, 100L, TicketListQuery(searchWord = searchWord))
            }
        }

        verify {
            listOf(makerTicketReader, performanceRepository, memberRepository) wasNot Called
        }
    }

    test("목록과 검색에서 삭제 상태 필터를 거부한다") {
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
        verify {
            listOf(makerTicketReader, performanceRepository, memberRepository) wasNot Called
        }
    }

    test("authoritative PerformanceRepository로 조회 권한을 검증한다") {
        every { memberRepository.findById(1L) } returns member(userId = 10L)
        every { performanceRepository.findById(100L) } returns performance(userId = 11L)

        shouldThrow<FrontofficeApplicationException> {
            service().findAllTicketsByConditions(1L, 100L, TicketListQuery())
        }

        verify { performanceRepository.findById(100L) }
        verify { makerTicketReader wasNot Called }
    }

    }

    private fun service() = TicketQueryService(
        makerTicketReader = makerTicketReader,
        performanceRepository = performanceRepository,
        memberRepository = memberRepository,
    )

    private fun stubOwner() {
        every { memberRepository.findById(1L) } returns member()
        every { performanceRepository.findById(100L) } returns performance()
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
