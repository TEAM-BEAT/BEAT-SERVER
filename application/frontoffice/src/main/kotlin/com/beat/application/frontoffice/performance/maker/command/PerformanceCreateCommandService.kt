package com.beat.application.frontoffice.performance.maker.command

import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.exception.translateDomainFailure
import com.beat.application.frontoffice.performance.CastResult
import com.beat.application.frontoffice.performance.PerformanceImageResult
import com.beat.application.frontoffice.performance.StaffResult
import com.beat.application.frontoffice.performance.exception.PerformanceApplicationErrorCode
import com.beat.application.frontoffice.performance.formatPerformancePeriod
import com.beat.application.frontoffice.performance.maker.PerformanceMutationResult
import com.beat.application.frontoffice.performance.maker.ScheduleResult
import com.beat.application.frontoffice.schedule.calculateDueDate
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.performance.model.Cast
import com.beat.domain.performance.model.Genre
import com.beat.domain.performance.model.Performance
import com.beat.domain.performance.model.PerformanceImage
import com.beat.domain.performance.model.Staff
import com.beat.domain.performance.repository.PerformanceRepository
import com.beat.domain.performance.vo.PaymentAccount
import com.beat.domain.performance.vo.PerformancePeriod
import com.beat.domain.performance.vo.RunningTime
import com.beat.domain.performance.vo.TicketPrice
import com.beat.domain.schedule.model.Schedule
import com.beat.domain.schedule.model.ScheduleNumber
import com.beat.domain.schedule.repository.ScheduleRepository
import com.beat.domain.schedule.service.ScheduleSequenceDomainService
import com.beat.domain.sharedkernel.vo.BankName
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PerformanceCreateCommandService
internal constructor(
    private val performanceRepository: PerformanceRepository,
    private val scheduleRepository: ScheduleRepository,
    private val memberRepository: MemberRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val scheduleSequenceDomainService: ScheduleSequenceDomainService,
    private val clock: Clock,
    private val performanceImageStorage: PerformanceImageStorage,
) {
    @Transactional
    fun createPerformance(
        memberId: Long,
        command: PerformanceCreateCommand,
    ): PerformanceMutationResult {
        return translateDomainFailure {
            val member =
                memberRepository.findById(memberId)
                    ?: throw FrontofficeApplicationException(
                        PerformanceApplicationErrorCode.MEMBER_NOT_FOUND
                    )
            val performanceDates = command.schedules.map { it.performanceDate }
            if (performanceDates.isEmpty()) {
                throw FrontofficeApplicationException(
                    PerformanceApplicationErrorCode.SCHEDULE_LIST_NOT_FOUND
                )
            }
            val casts =
                command.casts.map { castCommand ->
                    Cast.create(
                        castCommand.name,
                        castCommand.role,
                        validateStoredPerformanceImage(
                            performanceImageStorage,
                            castCommand.photo,
                            "cast",
                            required = false,
                        ),
                    )
                }
            val staffs =
                command.staffs.map { staffCommand ->
                    Staff.create(
                        staffCommand.name,
                        staffCommand.role,
                        validateStoredPerformanceImage(
                            performanceImageStorage,
                            staffCommand.photo,
                            "staff",
                            required = false,
                        ),
                    )
                }
            val images =
                command.images.map { imageCommand ->
                    PerformanceImage.create(
                        validateStoredPerformanceImage(
                            performanceImageStorage,
                            imageCommand.image,
                            "performance",
                        )
                    )
                }
            val performance =
                Performance.create(
                    command.performanceTitle,
                    Genre.valueOf(command.genre.name),
                    RunningTime.of(command.runningTime),
                    command.performanceDescription,
                    command.performanceAttentionNote,
                    PaymentAccount.fromNullable(
                        command.bankName?.let { BankName.valueOf(it.name) },
                        command.accountNumber,
                        command.accountHolder,
                    ),
                    validateStoredPerformanceImage(
                        performanceImageStorage,
                        command.posterImage,
                        "poster",
                    ),
                    command.performanceTeamName,
                    command.performanceVenue,
                    command.roadAddressName,
                    command.placeDetailAddress,
                    command.latitude,
                    command.longitude,
                    command.performanceContact,
                    PerformancePeriod.fromPerformanceDateTimes(performanceDates),
                    TicketPrice.of(command.ticketPrice),
                    command.schedules.size,
                    member.userId,
                    casts,
                    staffs,
                    images,
                )
            val savedPerformance = performanceRepository.save(performance)
            val performanceId = checkNotNull(savedPerformance.id)
            val now = LocalDateTime.now(clock)
            var schedules =
                command.schedules.map { scheduleCommand ->
                    Schedule.createUpcoming(
                        scheduleCommand.performanceDate,
                        savedPerformance.calculateEndAt(scheduleCommand.performanceDate),
                        scheduleCommand.totalTicketCount,
                        ScheduleNumber.valueOf(scheduleCommand.scheduleNumber.name),
                        performanceId,
                        now,
                    )
                }
            schedules =
                scheduleRepository.saveAll(
                    scheduleSequenceDomainService.assignScheduleNumbers(schedules)
                )
            eventPublisher.publishEvent(PerformancePosterChangedEvent(savedPerformance.posterImage))
            toResult(
                savedPerformance,
                schedules,
                savedPerformance.casts,
                savedPerformance.staffs,
                savedPerformance.images,
            )
        }
    }

    private fun toResult(
        performance: Performance,
        schedules: List<Schedule>,
        casts: List<Cast>,
        staffs: List<Staff>,
        images: List<PerformanceImage>,
    ): PerformanceMutationResult {
        val today = LocalDate.now(clock)
        val scheduleResponses = schedules.map { schedule ->
            ScheduleResult(
                schedule.id,
                schedule.performanceDate,
                schedule.totalTicketCount,
                calculateDueDate(today, schedule),
                schedule.scheduleNumber.name,
            )
        }
        val castResponses = casts.map { CastResult(it.id, it.castName, it.castRole, it.castPhoto) }
        val staffResponses = staffs.map {
            StaffResult(it.id, it.staffName, it.staffRole, it.staffPhoto)
        }
        val imageResponses = images.map { PerformanceImageResult(it.id, it.performanceImageUrl) }
        return PerformanceMutationResult(
            userId = performance.userId,
            performanceId = performance.id,
            performanceTitle = performance.performanceTitle,
            genre = performance.genre.name,
            runningTime = performance.runningTime,
            performanceDescription = performance.performanceDescription,
            performanceAttentionNote = performance.performanceAttentionNote,
            bankName = performance.bankName?.name,
            accountNumber = performance.accountNumber,
            accountHolder = performance.accountHolder,
            posterImage = performance.posterImage,
            performanceTeamName = performance.performanceTeamName,
            performanceVenue = performance.performanceVenue,
            roadAddressName = performance.roadAddressName,
            placeDetailAddress = performance.placeDetailAddress,
            latitude = performance.latitude,
            longitude = performance.longitude,
            performanceContact = performance.performanceContact,
            performancePeriod = formatPerformancePeriod(performance.performancePeriodValue),
            ticketPrice = performance.ticketPrice,
            totalScheduleCount = performance.totalScheduleCount,
            schedules = scheduleResponses,
            casts = castResponses,
            staffs = staffResponses,
            images = imageResponses,
        )
    }
}
