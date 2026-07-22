package com.beat.apis.booking.application.command

import com.beat.apis.booking.application.result.BookingCancelResult
import com.beat.apis.booking.application.result.BookingRefundResult
import com.beat.apis.booking.exception.BookingApplicationErrorCode
import com.beat.apis.exception.ApiApplicationException
import com.beat.apis.schedule.exception.ScheduleApplicationErrorCode
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
        var booking = bookingRepository.lockById(command.bookingId)
            .orElseThrow { ApiApplicationException(BookingApplicationErrorCode.NO_BOOKING_FOUND) }
        ensureOwnedBy(booking, actorUserId)
        val refundAccount = RefundAccount.of(
            command.bankName?.let(BankName::valueOf),
            command.accountNumber,
            command.accountHolder,
        )
        booking = bookingRepository.save(booking.requestRefund(refundAccount))
        return BookingRefundResult(
            requireNotNull(booking.getId()),
            booking.getBookingStatus().name,
            booking.getBankName()?.name,
            booking.getAccountNumber(),
            booking.getAccountHolder(),
        )
    }

    @Transactional
    fun cancelBooking(actorUserId: Long, command: BookingCancelCommand): BookingCancelResult {
        val snapshot = bookingRepository.findById(command.bookingId)
            .orElseThrow { ApiApplicationException(BookingApplicationErrorCode.NO_BOOKING_FOUND) }
        ensureOwnedBy(snapshot, actorUserId)
        val lockedSchedule = if (snapshot.hasActiveTicketAllocation()) {
            scheduleRepository.lockById(snapshot.getScheduleId())
                .orElseThrow { ApiApplicationException(ScheduleApplicationErrorCode.NO_SCHEDULE_FOUND) }
        } else {
            null
        }
        var booking = bookingRepository.lockById(command.bookingId)
            .orElseThrow { ApiApplicationException(BookingApplicationErrorCode.NO_BOOKING_FOUND) }
        ensureOwnedBy(booking, actorUserId)
        if (lockedSchedule != null && booking.getScheduleId() != lockedSchedule.getId()) {
            throw ApiApplicationException(ScheduleApplicationErrorCode.NO_SCHEDULE_FOUND)
        }

        val shouldReleaseTickets = booking.hasActiveTicketAllocation()
        booking = bookingRepository.save(booking.cancel(LocalDateTime.now(clock)))
        if (shouldReleaseTickets) {
            val schedule = lockedSchedule
                ?: throw ApiApplicationException(ScheduleApplicationErrorCode.NO_SCHEDULE_FOUND)
            scheduleRepository.save(schedule.releaseTickets(booking.getPurchaseTicketCount()))
        }
        return BookingCancelResult(
            requireNotNull(booking.getId()),
            booking.getBookingStatus().name,
        )
    }

    private fun ensureOwnedBy(booking: Booking, actorUserId: Long) {
        if (booking.getUserId() != actorUserId) {
            throw ApiApplicationException(BookingApplicationErrorCode.NO_BOOKING_FOUND)
        }
    }
}
