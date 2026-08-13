package com.beat.domain.common

import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.model.BookingStatus
import com.beat.domain.performance.model.Cast
import com.beat.domain.member.model.Member
import com.beat.domain.member.model.SocialType
import com.beat.domain.member.vo.SocialIdentity
import com.beat.domain.performance.model.Genre
import com.beat.domain.performance.model.Performance
import com.beat.domain.performance.vo.PerformancePeriod
import com.beat.domain.performance.vo.RunningTime
import com.beat.domain.performance.vo.TicketPrice
import com.beat.domain.performance.model.PerformanceImage
import com.beat.domain.promotion.model.CarouselNumber
import com.beat.domain.promotion.model.Promotion
import com.beat.domain.schedule.model.Schedule
import com.beat.domain.schedule.model.ScheduleNumber
import com.beat.domain.performance.model.Staff
import com.beat.domain.user.model.Role
import com.beat.domain.user.model.Users
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class DomainEntityEqualityBehaviorTest {
    @Test
    fun allEntitiesUseIdentityEqualityAndInstanceEqualityWhileTransient() {
        entityCases().forEach { case ->
            assertEquals(case.persisted, case.sameIdentityWithDifferentState, "${case.name} same identity")
            assertEquals(
                case.persisted.hashCode(),
                case.sameIdentityWithDifferentState.hashCode(),
                "${case.name} hash contract",
            )
            assertNotEquals(case.persisted, case.differentIdentity, "${case.name} different identity")
            assertNotEquals(case.transient, case.otherTransient, "${case.name} transient identity")
            assertEquals(case.transient, case.transient, "${case.name} same transient instance")
        }
    }

    private fun entityCases(): List<EntityCase> = listOf(
        EntityCase("Booking", booking(1, "first"), booking(1, "changed"), booking(2, "first"), newBooking(), newBooking()),
        EntityCase("Cast", cast(1, "first"), cast(1, "changed"), cast(2, "first"), newCast(), newCast()),
        EntityCase("Member", member(1, "first"), member(1, "changed"), member(2, "first"), newMember(), newMember()),
        EntityCase(
            "Performance",
            performance(1, "first"),
            performance(1, "changed"),
            performance(2, "first"),
            newPerformance(),
            newPerformance(),
        ),
        EntityCase(
            "PerformanceImage",
            PerformanceImage.rehydrate(1, "first"),
            PerformanceImage.rehydrate(1, "changed"),
            PerformanceImage.rehydrate(2, "first"),
            PerformanceImage.create("image"),
            PerformanceImage.create("image"),
        ),
        EntityCase(
            "Promotion",
            promotion(1, "first"),
            promotion(1, "changed"),
            promotion(2, "first"),
            newPromotion(),
            newPromotion(),
        ),
        EntityCase("Schedule", schedule(1, 10), schedule(1, 20), schedule(2, 10), newSchedule(), newSchedule()),
        EntityCase("Staff", staff(1, "first"), staff(1, "changed"), staff(2, "first"), newStaff(), newStaff()),
        EntityCase(
            "Users",
            Users.rehydrate(1, Role.USER),
            Users.rehydrate(1, Role.ADMIN),
            Users.rehydrate(2, Role.USER),
            Users.create(),
            Users.create(),
        ),
    )

    private fun booking(id: Long, name: String): Booking = Booking.rehydrate(
        id, 1, name, "010-0000-0000", BookingStatus.CHECKING_PAYMENT, BASE_TIME, null, null, null, null, 1, 1,
    )

    private fun newBooking(): Booking = Booking.create(
        1, "name", "010-0000-0000", null, null, 1, 1, BASE_TIME,
    )

    private fun cast(id: Long, name: String): Cast = Cast.rehydrate(id, name, "role", "photo")

    private fun newCast(): Cast = Cast.create("name", "role", "photo")

    private fun member(id: Long, nickname: String): Member = Member.rehydrate(
        id, nickname, null, null, 1, SOCIAL_IDENTITY,
    )

    private fun newMember(): Member = Member.create("nickname", null, 1, SOCIAL_IDENTITY)

    private fun performance(id: Long, title: String): Performance = Performance.rehydrate(
        id = id,
        performanceTitle = title,
        genre = Genre.PLAY,
        runningTime = RunningTime.of(90),
        performanceDescription = "description",
        performanceAttentionNote = "attention",
        paymentAccount = null,
        posterImage = "poster",
        performanceTeamName = "team",
        performanceVenue = "venue",
        roadAddressName = "road",
        placeDetailAddress = "detail",
        latitude = "latitude",
        longitude = "longitude",
        performanceContact = "contact",
        performancePeriod = PERFORMANCE_PERIOD,
        ticketPrice = TicketPrice.of(10_000),
        totalScheduleCount = 1,
        userId = 1,
    )

    private fun newPerformance(): Performance = Performance.create(
        performanceTitle = "title",
        genre = Genre.PLAY,
        runningTime = RunningTime.of(90),
        performanceDescription = "description",
        performanceAttentionNote = "attention",
        paymentAccount = null,
        posterImage = "poster",
        performanceTeamName = "team",
        performanceVenue = "venue",
        roadAddressName = "road",
        placeDetailAddress = "detail",
        latitude = "latitude",
        longitude = "longitude",
        performanceContact = "contact",
        performancePeriod = PERFORMANCE_PERIOD,
        ticketPrice = TicketPrice.of(10_000),
        totalScheduleCount = 1,
        userId = 1,
    )

    private fun promotion(id: Long, photo: String): Promotion =
        Promotion.rehydrate(id, photo, null, "url", true, CarouselNumber.ONE)

    private fun newPromotion(): Promotion = Promotion.create("photo", null, "url", true, CarouselNumber.ONE)

    private fun schedule(id: Long, ticketCount: Int): Schedule = Schedule.rehydrate(
        id, BASE_TIME.plusDays(1), BASE_TIME.plusDays(2), ticketCount, 0, ScheduleNumber.FIRST, 1,
    )

    private fun newSchedule(): Schedule = Schedule.create(
        BASE_TIME.plusDays(1), BASE_TIME.plusDays(2), 10, ScheduleNumber.FIRST, 1,
    )

    private fun staff(id: Long, name: String): Staff = Staff.rehydrate(id, name, "role", "photo")

    private fun newStaff(): Staff = Staff.create("name", "role", "photo")

    private data class EntityCase(
        val name: String,
        val persisted: Any,
        val sameIdentityWithDifferentState: Any,
        val differentIdentity: Any,
        val transient: Any,
        val otherTransient: Any,
    )

    private companion object {
        val BASE_TIME: LocalDateTime = LocalDateTime.of(2026, 7, 17, 12, 0)
        val PERFORMANCE_PERIOD: PerformancePeriod =
            PerformancePeriod.of(LocalDate.of(2026, 7, 17), LocalDate.of(2026, 7, 17))
        val SOCIAL_IDENTITY: SocialIdentity = SocialIdentity.of(SocialType.KAKAO, 1)
    }
}
