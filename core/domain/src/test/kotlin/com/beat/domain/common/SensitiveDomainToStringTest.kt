package com.beat.domain.common

import com.beat.domain.booking.vo.RefundAccount
import com.beat.domain.member.model.Member
import com.beat.domain.member.model.SocialType
import com.beat.domain.member.vo.SocialIdentity
import com.beat.domain.sharedkernel.vo.BankName
import com.beat.domain.performance.model.Genre
import com.beat.domain.performance.model.Performance
import com.beat.domain.performance.vo.PaymentAccount
import com.beat.domain.performance.vo.PerformancePeriod
import com.beat.domain.performance.vo.RunningTime
import com.beat.domain.performance.vo.TicketPrice
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SensitiveDomainToStringTest {
    @Test
    fun sensitiveValuesAreNotExposedByToString() {
        val accountNumber = "123-456-789"
        val accountHolder = "sensitive-holder"
        val email = "sensitive@example.com"
        val socialId = 987654321L
        val socialIdentity = SocialIdentity.of(SocialType.KAKAO, socialId)
        val values = listOf(
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
                paymentAccount = PaymentAccount.of(BankName.KAKAOBANK, accountNumber, accountHolder),
                posterImage = "poster",
                performanceTeamName = "team",
                performanceVenue = "venue",
                roadAddressName = "road",
                placeDetailAddress = "detail",
                latitude = "latitude",
                longitude = "longitude",
                performanceContact = "contact",
                performancePeriod = PerformancePeriod.of(
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
            assertTrue(rendered.isNotBlank())
            assertFalse(rendered.contains(accountNumber))
            assertFalse(rendered.contains(accountHolder))
            assertFalse(rendered.contains(email))
            assertFalse(rendered.contains(socialId.toString()))
        }
    }
}
