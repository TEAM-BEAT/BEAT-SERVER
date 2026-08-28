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
import java.time.format.DateTimeFormatter
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
internal class PerformanceEditFormQueries(private val dsl: DSLContext) : PerformanceEditFormReader {

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
                Performance.TICKET_PRICE,
                Performance.TOTAL_SCHEDULE_COUNT,
            )
            .from(Performance.TABLE)
            .where(Performance.ID.eq(performanceId))
            .fetchOne { record ->
                PerformanceEditHeaderProjection(
                    performanceId = record[Performance.ID],
                    userId = record[Performance.USER_ID]!!,
                    performanceTitle = record[Performance.PERFORMANCE_TITLE]!!,
                    genre = record[Performance.GENRE]!!,
                    runningTime = record[Performance.RUNNING_TIME]!!,
                    performanceDescription = record[Performance.PERFORMANCE_DESCRIPTION]!!,
                    performanceAttentionNote = record[Performance.PERFORMANCE_ATTENTION_NOTE]!!,
                    bankName = record[Performance.BANK_NAME],
                    accountNumber = record[Performance.ACCOUNT_NUMBER],
                    accountHolder = record[Performance.ACCOUNT_HOLDER],
                    posterImage = record[Performance.POSTER_IMAGE]!!,
                    performanceTeamName = record[Performance.PERFORMANCE_TEAM_NAME]!!,
                    performanceVenue = record[Performance.PERFORMANCE_VENUE]!!,
                    roadAddressName = record[Performance.ROAD_ADDRESS_NAME]!!,
                    placeDetailAddress = record[Performance.PLACE_DETAIL_ADDRESS]!!,
                    latitude = record[Performance.LATITUDE]!!,
                    longitude = record[Performance.LONGITUDE]!!,
                    performanceContact = record[Performance.PERFORMANCE_CONTACT]!!,
                    periodStartDate = record[Performance.PERFORMANCE_START_DATE],
                    periodEndDate = record[Performance.PERFORMANCE_END_DATE],
                    ticketPrice = record[Performance.TICKET_PRICE]!!,
                    totalScheduleCount = record[Performance.TOTAL_SCHEDULE_COUNT]!!,
                )
            }

    private fun findSchedules(performanceId: Long): List<PerformanceEditScheduleReadModel> =
        dsl.select(
                Schedule.ID,
                Schedule.PERFORMANCE_DATE,
                Schedule.TOTAL_TICKET_COUNT,
                Schedule.SCHEDULE_NUMBER,
            )
            .from(Schedule.TABLE)
            .where(Schedule.PERFORMANCE_ID.eq(performanceId))
            .fetch { record ->
                PerformanceEditScheduleReadModel(
                    id = record[Schedule.ID]!!,
                    performanceDate = record[Schedule.PERFORMANCE_DATE]!!,
                    totalTicketCount = record[Schedule.TOTAL_TICKET_COUNT]!!,
                    scheduleNumber = record[Schedule.SCHEDULE_NUMBER]!!,
                )
            }

    private fun hasActiveBooking(performanceId: Long): Boolean {
        val inactiveStatuses = BookingStatus.inactiveForTicketAllocation().map { it.name }
        val conditions = mutableListOf<org.jooq.Condition>()
        conditions.add(Schedule.PERFORMANCE_ID.eq(performanceId))
        if (inactiveStatuses.isNotEmpty()) {
            conditions.add(Booking.BOOKING_STATUS.notIn(inactiveStatuses))
        }
        val query =
            dsl.select(Booking.ID)
                .from(Booking.TABLE)
                .join(Schedule.TABLE)
                .on(Booking.SCHEDULE_ID.eq(Schedule.ID))
                .where(conditions)
        return dsl.fetchExists(query)
    }

    private fun findCasts(performanceId: Long): List<PerformanceEditCastReadModel> =
        dsl.select(
                CastTable.ID,
                CastTable.CAST_NAME,
                CastTable.CAST_ROLE,
                CastTable.CAST_PHOTO,
            )
            .from(CastTable.TABLE)
            .where(CastTable.PERFORMANCE_ID.eq(performanceId))
            .fetch { record ->
                PerformanceEditCastReadModel(
                    id = record[CastTable.ID]!!,
                    name = record[CastTable.CAST_NAME]!!,
                    role = record[CastTable.CAST_ROLE]!!,
                    photo = record[CastTable.CAST_PHOTO]!!,
                )
            }

    private fun findStaffs(performanceId: Long): List<PerformanceEditStaffReadModel> =
        dsl.select(
                StaffTable.ID,
                StaffTable.STAFF_NAME,
                StaffTable.STAFF_ROLE,
                StaffTable.STAFF_PHOTO,
            )
            .from(StaffTable.TABLE)
            .where(StaffTable.PERFORMANCE_ID.eq(performanceId))
            .fetch { record ->
                PerformanceEditStaffReadModel(
                    id = record[StaffTable.ID]!!,
                    name = record[StaffTable.STAFF_NAME]!!,
                    role = record[StaffTable.STAFF_ROLE]!!,
                    photo = record[StaffTable.STAFF_PHOTO]!!,
                )
            }

    private fun findImages(performanceId: Long): List<PerformanceEditImageReadModel> =
        dsl.select(
                PerformanceImage.ID,
                PerformanceImage.PERFORMANCE_IMAGE_URL,
            )
            .from(PerformanceImage.TABLE)
            .where(PerformanceImage.PERFORMANCE_ID.eq(performanceId))
            .fetch { record ->
                PerformanceEditImageReadModel(
                    id = record[PerformanceImage.ID]!!,
                    url = record[PerformanceImage.PERFORMANCE_IMAGE_URL]!!,
                )
            }

    private fun formatPeriod(header: PerformanceEditHeaderProjection): String {
        val period =
            com.beat.domain.performance.vo.PerformancePeriod.of(
                header.periodStartDate!!,
                header.periodEndDate!!,
            )
        val start = period.startDate.format(PERIOD_FORMATTER)
        return if (period.startDate == period.endDate) start
        else "$start~${period.endDate.format(PERIOD_FORMATTER)}"
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
        val ticketPrice: Int,
        val totalScheduleCount: Int,
    )

    private companion object {
        val PERIOD_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    }
}
