package com.beat.application.frontoffice.booking.booker.command

import com.beat.application.frontoffice.booking.booker.result.BookingCancelResult
import com.beat.application.frontoffice.booking.booker.result.BookingRefundResult
import com.beat.application.frontoffice.booking.booker.BookingApplicationErrorCode
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.exception.translateDomainFailure
import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.repository.BookingRepository
import com.beat.domain.booking.vo.RefundAccount
import com.beat.domain.sharedkernel.vo.BankName
import com.beat.domain.schedule.repository.ScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.Clock

@Service
class BookingCancellationCommandService(
    private val bookingRepository: BookingRepository,
    private val clock: Clock,
    private val scheduleRepository: ScheduleRepository,
) {
    @Transactional
    fun refundBooking(actorUserId: Long, command: BookingRefundCommand): BookingRefundResult {
        return translateDomainFailure {
            var booking = bookingRepository.lockById(command.bookingId)
                ?: throw FrontofficeApplicationException(BookingApplicationErrorCode.NO_BOOKING_FOUND)
            ensureOwnedBy(booking, actorUserId)
            val refundAccount = RefundAccount.of(
                command.bankName?.let(BankName::valueOf),
                command.accountNumber,
                command.accountHolder,
            )
            booking = bookingRepository.save(booking.requestRefund(refundAccount))
            BookingRefundResult(
                requireNotNull(booking.id),
                booking.bookingStatus.name,
                booking.bankName?.name,
                booking.accountNumber,
                booking.accountHolder,
            )
        }
    }

    @Transactional
    fun cancelBooking(actorUserId: Long, command: BookingCancelCommand): BookingCancelResult {
        return translateDomainFailure {
            val snapshot = bookingRepository.findById(command.bookingId)
                ?: throw FrontofficeApplicationException(BookingApplicationErrorCode.NO_BOOKING_FOUND)
            ensureOwnedBy(snapshot, actorUserId)
            val lockedSchedule = if (snapshot.hasActiveTicketAllocation()) {
                scheduleRepository.lockById(snapshot.scheduleId)
                    ?: throw FrontofficeApplicationException(BookingApplicationErrorCode.SCHEDULE_NOT_FOUND)
            } else {
                null
            }
            var booking = bookingRepository.lockById(command.bookingId)
                ?: throw FrontofficeApplicationException(BookingApplicationErrorCode.NO_BOOKING_FOUND)
            ensureOwnedBy(booking, actorUserId)
            if (lockedSchedule != null && booking.scheduleId != lockedSchedule.id) {
                throw FrontofficeApplicationException(BookingApplicationErrorCode.SCHEDULE_NOT_FOUND)
            }

            val shouldReleaseTickets = booking.hasActiveTicketAllocation()
            booking = bookingRepository.save(booking.cancelUnpaidOrFree(LocalDateTime.now(clock)))
            if (shouldReleaseTickets) {
                val schedule = lockedSchedule
                    ?: throw FrontofficeApplicationException(BookingApplicationErrorCode.SCHEDULE_NOT_FOUND)
                scheduleRepository.save(schedule.releaseTickets(booking.purchaseTicketCount))
            }
            BookingCancelResult(
                requireNotNull(booking.id),
                booking.bookingStatus.name,
            )
        }
    }

    private fun ensureOwnedBy(booking: Booking, actorUserId: Long) {
        if (booking.userId != actorUserId) {
            throw FrontofficeApplicationException(BookingApplicationErrorCode.NO_BOOKING_FOUND)
        }
    }
}
