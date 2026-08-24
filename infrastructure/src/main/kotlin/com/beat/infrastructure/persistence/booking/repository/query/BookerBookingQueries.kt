package com.beat.infrastructure.persistence.booking.repository.query

import com.beat.application.frontoffice.booking.booker.BookingHistoryPerformanceSnapshot
import com.beat.application.frontoffice.booking.booker.BookingHistoryReadPort
import com.beat.application.frontoffice.booking.booker.BookingHistoryScheduleSnapshot
import com.beat.application.frontoffice.booking.booker.BookingHistorySnapshot
import com.beat.domain.booking.model.BookingStatus
import com.beat.domain.schedule.model.ScheduleNumber
import com.beat.domain.sharedkernel.vo.BankName
import com.beat.infrastructure.persistence.booking.entity.BookingJpaEntity
import com.beat.infrastructure.persistence.performance.entity.PaymentAccountJpaValue
import com.beat.infrastructure.persistence.performance.entity.PerformanceJpaEntity
import com.beat.infrastructure.persistence.schedule.entity.ScheduleJpaEntity
import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.hibernate.extension.createQuery
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
internal class BookerBookingQueries(
    private val entityManager: EntityManager,
    private val jpqlRenderContext: JpqlRenderContext,
) : BookingHistoryReadPort {
    override fun findByUserId(userId: Long): List<BookingHistorySnapshot> {
        val bookings = findBookings(userId)
        if (bookings.isEmpty()) return emptyList()

        val schedules = findSchedules(bookings.map(BookingProjection::scheduleId).distinct())
            .associateBy(ScheduleProjection::scheduleId)
        val performances = findPerformances(schedules.values.map(ScheduleProjection::performanceId).distinct())
            .associateBy(PerformanceProjection::performanceId)
        return bookings.map { booking ->
            val schedule = schedules[booking.scheduleId]
            BookingHistorySnapshot(
                userId = booking.userId,
                bookingId = checkNotNull(booking.bookingId),
                purchaseTicketCount = booking.purchaseTicketCount,
                bookerName = booking.bookerName,
                bookingStatus = booking.bookingStatus.name,
                createdAt = booking.createdAt,
                totalPaymentAmount = booking.totalPaymentAmount,
                schedule = schedule?.toSnapshot(),
                performance = schedule?.let { performances[it.performanceId]?.toSnapshot() },
            )
        }
    }

    private fun findBookings(userId: Long): List<BookingProjection> {
        val query = jpql {
            selectNew<BookingProjection>(
                path(BookingJpaEntity::id),
                path(BookingJpaEntity::userId),
                path(BookingJpaEntity::scheduleId),
                path(BookingJpaEntity::purchaseTicketCount),
                path(BookingJpaEntity::bookerName),
                path(BookingJpaEntity::bookingStatus),
                path(BookingJpaEntity::createdAt),
                path(BookingJpaEntity::totalPaymentAmount),
            ).from(entity(BookingJpaEntity::class))
                .where(path(BookingJpaEntity::userId).eq(userId))
        }
        return entityManager.createQuery(query, jpqlRenderContext).resultList
    }

    private fun findSchedules(scheduleIds: Collection<Long>): List<ScheduleProjection> {
        val query = jpql {
            selectNew<ScheduleProjection>(
                path(ScheduleJpaEntity::id),
                path(ScheduleJpaEntity::performanceId),
                path(ScheduleJpaEntity::performanceDate),
                path(ScheduleJpaEntity::scheduleNumber),
            ).from(entity(ScheduleJpaEntity::class))
                .where(path(ScheduleJpaEntity::id).`in`(scheduleIds))
        }
        return entityManager.createQuery(query, jpqlRenderContext).resultList
    }

    private fun findPerformances(performanceIds: Collection<Long>): List<PerformanceProjection> {
        if (performanceIds.isEmpty()) return emptyList()
        val query = jpql {
            val paymentAccount = path(PerformanceJpaEntity::paymentAccount)
            selectNew<PerformanceProjection>(
                path(PerformanceJpaEntity::id),
                path(PerformanceJpaEntity::performanceTitle),
                path(PerformanceJpaEntity::performanceVenue),
                path(PerformanceJpaEntity::performanceContact),
                paymentAccount(PaymentAccountJpaValue::bankName),
                paymentAccount(PaymentAccountJpaValue::accountNumber),
                paymentAccount(PaymentAccountJpaValue::accountHolder),
                path(PerformanceJpaEntity::posterImage),
                path(PerformanceJpaEntity::ticketPrice),
            ).from(entity(PerformanceJpaEntity::class))
                .where(path(PerformanceJpaEntity::id).`in`(performanceIds))
        }
        return entityManager.createQuery(query, jpqlRenderContext).resultList
    }

    private fun ScheduleProjection.toSnapshot(): BookingHistoryScheduleSnapshot =
        BookingHistoryScheduleSnapshot(
            scheduleId = checkNotNull(scheduleId),
            performanceId = performanceId,
            performanceDate = performanceDate,
            scheduleNumber = scheduleNumber.name,
        )

    private fun PerformanceProjection.toSnapshot(): BookingHistoryPerformanceSnapshot =
        BookingHistoryPerformanceSnapshot(
            performanceId = checkNotNull(performanceId),
            performanceTitle = performanceTitle,
            performanceVenue = performanceVenue,
            performanceContact = performanceContact,
            bankName = bankName?.name,
            accountNumber = accountNumber,
            accountHolder = accountHolder,
            posterImage = posterImage,
            ticketPrice = ticketPrice,
        )

    private data class BookingProjection(
        val bookingId: Long?,
        val userId: Long,
        val scheduleId: Long,
        val purchaseTicketCount: Int,
        val bookerName: String,
        val bookingStatus: BookingStatus,
        val createdAt: LocalDateTime,
        val totalPaymentAmount: Int?,
    )

    private data class ScheduleProjection(
        val scheduleId: Long?,
        val performanceId: Long,
        val performanceDate: LocalDateTime,
        val scheduleNumber: ScheduleNumber,
    )

    private data class PerformanceProjection(
        val performanceId: Long?,
        val performanceTitle: String,
        val performanceVenue: String,
        val performanceContact: String,
        val bankName: BankName?,
        val accountNumber: String?,
        val accountHolder: String?,
        val posterImage: String,
        val ticketPrice: Int,
    )
}
