package com.beat.infra.persistence.performance.repository.query

import com.beat.application.frontoffice.performance.maker.query.PerformanceEditFormReader
import com.beat.application.frontoffice.performance.maker.query.PerformanceEditCastReadModel
import com.beat.application.frontoffice.performance.maker.query.PerformanceEditFormReadModel
import com.beat.application.frontoffice.performance.maker.query.PerformanceEditImageReadModel
import com.beat.application.frontoffice.performance.maker.query.PerformanceEditScheduleReadModel
import com.beat.application.frontoffice.performance.maker.query.PerformanceEditStaffReadModel
import com.beat.domain.booking.model.BookingStatus
import com.beat.domain.performance.model.Genre
import com.beat.domain.schedule.model.ScheduleNumber
import com.beat.domain.sharedkernel.vo.BankName
import com.beat.infra.persistence.booking.entity.BookingJpaEntity
import com.beat.infra.persistence.cast.entity.CastJpaEntity
import com.beat.infra.persistence.performance.entity.PaymentAccountJpaValue
import com.beat.infra.persistence.performance.entity.PerformanceJpaEntity
import com.beat.infra.persistence.performance.entity.PerformancePeriodJpaValue
import com.beat.infra.persistence.performanceimage.entity.PerformanceImageJpaEntity
import com.beat.infra.persistence.schedule.entity.ScheduleJpaEntity
import com.beat.infra.persistence.staff.entity.StaffJpaEntity
import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.hibernate.extension.createQuery
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Repository
internal class PerformanceEditFormQueries(
    private val entityManager: EntityManager,
    private val jpqlRenderContext: JpqlRenderContext,
) : PerformanceEditFormReader {

    override fun findByPerformanceId(performanceId: Long): PerformanceEditFormReadModel? {
        val header = entityManager.createQuery(headerQuery(performanceId), jpqlRenderContext)
            .resultList
            .firstOrNull()
            ?: return null
        val schedules = findSchedules(performanceId)

        return PerformanceEditFormReadModel(
                performanceId = checkNotNull(header.performanceId),
                userId = header.userId,
                performanceTitle = header.performanceTitle,
                genre = header.genre.name,
                runningTime = header.runningTime,
                performanceDescription = header.performanceDescription,
                performanceAttentionNote = header.performanceAttentionNote,
                bankName = header.bankName?.name,
                accountNumber = header.accountNumber,
                accountHolder = header.accountHolder,
                posterImage = header.posterImage,
                performanceTeamName = header.performanceTeamName,
                performanceVenue = header.performanceVenue,
                roadAddressName = header.roadAddressName,
                placeDetailAddress = header.placeDetailAddress,
                latitude = header.latitude,
                longitude = header.longitude,
                performanceContact = header.performanceContact,
                performancePeriod = formatPeriod(header),
                ticketPrice = header.ticketPrice,
                totalScheduleCount = header.totalScheduleCount,
                hasActiveBooking = schedules.isNotEmpty() && hasActiveBooking(performanceId),
                schedules = schedules,
                casts = findCasts(performanceId),
                staffs = findStaffs(performanceId),
                images = findImages(performanceId),
            )
    }

    private fun headerQuery(performanceId: Long) = jpql {
        val paymentAccount = path(PerformanceJpaEntity::paymentAccount)
        val performancePeriod = path(PerformanceJpaEntity::performancePeriodValue)
        selectNew<PerformanceEditHeaderProjection>(
            path(PerformanceJpaEntity::id),
            path(PerformanceJpaEntity::userId),
            path(PerformanceJpaEntity::performanceTitle),
            path(PerformanceJpaEntity::genre),
            path(PerformanceJpaEntity::runningTime),
            path(PerformanceJpaEntity::performanceDescription),
            path(PerformanceJpaEntity::performanceAttentionNote),
            paymentAccount(PaymentAccountJpaValue::bankName),
            paymentAccount(PaymentAccountJpaValue::accountNumber),
            paymentAccount(PaymentAccountJpaValue::accountHolder),
            path(PerformanceJpaEntity::posterImage),
            path(PerformanceJpaEntity::performanceTeamName),
            path(PerformanceJpaEntity::performanceVenue),
            path(PerformanceJpaEntity::roadAddressName),
            path(PerformanceJpaEntity::placeDetailAddress),
            path(PerformanceJpaEntity::latitude),
            path(PerformanceJpaEntity::longitude),
            path(PerformanceJpaEntity::performanceContact),
            performancePeriod(PerformancePeriodJpaValue::startDate),
            performancePeriod(PerformancePeriodJpaValue::endDate),
            path(PerformanceJpaEntity::legacyPerformancePeriod),
            path(PerformanceJpaEntity::ticketPrice),
            path(PerformanceJpaEntity::totalScheduleCount),
        ).from(
            entity(PerformanceJpaEntity::class),
        ).where(
            path(PerformanceJpaEntity::id).eq(performanceId),
        )
    }

    private fun findSchedules(performanceId: Long): List<PerformanceEditScheduleReadModel> {
        val query = jpql {
            selectNew<PerformanceEditScheduleProjection>(
                path(ScheduleJpaEntity::id),
                path(ScheduleJpaEntity::performanceDate),
                path(ScheduleJpaEntity::totalTicketCount),
                path(ScheduleJpaEntity::scheduleNumber),
            ).from(
                entity(ScheduleJpaEntity::class),
            ).where(
                path(ScheduleJpaEntity::performanceId).eq(performanceId),
            )
        }
        return entityManager.createQuery(query, jpqlRenderContext).resultList.map { projection ->
            PerformanceEditScheduleReadModel(
                id = checkNotNull(projection.scheduleId),
                performanceDate = projection.performanceDate,
                totalTicketCount = projection.totalTicketCount,
                scheduleNumber = projection.scheduleNumber.name,
            )
        }
    }

    private fun hasActiveBooking(performanceId: Long): Boolean {
        val query = jpql {
            val inactivePredicates = BookingStatus.inactiveForTicketAllocation()
                .map { path(BookingJpaEntity::bookingStatus).ne(it) }
            select(path(BookingJpaEntity::id)).from(
                entity(BookingJpaEntity::class),
                join(entity(ScheduleJpaEntity::class)).on(
                    path(BookingJpaEntity::scheduleId).eq(path(ScheduleJpaEntity::id)),
                ),
            ).whereAnd(
                path(ScheduleJpaEntity::performanceId).eq(performanceId),
                *inactivePredicates.toTypedArray(),
            )
        }
        return entityManager.createQuery(query, jpqlRenderContext)
            .setMaxResults(1)
            .resultList
            .isNotEmpty()
    }

    private fun findCasts(performanceId: Long): List<PerformanceEditCastReadModel> {
        val query = jpql {
            selectNew<PerformanceEditCastProjection>(
                path(CastJpaEntity::id),
                path(CastJpaEntity::castName),
                path(CastJpaEntity::castRole),
                path(CastJpaEntity::castPhoto),
            ).from(
                entity(CastJpaEntity::class),
            ).where(
                path(CastJpaEntity::performanceId).eq(performanceId),
            )
        }
        return entityManager.createQuery(query, jpqlRenderContext).resultList.map { projection ->
            PerformanceEditCastReadModel(
                id = checkNotNull(projection.castId),
                name = projection.name,
                role = projection.role,
                photo = projection.photo,
            )
        }
    }

    private fun findStaffs(performanceId: Long): List<PerformanceEditStaffReadModel> {
        val query = jpql {
            selectNew<PerformanceEditStaffProjection>(
                path(StaffJpaEntity::id),
                path(StaffJpaEntity::staffName),
                path(StaffJpaEntity::staffRole),
                path(StaffJpaEntity::staffPhoto),
            ).from(
                entity(StaffJpaEntity::class),
            ).where(
                path(StaffJpaEntity::performanceId).eq(performanceId),
            )
        }
        return entityManager.createQuery(query, jpqlRenderContext).resultList.map { projection ->
            PerformanceEditStaffReadModel(
                id = checkNotNull(projection.staffId),
                name = projection.name,
                role = projection.role,
                photo = projection.photo,
            )
        }
    }

    private fun findImages(performanceId: Long): List<PerformanceEditImageReadModel> {
        val query = jpql {
            selectNew<PerformanceEditImageProjection>(
                path(PerformanceImageJpaEntity::id),
                path(PerformanceImageJpaEntity::performanceImageUrl),
            ).from(
                entity(PerformanceImageJpaEntity::class),
            ).where(
                path(PerformanceImageJpaEntity::performanceId).eq(performanceId),
            )
        }
        return entityManager.createQuery(query, jpqlRenderContext).resultList.map { projection ->
            PerformanceEditImageReadModel(
                id = checkNotNull(projection.imageId),
                url = projection.url,
            )
        }
    }

    private fun formatPeriod(header: PerformanceEditHeaderProjection): String {
        val period = resolvePerformancePeriod(
            performanceId = checkNotNull(header.performanceId),
            startDate = header.periodStartDate,
            endDate = header.periodEndDate,
            legacyPeriod = header.legacyPeriod,
        )
        val start = period.startDate.format(PERIOD_FORMATTER)
        return if (period.startDate == period.endDate) start else "$start~${period.endDate.format(PERIOD_FORMATTER)}"
    }

    private data class PerformanceEditHeaderProjection(
        val performanceId: Long?,
        val userId: Long,
        val performanceTitle: String,
        val genre: Genre,
        val runningTime: Int,
        val performanceDescription: String,
        val performanceAttentionNote: String,
        val bankName: BankName?,
        val accountNumber: String?,
        val accountHolder: String?,
        val posterImage: String,
        val performanceTeamName: String,
        val performanceVenue: String,
        val roadAddressName: String,
        val placeDetailAddress: String,
        val latitude: String,
        val longitude: String,
        val performanceContact: String,
        val periodStartDate: LocalDate?,
        val periodEndDate: LocalDate?,
        val legacyPeriod: String,
        val ticketPrice: Int,
        val totalScheduleCount: Int,
    )

    private data class PerformanceEditScheduleProjection(
        val scheduleId: Long?,
        val performanceDate: LocalDateTime,
        val totalTicketCount: Int,
        val scheduleNumber: ScheduleNumber,
    )

    private data class PerformanceEditCastProjection(
        val castId: Long?,
        val name: String,
        val role: String,
        val photo: String,
    )

    private data class PerformanceEditStaffProjection(
        val staffId: Long?,
        val name: String,
        val role: String,
        val photo: String,
    )

    private data class PerformanceEditImageProjection(
        val imageId: Long?,
        val url: String,
    )

    private companion object {
        val PERIOD_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    }
}
