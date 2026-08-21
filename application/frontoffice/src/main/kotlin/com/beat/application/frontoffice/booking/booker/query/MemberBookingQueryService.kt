package com.beat.application.frontoffice.booking.booker.query

import com.beat.application.frontoffice.booking.booker.BookingApplicationErrorCode
import com.beat.application.frontoffice.booking.booker.result.BookingRetrieveResult
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.domain.member.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class MemberBookingQueryService(
    private val memberRepository: MemberRepository,
    private val bookerBookingReader: BookerBookingReader,
    private val clock: Clock,
) {
    fun findMemberBookings(memberId: Long): List<BookingRetrieveResult> {
        val member = memberRepository.findById(memberId)
            .orElseThrow { FrontofficeApplicationException(BookingApplicationErrorCode.MEMBER_NOT_FOUND) }
        val today = LocalDate.now(clock)
        return bookerBookingReader.findByUserId(member.getUserId()).map { it.toResult(today) }
    }
}
