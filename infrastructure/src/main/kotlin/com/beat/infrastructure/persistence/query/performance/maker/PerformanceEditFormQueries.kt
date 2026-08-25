package com.beat.infrastructure.persistence.query.performance.maker

import com.beat.application.frontoffice.performance.maker.query.PerformanceEditCastReadModel
import com.beat.application.frontoffice.performance.maker.query.PerformanceEditFormReadModel
import com.beat.application.frontoffice.performance.maker.query.PerformanceEditFormReader
import com.beat.application.frontoffice.performance.maker.query.PerformanceEditImageReadModel
import com.beat.application.frontoffice.performance.maker.query.PerformanceEditScheduleReadModel
import com.beat.application.frontoffice.performance.maker.query.PerformanceEditStaffReadModel
import com.beat.domain.booking.model.BookingStatus
import com.beat.infrastructure.jooq.generated.Booking
import com.beat.infrastructure.jooq.generated.CastTable
import com.beat.infrastructure.jooq.generated.Performance
import com.beat.infrastructure.jooq.generated.PerformanceImage
import com.beat.infrastructure.jooq.generated.Schedule
import com.beat.infrastructure.jooq.generated.StaffTable
import com.beat.infrastructure.persistence.performance.repository.query.resolvePerformancePeriod
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.format.DateTimeFormatter

@Repository
internal class PerformanceEditFormQueries(
    private val dsl: DSLContext,
) : PerformanceEditFormReader {

    override fun findByPerformanceId(performanceId: Long): PerformanceEditFormReadModel? {
        val header = findHeader(performanceId) ?: return null
        val schedules = findSchedules(performanceId)

        return PerformanceEditFormReadModel(
            performanceId = checkNotNull(header.performanceId),
            userId = header.userId,
            performanceTitle = header.performanceTitle,
            genre = header.genre,
            runningTime = header.runningTime,
            performanceDescription = header.performanceDescription,
            performanceAttentionNote = header.performanceAttentionNote,
            bankName = header.bankName,
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

    private fun findHeader(performanceId: Long): PerformanceEditHeaderProjection? =
        dsl.select(
            Performance.ID,
            Performance.USER_ID,
            Performance.PERFORMANCE_TITLE,
            Performance.GENRE,
            Performance.RUNNING_TIME,
            Performance.PERFORMANCE_DESCRIPTION,
            Performance.PERFORMANCE_ATTENTION_NOTE,
            Performance.BANK_NAME,
            Performance.ACCOUNT_NUMBER,
            Performance.ACCOUNT_HOLDER,
            Performance.POSTER_IMAGE,
            Performance.PERFORMANCE_TEAM_NAME,
            Performance.PERFORMANCE_VENUE,
            Performance.ROAD_ADDRESS_NAME,
            Performance.PLACE_DETAIL_ADDRESS,
            Performance.LATITUDE,
            Performance.LONGITUDE,
            Performance.PERFORMANCE_CONTACT,
            Performance.PERFORMANCE_START_DATE,
            Performance.PERFORMANCE_END_DATE,
            Performance.PERFORMANCE_PERIOD,
            Performance.TICKET_PRICE,
            Performance.TOTAL_SCHEDULE_COUNT,
        ).from(Performance.TABLE)
            .where(Performance.ID.eq(performanceId))
            .fetchOne { record ->
                PerformanceEditHeaderProjection(
                    performanceId = record.get(Performance.ID),
                    userId = record.get(Performance.USER_ID)!!,
                    performanceTitle = record.get(Performance.PERFORMANCE_TITLE)!!,
                    genre = record.get(Performance.GENRE)!!,
                    runningTime = record.get(Performance.RUNNING_TIME)!!,
                    performanceDescription = record.get(Performance.PERFORMANCE_DESCRIPTION)!!,
                    performanceAttentionNote = record.get(Performance.PERFORMANCE_ATTENTION_NOTE)!!,
                    bankName = record.get(Performance.BANK_NAME),
                    accountNumber = record.get(Performance.ACCOUNT_NUMBER),
                    accountHolder = record.get(Performance.ACCOUNT_HOLDER),
                    posterImage = record.get(Performance.POSTER_IMAGE)!!,
                    performanceTeamName = record.get(Performance.PERFORMANCE_TEAM_NAME)!!,
                    performanceVenue = record.get(Performance.PERFORMANCE_VENUE)!!,
                    roadAddressName = record.get(Performance.ROAD_ADDRESS_NAME)!!,
                    placeDetailAddress = record.get(Performance.PLACE_DETAIL_ADDRESS)!!,
                    latitude = record.get(Performance.LATITUDE)!!,
                    longitude = record.get(Performance.LONGITUDE)!!,
                    performanceContact = record.get(Performance.PERFORMANCE_CONTACT)!!,
                    periodStartDate = record.get(Performance.PERFORMANCE_START_DATE),
                    periodEndDate = record.get(Performance.PERFORMANCE_END_DATE),
                    legacyPeriod = record.get(Performance.PERFORMANCE_PERIOD)!!,
                    ticketPrice = record.get(Performance.TICKET_PRICE)!!,
                    totalScheduleCount = record.get(Performance.TOTAL_SCHEDULE_COUNT)!!,
                )
            }

    private fun findSchedules(performanceId: Long): List<PerformanceEditScheduleReadModel> =
        dsl.select(
            Schedule.ID,
            Schedule.PERFORMANCE_DATE,
            Schedule.TOTAL_TICKET_COUNT,
            Schedule.SCHEDULE_NUMBER,
        ).from(Schedule.TABLE)
            .where(Schedule.PERFORMANCE_ID.eq(performanceId))
            .fetch { record ->
                PerformanceEditScheduleReadModel(
                    id = record.get(Schedule.ID)!!,
                    performanceDate = record.get(Schedule.PERFORMANCE_DATE)!!,
                    totalTicketCount = record.get(Schedule.TOTAL_TICKET_COUNT)!!,
                    scheduleNumber = record.get(Schedule.SCHEDULE_NUMBER)!!,
                )
            }

    private fun hasActiveBooking(performanceId: Long): Boolean {
        val inactiveStatuses = BookingStatus.inactiveForTicketAllocation().map { it.name }
        val conditions = mutableListOf<org.jooq.Condition>()
        conditions.add(Schedule.PERFORMANCE_ID.eq(performanceId))
        if (inactiveStatuses.isNotEmpty()) {
            conditions.add(Booking.BOOKING_STATUS.notIn(inactiveStatuses))
        }
        val query = dsl.select(Booking.ID)
            .from(Booking.TABLE)
            .join(Schedule.TABLE).on(Booking.SCHEDULE_ID.eq(Schedule.ID))
            .where(conditions)
        return dsl.fetchExists(query)
    }

    private fun findCasts(performanceId: Long): List<PerformanceEditCastReadModel> =
        dsl.select(
            CastTable.ID,
            CastTable.CAST_NAME,
            CastTable.CAST_ROLE,
            CastTable.CAST_PHOTO,
        ).from(CastTable.TABLE)
            .where(CastTable.PERFORMANCE_ID.eq(performanceId))
            .fetch { record ->
                PerformanceEditCastReadModel(
                    id = record.get(CastTable.ID)!!,
                    name = record.get(CastTable.CAST_NAME)!!,
                    role = record.get(CastTable.CAST_ROLE)!!,
                    photo = record.get(CastTable.CAST_PHOTO)!!,
                )
            }

    private fun findStaffs(performanceId: Long): List<PerformanceEditStaffReadModel> =
        dsl.select(
            StaffTable.ID,
            StaffTable.STAFF_NAME,
            StaffTable.STAFF_ROLE,
            StaffTable.STAFF_PHOTO,
        ).from(StaffTable.TABLE)
            .where(StaffTable.PERFORMANCE_ID.eq(performanceId))
            .fetch { record ->
                PerformanceEditStaffReadModel(
                    id = record.get(StaffTable.ID)!!,
                    name = record.get(StaffTable.STAFF_NAME)!!,
                    role = record.get(StaffTable.STAFF_ROLE)!!,
                    photo = record.get(StaffTable.STAFF_PHOTO)!!,
                )
            }

    private fun findImages(performanceId: Long): List<PerformanceEditImageReadModel> =
        dsl.select(
            PerformanceImage.ID,
            PerformanceImage.PERFORMANCE_IMAGE_URL,
        ).from(PerformanceImage.TABLE)
            .where(PerformanceImage.PERFORMANCE_ID.eq(performanceId))
            .fetch { record ->
                PerformanceEditImageReadModel(
                    id = record.get(PerformanceImage.ID)!!,
                    url = record.get(PerformanceImage.PERFORMANCE_IMAGE_URL)!!,
                )
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
        val genre: String,
        val runningTime: Int,
        val performanceDescription: String,
        val performanceAttentionNote: String,
        val bankName: String?,
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
        val periodStartDate: java.time.LocalDate?,
        val periodEndDate: java.time.LocalDate?,
        val legacyPeriod: String,
        val ticketPrice: Int,
        val totalScheduleCount: Int,
    )

    private companion object {
        val PERIOD_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    }
}
