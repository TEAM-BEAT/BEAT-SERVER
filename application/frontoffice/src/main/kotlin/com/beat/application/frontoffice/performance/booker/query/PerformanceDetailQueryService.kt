package com.beat.application.frontoffice.performance.booker.query

import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.exception.translateDomainFailure
import com.beat.application.frontoffice.performance.exception.PerformanceApplicationErrorCode
import com.beat.application.frontoffice.performance.formatPerformancePeriod
import com.beat.application.frontoffice.performance.nearestDueDate
import com.beat.application.frontoffice.schedule.calculateDueDate
import com.beat.domain.performance.model.Performance
import com.beat.domain.performance.repository.PerformanceRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PerformanceDetailQueryService
internal constructor(
    private val performanceRepository: PerformanceRepository,
    private val scheduleAvailabilityReader: PerformanceScheduleAvailabilityReader,
) {
    @Transactional(readOnly = true)
    fun getPerformanceDetail(performanceId: Long): PerformanceDetailResult {
        return translateDomainFailure {
            val performance = findPerformance(performanceId)
            val schedules = scheduleAvailabilityReader.findAllByPerformanceId(performanceId)
            val scheduleResponses = schedules.map { schedule ->
                val dueDate =
                    calculateDueDate(schedule.evaluatedAt.toLocalDate(), schedule.performanceDate)
                PerformanceDetailScheduleResult(
                    schedule.scheduleId,
                    schedule.performanceDate,
                    schedule.scheduleNumber,
                    dueDate,
                    schedule.isBooking,
                )
            }
            val minDueDate =
                if (schedules.isEmpty()) {
                    Int.MAX_VALUE
                } else {
                    nearestDueDate(
                        schedules.first().evaluatedAt.toLocalDate(),
                        schedules.map { it.performanceDate },
                    )
                }
            val casts =
                performance.casts.map { cast ->
                    CastResult(cast.id, cast.castName, cast.castRole, cast.castPhoto)
                }
            val staffs =
                performance.staffs.map { staff ->
                    StaffResult(staff.id, staff.staffName, staff.staffRole, staff.staffPhoto)
                }
            val images =
                performance.images.map { image ->
                    PerformanceImageResult(image.id, image.performanceImageUrl)
                }

            log.info {
                "Successfully completed getPerformanceDetail for performanceId: ${performanceId}"
            }
            PerformanceDetailResult(
                performanceId = performance.id,
                performanceTitle = performance.performanceTitle,
                performancePeriod = formatPerformancePeriod(performance.performancePeriodValue),
                schedules = scheduleResponses,
                ticketPrice = performance.ticketPrice,
                genre = performance.genre.name,
                posterImage = performance.posterImage,
                runningTime = performance.runningTime,
                performanceVenue = performance.performanceVenue,
                roadAddressName = performance.roadAddressName,
                placeDetailAddress = performance.placeDetailAddress,
                latitude = performance.latitude,
                longitude = performance.longitude,
                performanceDescription = performance.performanceDescription,
                performanceAttentionNote = performance.performanceAttentionNote,
                performanceContact = performance.performanceContact,
                performanceTeamName = performance.performanceTeamName,
                casts = casts,
                staffs = staffs,
                minDueDate = minDueDate,
                images = images,
            )
        }
    }

    @Transactional(readOnly = true)
    fun getBookingPerformanceDetail(performanceId: Long): BookingPerformanceDetailResult {
        return translateDomainFailure {
            val performance = findPerformance(performanceId)
            val schedules =
                scheduleAvailabilityReader.findAllByPerformanceId(performanceId).map { schedule ->
                    val dueDate =
                        calculateDueDate(
                            schedule.evaluatedAt.toLocalDate(),
                            schedule.performanceDate,
                        )
                    BookingPerformanceScheduleResult(
                        schedule.scheduleId,
                        schedule.performanceDate,
                        schedule.scheduleNumber,
                        schedule.availableTicketCount,
                        schedule.isBooking,
                        dueDate,
                    )
                }
            BookingPerformanceDetailResult(
                performance.id,
                performance.performanceTitle,
                formatPerformancePeriod(performance.performancePeriodValue),
                schedules,
                performance.ticketPrice,
                performance.genre.name,
                performance.posterImage,
                performance.performanceVenue,
                performance.performanceTeamName,
                performance.bankName?.name,
                performance.accountNumber,
                performance.accountHolder,
            )
        }
    }

    private fun findPerformance(performanceId: Long): Performance =
        performanceRepository.findById(performanceId)
            ?: throw FrontofficeApplicationException(
                PerformanceApplicationErrorCode.PERFORMANCE_NOT_FOUND
            )

    private companion object {
        val log = KotlinLogging.logger {}
    }
}
