package com.beat.apis.booking.application.service.command

import com.beat.apis.booking.application.dto.TicketDeleteRequest
import com.beat.apis.booking.application.dto.TicketRefundRequest
import com.beat.apis.booking.application.dto.TicketUpdateRequest
import com.beat.contracts.sms.SmsMessage
import com.beat.contracts.sms.SmsPort
import com.beat.domain.booking.dao.TicketRepository
import com.beat.domain.booking.domain.Booking
import com.beat.domain.booking.domain.BookingStatus
import com.beat.domain.booking.exception.BookingErrorCode
import com.beat.domain.booking.exception.TicketErrorCode
import com.beat.domain.member.dao.MemberRepository
import com.beat.domain.member.domain.Member
import com.beat.domain.member.exception.MemberErrorCode
import com.beat.domain.performance.dao.PerformanceRepository
import com.beat.domain.performance.domain.Performance
import com.beat.domain.performance.exception.PerformanceErrorCode
import com.beat.domain.schedule.dao.ScheduleRepository
import com.beat.domain.user.dao.UserRepository
import com.beat.domain.user.domain.Users
import com.beat.domain.user.exception.UserErrorCode
import com.beat.global.common.exception.BadRequestException
import com.beat.global.common.exception.ForbiddenException
import com.beat.global.common.exception.NotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class TicketCommandService(
    private val ticketRepository: TicketRepository,
    private val performanceRepository: PerformanceRepository,
    private val memberRepository: MemberRepository,
    private val userRepository: UserRepository,
    private val scheduleRepository: ScheduleRepository,
    private val smsPort: SmsPort,
) {

    fun updateTickets(memberId: Long, request: TicketUpdateRequest) {
        val member = findMember(memberId)
        val user = findUser(member)
        val performance = findPerformance(request.performanceId())
        performance.validatePerformanceOwnership(user.id)

        request.bookingList().forEach { detail ->
            val booking = findBooking(detail.bookingId())

            if (
                booking.bookingStatus == BookingStatus.BOOKING_CONFIRMED &&
                detail.bookingStatus() != BookingStatus.BOOKING_CONFIRMED
            ) {
                throw BadRequestException(TicketErrorCode.PAYMENT_COMPLETED_TICKET_UPDATE_NOT_ALLOWED)
            }

            if (
                booking.bookingStatus == BookingStatus.CHECKING_PAYMENT &&
                detail.bookingStatus() == BookingStatus.BOOKING_CONFIRMED
            ) {
                booking.updateBookingStatus(BookingStatus.BOOKING_CONFIRMED)
                ticketRepository.save(booking)

                sendBookingConfirmedSms(
                    bookerName = detail.bookerName(),
                    bookerPhoneNumber = detail.bookerPhoneNumber(),
                    performanceTitle = request.performanceTitle(),
                )
            }
        }
    }

    fun refundTicketsByBookingIds(memberId: Long, request: TicketRefundRequest) {
        val member = findMember(memberId)
        val user = findUser(member)
        val performance = findPerformance(request.performanceId())
        performance.validatePerformanceOwnership(user.id)

        request.bookingList().forEach { bookingRequest ->
            val booking = findBooking(bookingRequest.bookingId())

            booking.updateBookingStatus(BookingStatus.BOOKING_CANCELLED)
            ticketRepository.save(booking)

            decreaseSoldTicketCountAndReopenBookingIfNeeded(booking)
        }
    }

    fun deleteTicketsByBookingIds(memberId: Long, request: TicketDeleteRequest) {
        val member = findMember(memberId)
        val userId = findUser(member).id
        val performance = findPerformance(request.performanceId())
        performance.validatePerformanceOwnership(userId)

        if (performance.users.id != userId) {
            throw ForbiddenException(PerformanceErrorCode.NOT_PERFORMANCE_OWNER)
        }

        request.bookingList().forEach { bookingRequest ->
            val booking = findBooking(bookingRequest.bookingId())

            booking.updateBookingStatus(BookingStatus.BOOKING_DELETED)
            ticketRepository.save(booking)

            decreaseSoldTicketCountAndReopenBookingIfNeeded(booking)
        }
    }

    private fun sendBookingConfirmedSms(
        bookerName: String,
        bookerPhoneNumber: String,
        performanceTitle: String,
    ) {
        val message = "[BEAT] ${bookerName}님 $performanceTitle 예매 확정되었습니다."

        try {
            smsPort.sendSms(SmsMessage(bookerPhoneNumber, message))
        } catch (exception: RuntimeException) {
            log.error("SMS 전송 실패 - 예매자: {}, 공연: {}", bookerName, performanceTitle, exception)
        }
    }

    private fun decreaseSoldTicketCountAndReopenBookingIfNeeded(booking: Booking) {
        val schedule = booking.schedule
        schedule.decreaseSoldTicketCount(booking.purchaseTicketCount)

        if (!schedule.isBooking) {
            schedule.updateIsBooking(true)
            scheduleRepository.save(schedule)
        }
    }

    private fun findMember(memberId: Long): Member {
        return memberRepository.findById(memberId)
            .orElseThrow { NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND) }
    }

    private fun findUser(member: Member): Users {
        return userRepository.findById(member.user.id)
            .orElseThrow { NotFoundException(UserErrorCode.USER_NOT_FOUND) }
    }

    private fun findPerformance(performanceId: Long): Performance {
        return performanceRepository.findById(performanceId)
            .orElseThrow { NotFoundException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND) }
    }

    private fun findBooking(bookingId: Long): Booking {
        return ticketRepository.findById(bookingId)
            .orElseThrow { NotFoundException(BookingErrorCode.NO_BOOKING_FOUND) }
    }

    companion object {
        private val log = LoggerFactory.getLogger(TicketCommandService::class.java)
    }
}