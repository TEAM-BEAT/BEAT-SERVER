package com.beat.infrastructure.persistence.performance.mapper

import com.beat.domain.exception.DomainException
import com.beat.domain.performance.model.Cast
import com.beat.domain.performance.model.Performance
import com.beat.domain.performance.model.PerformanceImage
import com.beat.domain.performance.model.Staff
import com.beat.domain.performance.vo.PaymentAccount
import com.beat.domain.performance.vo.PerformancePeriod
import com.beat.domain.performance.vo.RunningTime
import com.beat.domain.performance.vo.TicketPrice
import com.beat.infrastructure.persistence.exception.PersistenceMappingException
import com.beat.infrastructure.persistence.performance.entity.PaymentAccountJpaValue
import com.beat.infrastructure.persistence.performance.entity.PerformanceJpaEntity
import com.beat.infrastructure.persistence.performance.entity.PerformancePeriodJpaValue
import org.springframework.stereotype.Component

@Component
internal class PerformancePersistenceMapper {
    fun toDomain(entity: PerformanceJpaEntity): Performance = toDomain(entity, emptyList(), emptyList(), emptyList())

    fun toDomain(
        entity: PerformanceJpaEntity,
        casts: List<Cast>,
        staffs: List<Staff>,
        images: List<PerformanceImage>,
    ): Performance = try {
        Performance.rehydrate(
            entity.id,
            entity.performanceTitle,
            entity.genre,
            RunningTime.of(entity.runningTime),
            entity.performanceDescription,
            entity.performanceAttentionNote,
            toDomain(entity.paymentAccount),
            entity.posterImage,
            entity.performanceTeamName,
            entity.performanceVenue,
            entity.roadAddressName,
            entity.placeDetailAddress,
            entity.latitude,
            entity.longitude,
            entity.performanceContact,
            toDomainPeriod(entity),
            TicketPrice.of(entity.ticketPrice),
            entity.totalScheduleCount,
            entity.userId,
            casts,
            staffs,
            images,
        )
    } catch (exception: DomainException) {
        throw PersistenceMappingException.invalidStoredState("Performance", entity.id, exception)
    }

    fun toEntity(domain: Performance): PerformanceJpaEntity = PerformanceJpaEntity.rehydrate(
        domain.id,
        domain.performanceTitle,
        domain.genre,
        domain.runningTimeValue.minutes,
        domain.performanceDescription,
        domain.performanceAttentionNote,
        toEntity(domain.paymentAccount),
        domain.posterImage,
        domain.performanceTeamName,
        domain.performanceVenue,
        domain.roadAddressName,
        domain.placeDetailAddress,
        domain.latitude,
        domain.longitude,
        domain.performanceContact,
        toEntity(domain.performancePeriodValue),
        domain.ticketPriceValue.amount,
        domain.totalScheduleCount,
        domain.userId,
    )

    private fun toDomain(value: PaymentAccountJpaValue?): PaymentAccount? = value?.let {
        PaymentAccount.fromNullable(it.bankName, it.accountNumber, it.accountHolder)
    }

    private fun toEntity(value: PaymentAccount?): PaymentAccountJpaValue? = value?.let {
        PaymentAccountJpaValue(it.bankName, it.accountNumber, it.accountHolder)
    }

    private fun toDomainPeriod(entity: PerformanceJpaEntity): PerformancePeriod =
        PerformancePeriod.of(entity.performancePeriodValue.startDate, entity.performancePeriodValue.endDate)

    private fun toEntity(value: PerformancePeriod): PerformancePeriodJpaValue =
        PerformancePeriodJpaValue(value.startDate, value.endDate)
}
