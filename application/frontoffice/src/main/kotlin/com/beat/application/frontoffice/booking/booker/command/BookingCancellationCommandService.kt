package com.beat.application.frontoffice.booking.booker.command

import com.beat.application.frontoffice.booking.booker.BookingApplicationErrorCode
import com.beat.application.frontoffice.booking.booker.result.BookingCancelResult
import com.beat.application.frontoffice.booking.booker.result.BookingRefundResult
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.exception.translateDomainFailure
import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.repository.BookingRepository
import com.beat.domain.booking.vo.RefundAccount
import com.beat.domain.schedule.repository.ScheduleRepository
import com.beat.domain.sharedkernel.vo.BankName
import java.time.Clock
import java.time.LocalDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BookingCancellationCommandService
internal constructor(
    private val bookingRepository: BookingRepository,
    private val clock: Clock,
    private val scheduleRepository: ScheduleRepository,
    private val guestBookingSessionManager: GuestBookingSessionManager,
) {
    @Transactional
    fun refundBooking(
        actor: BookingActorCommand,
        command: BookingRefundCommand,
    ): BookingRefundResult {
        return translateDomainFailure {
            val actorUserId = guestBookingSessionManager.resolveActorUserId(actor)
            var booking =
                bookingRepository.lockById(command.bookingId)
                    ?: throw FrontofficeApplicationException(
                        BookingApplicationErrorCode.NO_BOOKING_FOUND
                    )
            ensureOwnedBy(booking, actorUserId)
            val refundAccount =
                RefundAccount.of(
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
    fun cancelBooking(
        actor: BookingActorCommand,
        command: BookingCancelCommand,
    ): BookingCancelResult {
        return translateDomainFailure {
            val actorUserId = guestBookingSessionManager.resolveActorUserId(actor)
            val snapshot =
                bookingRepository.findById(command.bookingId)
                    ?: throw FrontofficeApplicationException(
                        BookingApplicationErrorCode.NO_BOOKING_FOUND
                    )
            ensureOwnedBy(snapshot, actorUserId)
            val lockedSchedule =
                if (snapshot.hasActiveTicketAllocation()) {
                    scheduleRepository.lockById(snapshot.scheduleId)
                        ?: throw FrontofficeApplicationException(
                            BookingApplicationErrorCode.SCHEDULE_NOT_FOUND
                        )
                } else {
                    null
                }
            var booking =
                bookingRepository.lockById(command.bookingId)
                    ?: throw FrontofficeApplicationException(
                        BookingApplicationErrorCode.NO_BOOKING_FOUND
                    )
            ensureOwnedBy(booking, actorUserId)
            if (lockedSchedule != null && booking.scheduleId != lockedSchedule.id) {
                throw FrontofficeApplicationException(
                    BookingApplicationErrorCode.SCHEDULE_NOT_FOUND
                )
            }

            val shouldReleaseTickets = booking.hasActiveTicketAllocation()
            booking = bookingRepository.save(booking.cancelUnpaidOrFree(LocalDateTime.now(clock)))
            if (shouldReleaseTickets) {
                val schedule =
                    lockedSchedule
                        ?: throw FrontofficeApplicationException(
                            BookingApplicationErrorCode.SCHEDULE_NOT_FOUND
                        )
                scheduleRepository.save(schedule.releaseTickets(booking.purchaseTicketCount))
            }
            BookingCancelResult(
                requireNotNull(booking.id),
                booking.bookingStatus.name,
            )
        }
    }

    private fun ensureOwnedBy(booking: Booking, actorUserId: Long) {
        if (!booking.isOwnedBy(actorUserId)) {
            throw FrontofficeApplicationException(BookingApplicationErrorCode.NO_BOOKING_FOUND)
        }
    }
}
