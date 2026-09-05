package com.beat.apps.api.booking.experiment

import com.beat.application.frontoffice.booking.booker.experiment.StockContentionExperimentEnabled
import com.beat.application.frontoffice.booking.booker.experiment.StockContentionExperimentProperties
import com.beat.application.frontoffice.booking.booker.experiment.StockContentionExperimentService
import com.beat.application.frontoffice.booking.booker.experiment.StockContentionStrategyRegistry
import com.beat.apps.api.booking.api.experiment.StockContentionExperimentController
import com.beat.infrastructure.booking.booker.experiment.AtomicStockContentionReservationStrategy
import com.beat.infrastructure.booking.booker.experiment.JdbcStockContentionScheduleStore
import com.beat.infrastructure.booking.booker.experiment.OptimisticStockContentionReservationStrategy
import com.beat.infrastructure.booking.booker.experiment.PessimisticStockContentionReservationStrategy
import com.beat.infrastructure.booking.booker.experiment.RedisStockContentionReservationStrategy
import com.beat.infrastructure.booking.booker.experiment.StockContentionExperimentInfraConfig
import com.beat.infrastructure.booking.booker.experiment.StockContentionRedisReservationLock
import com.beat.infrastructure.booking.booker.experiment.StockContentionScheduleVersionPrerequisite
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile

class StockContentionExperimentBeanGateSpec : FunSpec() {
    init {
        test("실험 controller와 모든 전략은 dev 전용 profile과 명시적 enabled flag를 요구한다") {
            val gatedTypes =
                listOf(
                    StockContentionExperimentController::class.java,
                    StockContentionExperimentProperties::class.java,
                    StockContentionExperimentService::class.java,
                    StockContentionStrategyRegistry::class.java,
                    JdbcStockContentionScheduleStore::class.java,
                    StockContentionScheduleVersionPrerequisite::class.java,
                    PessimisticStockContentionReservationStrategy::class.java,
                    OptimisticStockContentionReservationStrategy::class.java,
                    RedisStockContentionReservationStrategy::class.java,
                    AtomicStockContentionReservationStrategy::class.java,
                    StockContentionRedisReservationLock::class.java,
                    StockContentionExperimentInfraConfig::class.java,
                )

            gatedTypes.forEach { type ->
                type.getAnnotation(Profile::class.java).value.toList() shouldBe
                    listOf("dev & !prod")
                if (type == StockContentionExperimentController::class.java) {
                    type.getAnnotation(ConditionalOnProperty::class.java).havingValue shouldBe
                        "true"
                    type.getAnnotation(ConditionalOnProperty::class.java).name.toList() shouldBe
                        listOf("enabled")
                } else {
                    type
                        .getAnnotation(StockContentionExperimentEnabled::class.java)
                        .shouldNotBeNull()
                }
            }
        }
    }
}
