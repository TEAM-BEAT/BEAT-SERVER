package com.beat.application.frontoffice.performance.maker.command

import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.exception.translateDomainFailure
import com.beat.application.frontoffice.performance.CastResult
import com.beat.application.frontoffice.performance.PerformanceImageResult
import com.beat.application.frontoffice.performance.StaffResult
import com.beat.application.frontoffice.performance.exception.CastApplicationErrorCode
import com.beat.application.frontoffice.performance.exception.PerformanceApplicationErrorCode
import com.beat.application.frontoffice.performance.exception.PerformanceImageApplicationErrorCode
import com.beat.application.frontoffice.performance.exception.StaffApplicationErrorCode
import com.beat.application.frontoffice.performance.formatPerformancePeriod
import com.beat.application.frontoffice.performance.maker.PerformanceMutationResult
import com.beat.application.frontoffice.performance.maker.ScheduleResult
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
class PerformanceModifyCommandService
internal constructor(
    private val performanceRepository: PerformanceRepository,
    private val memberRepository: MemberRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val scheduleSynchronizer: ScheduleSynchronizer,
    private val contentOwnershipReadPort: PerformanceContentOwnershipReader,
    private val performanceImageStorage: PerformanceImageStorage,
) {

    @Transactional
    fun modifyPerformance(
        memberId: Long,
        command: PerformanceModifyCommand,
    ): PerformanceMutationResult {
        return translateDomainFailure {
            log.info {
                "Starting updatePerformance for memberId: ${memberId}, performanceId: ${command.performanceId}"
            }
            val member = findMember(memberId)
            var performance = findPerformance(command.performanceId)
            if (!performance.isOwnedBy(member.userId)) {
                throw FrontofficeApplicationException(
                    PerformanceApplicationErrorCode.NOT_PERFORMANCE_OWNER
                )
            }
            validateModificationRequest(command)
            val scheduleCommands = command.schedules
            val castCommands = command.casts
            val staffCommands = command.staffs
            val imageCommands = command.images

            val hasActiveBooking =
                scheduleSynchronizer.lockAndCheckActiveBookings(command.performanceId)
            performance =
                updatePerformance(performance, command, scheduleCommands, hasActiveBooking)
                    .replaceContent(
                        synchronizeCasts(performance, castCommands),
                        synchronizeStaffs(performance, staffCommands),
                        synchronizeImages(performance, imageCommands),
                    )
            performance = performanceRepository.save(performance)
            val schedules = scheduleSynchronizer.synchronize(scheduleCommands, performance)
            val casts =
                performance.casts.map { CastResult(it.id, it.castName, it.castRole, it.castPhoto) }
            val staffs =
                performance.staffs.map {
                    StaffResult(it.id, it.staffName, it.staffRole, it.staffPhoto)
                }
            val images =
                performance.images.map {
                    PerformanceImageResult(it.id, it.performanceImageUrl)
                }
            val result = toResult(performance, schedules, casts, staffs, images)
            eventPublisher.publishEvent(PerformancePosterChangedEvent(performance.posterImage))
            log.info {
                "Successfully completed updatePerformance for performanceId: ${command.performanceId}"
            }
            result
        }
    }

    private fun validateModificationRequest(command: PerformanceModifyCommand) {
        if (command.schedules.isEmpty()) {
            throw FrontofficeApplicationException(
                PerformanceApplicationErrorCode.SCHEDULE_LIST_NOT_FOUND
            )
        }
        if (
            hasDuplicateIds(command.schedules.map(ScheduleModifyCommand::scheduleId)) ||
                hasDuplicateIds(command.casts.map(CastModifyCommand::id)) ||
                hasDuplicateIds(command.staffs.map(StaffModifyCommand::id)) ||
                hasDuplicateIds(command.images.map(PerformanceImageModifyCommand::id))
        ) {
            throw FrontofficeApplicationException(
                PerformanceApplicationErrorCode.DUPLICATE_MODIFICATION_ID
            )
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
            throw FrontofficeApplicationException(
                PerformanceApplicationErrorCode.SCHEDULE_LIST_NOT_FOUND
            )
        }
        var updated =
            current.update(
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
                scheduleCommands.size,
            )
        return updated.updateTicketPrice(command.ticketPrice, hasActiveBooking)
    }

    private fun synchronizeCasts(
        performance: Performance,
        commands: List<CastModifyCommand>,
    ): List<Cast> {
        val existing = performance.casts.associateBy { it.id }
        return commands.map { command ->
            val castId = command.id
            if (castId == null) {
                Cast.create(
                    command.name,
                    command.role,
                    validateStoredPerformanceImage(
                        performanceImageStorage,
                        command.photo,
                        "cast",
                        required = false,
                    ),
                )
            } else {
                val cast = existing[castId] ?: throwInvalidCast(castId)
                cast.update(
                    command.name,
                    command.role,
                    validateStoredPerformanceImage(
                        performanceImageStorage,
                        command.photo,
                        "cast",
                        required = false,
                    ),
                )
            }
        }
    }

    private fun synchronizeStaffs(
        performance: Performance,
        commands: List<StaffModifyCommand>,
    ): List<Staff> {
        val existing = performance.staffs.associateBy { it.id }
        return commands.map { command ->
            val staffId = command.id
            if (staffId == null) {
                Staff.create(
                    command.name,
                    command.role,
                    validateStoredPerformanceImage(
                        performanceImageStorage,
                        command.photo,
                        "staff",
                        required = false,
                    ),
                )
            } else {
                val staff = existing[staffId] ?: throwInvalidStaff(staffId)
                staff.update(
                    command.name,
                    command.role,
                    validateStoredPerformanceImage(
                        performanceImageStorage,
                        command.photo,
                        "staff",
                        required = false,
                    ),
                )
            }
        }
    }

    private fun synchronizeImages(
        performance: Performance,
        commands: List<PerformanceImageModifyCommand>,
    ): List<PerformanceImage> {
        val existing = performance.images.associateBy { it.id }
        return commands.map { command ->
            val imageId = command.id
            val imageKey =
                validateStoredPerformanceImage(
                    performanceImageStorage,
                    command.image,
                    "performance",
                )
            if (imageId == null) {
                PerformanceImage.create(imageKey)
            } else {
                val image = existing[imageId] ?: throwInvalidImage(imageId)
                image.update(imageKey)
            }
        }
    }

    private fun throwInvalidCast(castId: Long): Nothing {
        val error =
            if (contentOwnershipReadPort.findPerformanceIdByCastId(castId) == null) {
                CastApplicationErrorCode.CAST_NOT_FOUND
            } else {
                CastApplicationErrorCode.CAST_NOT_BELONG_TO_PERFORMANCE
            }
        throw FrontofficeApplicationException(error)
    }

    private fun throwInvalidStaff(staffId: Long): Nothing {
        val error =
            if (contentOwnershipReadPort.findPerformanceIdByStaffId(staffId) == null) {
                StaffApplicationErrorCode.STAFF_NOT_FOUND
            } else {
                StaffApplicationErrorCode.STAFF_NOT_BELONG_TO_PERFORMANCE
            }
        throw FrontofficeApplicationException(error)
    }

    private fun throwInvalidImage(imageId: Long): Nothing {
        val error =
            if (contentOwnershipReadPort.findPerformanceIdByImageId(imageId) == null) {
                PerformanceImageApplicationErrorCode.PERFORMANCE_IMAGE_NOT_FOUND
            } else {
                PerformanceImageApplicationErrorCode.PERFORMANCE_IMAGE_NOT_BELONG_TO_PERFORMANCE
            }
        throw FrontofficeApplicationException(error)
    }

    private fun toResult(
        performance: Performance,
        schedules: List<ScheduleResult>,
        casts: List<CastResult>,
        staffs: List<StaffResult>,
        images: List<PerformanceImageResult>,
    ): PerformanceMutationResult =
        PerformanceMutationResult(
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
            schedules = schedules,
            casts = casts,
            staffs = staffs,
            images = images,
        )

    private fun findMember(memberId: Long): Member =
        memberRepository.findById(memberId)
            ?: throw FrontofficeApplicationException(
                PerformanceApplicationErrorCode.MEMBER_NOT_FOUND
            )

    private fun findPerformance(performanceId: Long): Performance =
        performanceRepository.lockById(performanceId)
            ?: throw FrontofficeApplicationException(
                PerformanceApplicationErrorCode.PERFORMANCE_NOT_FOUND
            )

    private companion object {
        val log = KotlinLogging.logger {}
    }
}
