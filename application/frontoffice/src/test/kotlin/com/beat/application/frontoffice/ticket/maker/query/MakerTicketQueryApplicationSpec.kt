package com.beat.application.frontoffice.ticket.maker.query

import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorType
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.fixture.frontofficeMemberFixture
import com.beat.application.frontoffice.fixture.frontofficePerformanceFixture
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.performance.repository.PerformanceRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.Called
import io.mockk.verify
import java.time.LocalDateTime

class MakerTicketQueryApplicationSpec : FunSpec() {
    init {
    test("명시적인 회차와 예매 상태 필터를 매핑하고 null이 아닌 결과를 반환한다") {
        val (makerTicketReader, performanceRepository, memberRepository) = makerTicketDependencies()
        stubOwner(memberRepository, performanceRepository)
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

        val result = service(makerTicketReader, performanceRepository, memberRepository).findAllTicketsByConditions(
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
        val (makerTicketReader, performanceRepository, memberRepository) = makerTicketDependencies()
        stubOwner(memberRepository, performanceRepository)
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

        val result = service(makerTicketReader, performanceRepository, memberRepository).searchAllTicketsByConditions(
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
        val (makerTicketReader, performanceRepository, memberRepository) = makerTicketDependencies()

        listOf(null, "", "a").forEach { searchWord ->
            shouldThrow<FrontofficeApplicationException> {
                service(makerTicketReader, performanceRepository, memberRepository)
                    .searchAllTicketsByConditions(1L, 100L, TicketListQuery(searchWord = searchWord))
            }
        }

        verify {
            listOf(makerTicketReader, performanceRepository, memberRepository) wasNot Called
        }
    }

    test("목록과 검색에서 삭제 상태 필터를 거부한다") {
        val (makerTicketReader, performanceRepository, memberRepository) = makerTicketDependencies()

        val listException = shouldThrow<FrontofficeApplicationException> {
            service(makerTicketReader, performanceRepository, memberRepository).findAllTicketsByConditions(
                1L,
                100L,
                TicketListQuery(bookingStatuses = listOf("BOOKING_DELETED")),
            )
        }
        val searchException = shouldThrow<FrontofficeApplicationException> {
            service(makerTicketReader, performanceRepository, memberRepository).searchAllTicketsByConditions(
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
        val (makerTicketReader, performanceRepository, memberRepository) = makerTicketDependencies()
        every { memberRepository.findById(1L) } returns member(userId = 10L)
        every { performanceRepository.findById(100L) } returns performance(userId = 11L)

        shouldThrow<FrontofficeApplicationException> {
            service(makerTicketReader, performanceRepository, memberRepository)
                .findAllTicketsByConditions(1L, 100L, TicketListQuery())
        }

        verify { performanceRepository.findById(100L) }
        verify { makerTicketReader wasNot Called }
    }

    }

    private fun service(
        makerTicketReader: MakerTicketReader,
        performanceRepository: PerformanceRepository,
        memberRepository: MemberRepository,
    ) = TicketQueryService(
        makerTicketReader = makerTicketReader,
        performanceRepository = performanceRepository,
        memberRepository = memberRepository,
    )

    private fun stubOwner(
        memberRepository: MemberRepository,
        performanceRepository: PerformanceRepository,
    ) {
        every { memberRepository.findById(1L) } returns member()
        every { performanceRepository.findById(100L) } returns performance()
    }

    private fun assertTicketValidationFailure(exception: FrontofficeApplicationException) {
        exception.errorCode.type shouldBe FrontofficeApplicationErrorType.INVALID_INPUT
        exception.errorCode.message shouldBe "삭제된 예매자를 조회할 수 없습니다."
    }

    private fun member(userId: Long = 10L) = frontofficeMemberFixture(
        id = 1L,
        nickname = "maker",
        email = null,
        userId = userId,
        socialId = 123L,
    )

    private fun performance(userId: Long = 10L) = frontofficePerformanceFixture(
        id = 100L,
        userId = userId,
        totalScheduleCount = 2,
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

private data class MakerTicketDependencies(
    val makerTicketReader: MakerTicketReader = mockk(relaxed = true),
    val performanceRepository: PerformanceRepository = mockk(relaxed = true),
    val memberRepository: MemberRepository = mockk(relaxed = true),
)

private fun makerTicketDependencies() = MakerTicketDependencies()
