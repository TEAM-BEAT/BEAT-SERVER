package com.beat.application.frontoffice.booking.booker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.beat.application.frontoffice.booking.booker.query.BookerBookingPerformanceReadModel;
import com.beat.application.frontoffice.booking.booker.query.BookerBookingReadModel;
import com.beat.application.frontoffice.booking.booker.query.BookerBookingReader;
import com.beat.application.frontoffice.booking.booker.query.BookerBookingScheduleReadModel;
import com.beat.application.frontoffice.booking.booker.query.GuestBookingQueryService;
import com.beat.application.frontoffice.booking.booker.query.MemberBookingQueryService;
import com.beat.application.frontoffice.exception.FrontofficeApplicationException;
import com.beat.domain.member.model.Member;
import com.beat.domain.member.model.SocialType;
import com.beat.domain.member.repository.MemberRepository;
import com.beat.domain.member.vo.SocialIdentity;

@ExtendWith(MockitoExtension.class)
class BookerBookingQueryServiceTest {

	@Mock
	private BookerBookingReader bookerBookingReader;

	@Mock
	private MemberRepository memberRepository;

	private GuestBookingQueryService guestService;
	private MemberBookingQueryService memberService;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
		guestService = new GuestBookingQueryService(bookerBookingReader, clock);
		memberService = new MemberBookingQueryService(memberRepository, bookerBookingReader, clock);
	}

	@Test
	void guestQueryPreservesReaderOrderAndStoredPaymentAmount() {
		when(bookerBookingReader.findByUserId(7L)).thenReturn(List.of(
			booking(1L, 10L, 24_000, schedule(10L, 100L, "FIRST"), performance(100L, 15_000)),
			booking(2L, 11L, null, schedule(11L, 100L, "SECOND"), performance(100L, 20_000))
		));

		var results = guestService.findGuestBookings(7L);

		assertEquals(List.of(10L, 11L), results.stream().map(it -> it.getScheduleId()).toList());
		assertEquals(24_000, results.get(0).getTotalPaymentAmount());
		assertEquals(40_000, results.get(1).getTotalPaymentAmount());
		assertEquals(9, results.get(0).getDueDate());
	}

	@Test
	void memberQueryResolvesMemberToAuthoritativeUserIdentity() {
		Member member = Member.rehydrate(
			1L, "member", "member@example.com", null, 7L,
			SocialIdentity.of(SocialType.KAKAO, 123L));
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(bookerBookingReader.findByUserId(7L)).thenReturn(List.of(
			booking(1L, 10L, 20_000, schedule(10L, 100L, "FIRST"), performance(100L, 20_000))));

		var result = memberService.findMemberBookings(1L).getFirst();

		assertEquals(7L, result.getUserId());
		assertEquals(10L, result.getScheduleId());
	}

	@Test
	void queryReportsMissingScheduleWithStableApplicationError() {
		when(bookerBookingReader.findByUserId(7L)).thenReturn(List.of(booking(1L, 10L, null, null, null)));

		FrontofficeApplicationException exception = assertThrows(
			FrontofficeApplicationException.class,
			() -> guestService.findGuestBookings(7L));

		assertEquals(BookingApplicationErrorCode.SCHEDULE_NOT_FOUND, exception.getErrorCode());
		assertEquals("SCHEDULE_NOT_FOUND", exception.getErrorCode().getCode());
		assertEquals("해당 회차를 찾을 수 없습니다.", exception.getErrorCode().getMessage());
	}

	@Test
	void queryReportsMissingPerformanceWithStableApplicationError() {
		when(bookerBookingReader.findByUserId(7L)).thenReturn(List.of(
			booking(1L, 10L, null, schedule(10L, 100L, "FIRST"), null)));

		FrontofficeApplicationException exception = assertThrows(
			FrontofficeApplicationException.class,
			() -> guestService.findGuestBookings(7L));

		assertEquals(BookingApplicationErrorCode.PERFORMANCE_NOT_FOUND, exception.getErrorCode());
		assertEquals("PERFORMANCE_NOT_FOUND", exception.getErrorCode().getCode());
		assertEquals("해당 공연 정보를 찾을 수 없습니다.", exception.getErrorCode().getMessage());
	}

	private BookerBookingReadModel booking(
		Long bookingId,
		Long scheduleId,
		Integer amount,
		BookerBookingScheduleReadModel schedule,
		BookerBookingPerformanceReadModel performance
	) {
		return new BookerBookingReadModel(
			7L, bookingId, 2, "booker", "CHECKING_PAYMENT",
			LocalDateTime.of(2026, 1, 1, 12, 0), amount, schedule, performance);
	}

	private BookerBookingScheduleReadModel schedule(Long scheduleId, Long performanceId, String number) {
		return new BookerBookingScheduleReadModel(
			scheduleId, performanceId, LocalDateTime.of(2026, 1, 10, 18, 0), number);
	}

	private BookerBookingPerformanceReadModel performance(Long performanceId, int ticketPrice) {
		return new BookerBookingPerformanceReadModel(
			performanceId, "공연", "공연장", "010-0000-0000", "KAKAOBANK", "계좌", "예금주",
			"poster.png", ticketPrice);
	}
}
