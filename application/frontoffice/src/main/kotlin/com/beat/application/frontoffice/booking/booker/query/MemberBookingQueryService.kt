package com.beat.application.frontoffice.booking.booker.query

import com.beat.application.frontoffice.booking.booker.exception.BookingApplicationErrorCode
import com.beat.application.frontoffice.booking.booker.query.result.BookingRetrieveResult
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.exception.translateDomainFailure
import com.beat.domain.member.repository.MemberRepository
import java.time.Clock
import java.time.LocalDate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberBookingQueryService
internal constructor(
    private val memberRepository: MemberRepository,
    private val memberBookingHistoryReader: MemberBookingHistoryReader,
    private val clock: Clock,
) {
    fun findMemberBookings(memberId: Long): List<BookingRetrieveResult> {
        return translateDomainFailure {
            val member =
                memberRepository.findById(memberId)
                    ?: throw FrontofficeApplicationException(
                        BookingApplicationErrorCode.MEMBER_NOT_FOUND
                    )
            val today = LocalDate.now(clock)
            memberBookingHistoryReader.findByUserId(member.userId).map { it.toResult(today) }
        }
    }
}
