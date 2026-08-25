package com.beat.infrastructure.persistence.performance.mapper

import com.beat.domain.performance.model.Genre
import com.beat.domain.performance.model.Performance
import com.beat.domain.performance.vo.PaymentAccount
import com.beat.domain.performance.vo.PerformancePeriod
import com.beat.domain.performance.vo.RunningTime
import com.beat.domain.performance.vo.TicketPrice
import com.beat.domain.sharedkernel.vo.BankName
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class PerformancePersistenceMapperTest :
    FunSpec({
        val mapper = PerformancePersistenceMapper()

        test("레거시 컬럼을 변경하지 않고 value object를 왕복한다") {
            val domain = performance(PaymentAccount.of(BankName.KAKAOBANK, "123", "holder"))

            val entity = mapper.toEntity(domain)
            val roundTrip = mapper.toDomain(entity)

            entity.paymentAccount!!.bankName shouldBe BankName.KAKAOBANK
            entity.performancePeriodValue.startDate shouldBe LocalDate.of(2026, 7, 16)
            roundTrip.paymentAccount shouldBe domain.paymentAccount
            roundTrip.performancePeriodValue shouldBe domain.performancePeriodValue
            roundTrip.runningTimeValue shouldBe domain.runningTimeValue
            roundTrip.ticketPriceValue shouldBe domain.ticketPriceValue
        }

        test("모두 null인 payment account는 null로 유지된다") {
            val entity = mapper.toEntity(performance(null))

            entity.paymentAccount shouldBe null
            mapper.toDomain(entity).paymentAccount shouldBe null
        }

        test("CONTRACT 이후 period 컬럼은 항상 not null로 저장된다") {
            val entity = mapper.toEntity(performance(null))

            entity.performancePeriodValue.startDate shouldBe LocalDate.of(2026, 7, 16)
            entity.performancePeriodValue.endDate shouldBe LocalDate.of(2026, 7, 18)
        }
    })

private fun performance(paymentAccount: PaymentAccount?): Performance =
    Performance.rehydrate(
        1L,
        "title",
        Genre.BAND,
        RunningTime.of(90),
        "description",
        "attention",
        paymentAccount,
        "poster",
        "team",
        "venue",
        "road",
        "detail",
        "37.1",
        "127.1",
        "010-0000-0000",
        PerformancePeriod.of(LocalDate.of(2026, 7, 16), LocalDate.of(2026, 7, 18)),
        TicketPrice.of(20_000),
        3,
        7L,
    )
