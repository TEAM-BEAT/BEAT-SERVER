package com.beat.domain.common

import com.beat.domain.booking.fixture.bookingFixture
import com.beat.domain.booking.vo.RefundAccount
import com.beat.domain.member.model.Member
import com.beat.domain.member.model.SocialType
import com.beat.domain.member.vo.SocialIdentity
import com.beat.domain.performance.model.Genre
import com.beat.domain.performance.model.Performance
import com.beat.domain.performance.vo.PaymentAccount
import com.beat.domain.performance.vo.PerformancePeriod
import com.beat.domain.performance.vo.RunningTime
import com.beat.domain.performance.vo.TicketPrice
import com.beat.domain.sharedkernel.vo.BankName
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class SensitiveDomainToStringTest : FunSpec() {
    init {
        isolationMode = IsolationMode.SingleInstance

        test("민감한 값은 Domain toString에 노출되지 않는다") {
            val accountNumber = "123-456-789"
            val accountHolder = "sensitive-holder"
            val email = "sensitive@example.com"
            val socialId = 987654321L
            val socialIdentity = SocialIdentity.of(SocialType.KAKAO, socialId)
            val booking =
                bookingFixture(
                    bookerName = "private-booker",
                    bookerPhoneNumber = "010-9876-5432",
                    password = "secret-password",
                )
            val values =
                listOf(
                    PaymentAccount.of(BankName.KAKAOBANK, accountNumber, accountHolder),
                    RefundAccount.of(BankName.KAKAOBANK, accountNumber, accountHolder),
                    socialIdentity,
                    Member.rehydrate(1, "nickname", email, null, 1, socialIdentity),
                    Performance.rehydrate(
                        id = 1,
                        performanceTitle = "title",
                        genre = Genre.PLAY,
                        runningTime = RunningTime.of(90),
                        performanceDescription = "description",
                        performanceAttentionNote = "attention",
                        paymentAccount =
                            PaymentAccount.of(BankName.KAKAOBANK, accountNumber, accountHolder),
                        posterImage = "poster",
                        performanceTeamName = "team",
                        performanceVenue = "venue",
                        roadAddressName = "road",
                        placeDetailAddress = "detail",
                        latitude = "latitude",
                        longitude = "longitude",
                        performanceContact = "contact",
                        performancePeriod =
                            PerformancePeriod.of(
                                LocalDate.of(2026, 7, 17),
                                LocalDate.of(2026, 7, 17),
                            ),
                        ticketPrice = TicketPrice.of(10_000),
                        totalScheduleCount = 1,
                        userId = 1,
                    ),
                )

            values.forEach { value ->
                val rendered = value.toString()
                rendered.isNotBlank() shouldBe true
                rendered.contains(accountNumber) shouldBe false
                rendered.contains(accountHolder) shouldBe false
                rendered.contains(email) shouldBe false
                rendered.contains(socialId.toString()) shouldBe false
            }

            val bookingRendered = booking.toString()
            bookingRendered.contains("private-booker") shouldBe false
            bookingRendered.contains("010-9876-5432") shouldBe false
            bookingRendered.contains("990101") shouldBe false
            bookingRendered.contains("secret-password") shouldBe false
        }
    }
}
