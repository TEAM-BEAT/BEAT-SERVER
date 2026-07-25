package com.beat.apis.performance.application.command

import com.beat.apis.exception.ApiApplicationException
import com.beat.apis.member.exception.MemberApplicationErrorCode
import com.beat.apis.performance.application.result.CastResult
import com.beat.apis.performance.application.result.PerformanceImageResult
import com.beat.apis.performance.application.result.PerformanceMutationResult
import com.beat.apis.performance.application.result.ScheduleResult
import com.beat.apis.performance.application.result.StaffResult
import com.beat.apis.performance.application.formatPerformancePeriod
import com.beat.apis.performance.application.extractRequiredPerformanceImageKey
import com.beat.apis.performance.application.event.PerformancePosterChangedEvent
import com.beat.apis.performance.exception.PerformanceApplicationErrorCode
import com.beat.apis.performance.exception.CastApplicationErrorCode
import com.beat.apis.performance.exception.PerformanceImageApplicationErrorCode
import com.beat.apis.performance.exception.StaffApplicationErrorCode
import com.beat.contracts.performance.PerformanceContentOwnershipReadPort
import com.beat.domain.member.model.Member
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
import com.beat.domain.sharedkernel.vo.BankName
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PerformanceModifyCommandService internal constructor(
    private val performanceRepository: PerformanceRepository,
    private val memberRepository: MemberRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val scheduleSynchronizer: ScheduleSynchronizer,
    private val contentOwnershipReadPort: PerformanceContentOwnershipReadPort,
) {

    @Transactional
    fun modifyPerformance(memberId: Long, command: PerformanceModifyCommand): PerformanceMutationResult {
        log.info { "Starting updatePerformance for memberId: ${memberId}, performanceId: ${command.performanceId}" }
        val member = findMember(memberId)
        var performance = findPerformance(command.performanceId)
        if (!performance.isOwnedBy(member.getUserId())) {
            throw ApiApplicationException(PerformanceApplicationErrorCode.NOT_PERFORMANCE_OWNER)
        }
        validateModificationRequest(command)
        val scheduleCommands = command.schedules
        val castCommands = command.casts
        val staffCommands = command.staffs
        val imageCommands = command.images

        val hasActiveBooking = scheduleSynchronizer.lockAndCheckActiveBookings(command.performanceId)
        performance = updatePerformance(performance, command, scheduleCommands, hasActiveBooking).replaceContent(
            synchronizeCasts(performance, castCommands),
            synchronizeStaffs(performance, staffCommands),
            synchronizeImages(performance, imageCommands),
        )
        performance = performanceRepository.save(performance)
        val schedules = scheduleSynchronizer.synchronize(scheduleCommands, performance)
        val casts = performance.getCasts().map { CastResult(it.getId(), it.castName, it.castRole, it.castPhoto) }
        val staffs = performance.getStaffs().map {
            StaffResult(it.getId(), it.staffName, it.staffRole, it.staffPhoto)
        }
        val images = performance.getImages().map {
            PerformanceImageResult(it.getId(), it.performanceImageUrl)
        }
        val result = toResult(performance, schedules, casts, staffs, images)
        eventPublisher.publishEvent(PerformancePosterChangedEvent(performance.posterImage))
        log.info { "Successfully completed updatePerformance for performanceId: ${command.performanceId}" }
        return result
    }

    private fun validateModificationRequest(command: PerformanceModifyCommand) {
        if (command.schedules.isEmpty()) {
            throw ApiApplicationException(PerformanceApplicationErrorCode.SCHEDULE_LIST_NOT_FOUND)
        }
        if (hasDuplicateIds(command.schedules.map(ScheduleModifyCommand::scheduleId)) ||
            hasDuplicateIds(command.casts.map(CastModifyCommand::id)) ||
            hasDuplicateIds(command.staffs.map(StaffModifyCommand::id)) ||
            hasDuplicateIds(command.images.map(PerformanceImageModifyCommand::id))
        ) {
            throw ApiApplicationException(PerformanceApplicationErrorCode.DUPLICATE_MODIFICATION_ID)
        }
    }

    private fun hasDuplicateIds(ids: List<Long?>): Boolean {
        val uniqueIds = hashSetOf<Long>()
        return ids.filterNotNull().any { !uniqueIds.add(it) }
    }

    private fun updatePerformance(
        current: Performance,
        command: PerformanceModifyCommand,
        scheduleCommands: List<ScheduleModifyCommand>,
        hasActiveBooking: Boolean,
    ): Performance {
        val performanceDates = scheduleCommands.map(ScheduleModifyCommand::performanceDate)
        if (performanceDates.isEmpty()) {
            throw ApiApplicationException(PerformanceApplicationErrorCode.SCHEDULE_LIST_NOT_FOUND)
        }
        var updated = current.update(
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
            extractRequiredPerformanceImageKey(command.posterImage),
            command.performanceTeamName,
            command.performanceVenue,
            command.roadAddressName,
            command.placeDetailAddress,
            command.latitude,
            command.longitude,
            command.performanceContact,
            PerformancePeriod.fromPerformanceDateTimes(performanceDates),
            scheduleCommands.size,
        )
        return updated.updateTicketPrice(command.ticketPrice, hasActiveBooking)
    }

    private fun synchronizeCasts(performance: Performance, commands: List<CastModifyCommand>): List<Cast> {
        val existing = performance.getCasts().associateBy { it.getId() }
        return commands.map { command ->
            val castId = command.id
            if (castId == null) {
                Cast.create(
                    command.name,
                    command.role,
                    extractRequiredPerformanceImageKey(command.photo),
                )
            } else {
                val cast = existing[castId] ?: throwInvalidCast(castId)
                cast.update(
                    command.name,
                    command.role,
                    extractRequiredPerformanceImageKey(command.photo),
                )
            }
        }
    }

    private fun synchronizeStaffs(performance: Performance, commands: List<StaffModifyCommand>): List<Staff> {
        val existing = performance.getStaffs().associateBy { it.getId() }
        return commands.map { command ->
            val staffId = command.id
            if (staffId == null) {
                Staff.create(
                    command.name,
                    command.role,
                    extractRequiredPerformanceImageKey(command.photo),
                )
            } else {
                val staff = existing[staffId] ?: throwInvalidStaff(staffId)
                staff.update(
                    command.name,
                    command.role,
                    extractRequiredPerformanceImageKey(command.photo),
                )
            }
        }
    }

    private fun synchronizeImages(
        performance: Performance,
        commands: List<PerformanceImageModifyCommand>,
    ): List<PerformanceImage> {
        val existing = performance.getImages().associateBy { it.getId() }
        return commands.map { command ->
            val imageId = command.id
            val imageKey = extractRequiredPerformanceImageKey(command.image)
            if (imageId == null) {
                PerformanceImage.create(imageKey)
            } else {
                val image = existing[imageId] ?: throwInvalidImage(imageId)
                image.update(imageKey)
            }
        }
    }

    private fun throwInvalidCast(castId: Long): Nothing {
        val error = if (contentOwnershipReadPort.findPerformanceIdByCastId(castId) == null) {
            CastApplicationErrorCode.CAST_NOT_FOUND
        } else {
            CastApplicationErrorCode.CAST_NOT_BELONG_TO_PERFORMANCE
        }
        throw ApiApplicationException(error)
    }

    private fun throwInvalidStaff(staffId: Long): Nothing {
        val error = if (contentOwnershipReadPort.findPerformanceIdByStaffId(staffId) == null) {
            StaffApplicationErrorCode.STAFF_NOT_FOUND
        } else {
            StaffApplicationErrorCode.STAFF_NOT_BELONG_TO_PERFORMANCE
        }
        throw ApiApplicationException(error)
    }

    private fun throwInvalidImage(imageId: Long): Nothing {
        val error = if (contentOwnershipReadPort.findPerformanceIdByImageId(imageId) == null) {
            PerformanceImageApplicationErrorCode.PERFORMANCE_IMAGE_NOT_FOUND
        } else {
            PerformanceImageApplicationErrorCode.PERFORMANCE_IMAGE_NOT_BELONG_TO_PERFORMANCE
        }
        throw ApiApplicationException(error)
    }

    private fun toResult(
        performance: Performance,
        schedules: List<ScheduleResult>,
        casts: List<CastResult>,
        staffs: List<StaffResult>,
        images: List<PerformanceImageResult>,
    ): PerformanceMutationResult = PerformanceMutationResult(
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
        schedules = schedules,
        casts = casts,
        staffs = staffs,
        images = images,
    )

    private fun findMember(memberId: Long): Member = memberRepository.findById(memberId)
        .orElseThrow { ApiApplicationException(MemberApplicationErrorCode.MEMBER_NOT_FOUND) }

    private fun findPerformance(performanceId: Long): Performance = performanceRepository.lockById(performanceId)
        .orElseThrow { ApiApplicationException(PerformanceApplicationErrorCode.PERFORMANCE_NOT_FOUND) }

    private companion object {
        val log = KotlinLogging.logger {}
    }
}
