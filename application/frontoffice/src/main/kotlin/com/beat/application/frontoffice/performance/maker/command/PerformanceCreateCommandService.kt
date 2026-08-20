package com.beat.application.frontoffice.performance.maker.command

import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.performance.CastResult
import com.beat.application.frontoffice.performance.PerformanceImageResult
import com.beat.application.frontoffice.performance.StaffResult
import com.beat.application.frontoffice.performance.formatPerformancePeriod
import com.beat.application.frontoffice.performance.exception.PerformanceApplicationErrorCode
import com.beat.application.frontoffice.schedule.calculateDueDate
import com.beat.application.frontoffice.performance.maker.PerformanceMutationResult
import com.beat.application.frontoffice.performance.maker.ScheduleResult
import com.beat.domain.performance.model.Cast
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.performance.model.Performance
import com.beat.domain.performance.repository.PerformanceRepository
import com.beat.domain.performance.vo.PaymentAccount
import com.beat.domain.performance.vo.PerformancePeriod
import com.beat.domain.performance.vo.RunningTime
import com.beat.domain.performance.vo.TicketPrice
import com.beat.domain.performance.model.Genre
import com.beat.domain.performance.model.PerformanceImage
import com.beat.domain.schedule.model.Schedule
import com.beat.domain.schedule.model.ScheduleNumber
import com.beat.domain.sharedkernel.vo.BankName
import com.beat.domain.schedule.repository.ScheduleRepository
import com.beat.domain.schedule.service.ScheduleSequenceDomainService
import com.beat.domain.performance.model.Staff
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Clock

@Service
class PerformanceCreateCommandService(
    private val performanceRepository: PerformanceRepository,
    private val scheduleRepository: ScheduleRepository,
    private val memberRepository: MemberRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val scheduleSequenceDomainService: ScheduleSequenceDomainService,
    private val clock: Clock,
    private val performanceImageStorage: PerformanceImageStorage,
) {
    @Transactional
    fun createPerformance(memberId: Long, command: PerformanceCreateCommand): PerformanceMutationResult {
        val member = memberRepository.findById(memberId)
            .orElseThrow { FrontofficeApplicationException(PerformanceApplicationErrorCode.MEMBER_NOT_FOUND) }
        val performanceDates = command.schedules.map { it.performanceDate }
        if (performanceDates.isEmpty()) {
            throw FrontofficeApplicationException(PerformanceApplicationErrorCode.SCHEDULE_LIST_NOT_FOUND)
        }
        val casts = command.casts.map { castCommand ->
            Cast.create(
                castCommand.name,
                castCommand.role,
                validateStoredPerformanceImage(performanceImageStorage, castCommand.photo, "cast", required = false),
            )
        }
        val staffs = command.staffs.map { staffCommand ->
            Staff.create(
                staffCommand.name,
                staffCommand.role,
                validateStoredPerformanceImage(performanceImageStorage, staffCommand.photo, "staff", required = false),
            )
        }
        val images = command.images.map { imageCommand ->
            PerformanceImage.create(validateStoredPerformanceImage(performanceImageStorage, imageCommand.image, "performance"))
        }
        val performance = Performance.create(
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
            validateStoredPerformanceImage(performanceImageStorage, command.posterImage, "poster"),
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
            member.getUserId(),
            casts,
            staffs,
            images,
        )
        val savedPerformance = performanceRepository.save(performance)
        val performanceId = checkNotNull(savedPerformance.getId())
        val now = LocalDateTime.now(clock)
        var schedules = command.schedules.map { scheduleCommand ->
            Schedule.createUpcoming(
                scheduleCommand.performanceDate,
                savedPerformance.calculateEndAt(scheduleCommand.performanceDate),
                scheduleCommand.totalTicketCount,
                ScheduleNumber.valueOf(scheduleCommand.scheduleNumber.name),
                performanceId,
                now,
            )
        }
        schedules = scheduleRepository.saveAll(scheduleSequenceDomainService.assignScheduleNumbers(schedules))
        eventPublisher.publishEvent(PerformancePosterChangedEvent(savedPerformance.posterImage))
        return toResult(
            savedPerformance,
            schedules,
            savedPerformance.getCasts(),
            savedPerformance.getStaffs(),
            savedPerformance.getImages(),
        )
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
                schedule.getId(),
                schedule.getPerformanceDate(),
                schedule.getTotalTicketCount(),
                calculateDueDate(today, schedule),
                schedule.getScheduleNumber().name,
            )
        }
        val castResponses = casts.map { CastResult(it.getId(), it.castName, it.castRole, it.castPhoto) }
        val staffResponses = staffs.map { StaffResult(it.getId(), it.staffName, it.staffRole, it.staffPhoto) }
        val imageResponses = images.map { PerformanceImageResult(it.getId(), it.performanceImageUrl) }
        return PerformanceMutationResult(
            userId = performance.getUserId(),
            performanceId = performance.getId(),
            performanceTitle = performance.performanceTitle,
            genre = performance.genre.name,
            runningTime = performance.getRunningTime(),
            performanceDescription = performance.performanceDescription,
            performanceAttentionNote = performance.performanceAttentionNote,
            bankName = performance.getBankName()?.name,
            accountNumber = performance.getAccountNumber(),
            accountHolder = performance.getAccountHolder(),
            posterImage = performance.posterImage,
            performanceTeamName = performance.performanceTeamName,
            performanceVenue = performance.performanceVenue,
            roadAddressName = performance.roadAddressName,
            placeDetailAddress = performance.placeDetailAddress,
            latitude = performance.latitude,
            longitude = performance.longitude,
            performanceContact = performance.performanceContact,
            performancePeriod = formatPerformancePeriod(performance.getPerformancePeriodValue()),
            ticketPrice = performance.getTicketPrice(),
            totalScheduleCount = performance.totalScheduleCount,
            schedules = scheduleResponses,
            casts = castResponses,
            staffs = staffResponses,
            images = imageResponses,
        )
    }
}
