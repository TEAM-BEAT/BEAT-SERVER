package com.beat.apis.booking.application;

import com.beat.apis.booking.application.query.GuestBookingQueryService;
import com.beat.apis.booking.application.query.MemberBookingQueryService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.beat.apis.booking.application.result.BookingRetrieveResult;
import com.beat.apis.schedule.api.type.ScheduleNumberType;
import com.beat.domain.booking.repository.BookingRepository;
import com.beat.domain.booking.model.Booking;
import com.beat.domain.member.model.Member;
import com.beat.domain.member.vo.SocialIdentity;
import com.beat.domain.member.model.SocialType;
import com.beat.domain.member.repository.MemberRepository;
import com.beat.domain.sharedkernel.vo.BankName;
import com.beat.domain.performance.model.Genre;
import com.beat.contracts.performance.PerformanceSummaryReadPort;
import com.beat.contracts.performance.readmodel.PerformanceSummaryReadModel;
import com.beat.domain.performance.vo.PaymentAccount;
import com.beat.domain.performance.vo.PerformancePeriod;
import com.beat.domain.performance.vo.RunningTime;
import com.beat.domain.performance.vo.TicketPrice;
import com.beat.domain.schedule.model.Schedule;
import com.beat.domain.schedule.model.ScheduleNumber;
import com.beat.domain.schedule.repository.ScheduleRepository;
import com.beat.apis.exception.ApiApplicationException;
import com.beat.apis.performance.exception.PerformanceApplicationErrorCode;
import com.beat.apis.schedule.exception.ScheduleApplicationErrorCode;

@ExtendWith(MockitoExtension.class)
class BookingRetrieveServiceBatchLookupTest {

	@Mock
	private BookingRepository bookingRepository;

	@Mock
	private PerformanceSummaryReadPort performanceSummaryReadPort;

	@Mock
	private ScheduleRepository scheduleRepository;

	@Mock
	private MemberRepository memberRepository;

	private GuestBookingQueryService guestBookingRetrieveService;
	private MemberBookingQueryService memberBookingRetrieveService;

	@BeforeEach
	void setUp() {
		guestBookingRetrieveService = new GuestBookingQueryService(
			bookingRepository,
			performanceSummaryReadPort,
			scheduleRepository,
			Clock.systemUTC()
		);
		memberBookingRetrieveService = new MemberBookingQueryService(
			bookingRepository,
			memberRepository,
			performanceSummaryReadPort,
			scheduleRepository,
			Clock.systemUTC()
		);
	}

	@Test
	void guestRetrieveUsesBatchScheduleAndPerformanceLookupWithoutChangingResponse() {
		Booking booking = booking(2, 10L, 1L, 24_000);
		Schedule schedule = schedule(10L, 100L, ScheduleNumber.FIRST);
		PerformanceSummaryReadModel performance = performance(100L, 15_000);

		when(bookingRepository.findByUserId(1L)).thenReturn(List.of(booking));
		when(scheduleRepository.findAllById(List.of(10L))).thenReturn(List.of(schedule));
		when(performanceSummaryReadPort.findAllByIds(List.of(100L))).thenReturn(List.of(performance));

		List<BookingRetrieveResult> responses = guestBookingRetrieveService.findGuestBookings(1L);

		assertEquals(1, responses.size());
		BookingRetrieveResult response = responses.getFirst();
		assertEquals(10L, response.getScheduleId());
		assertEquals(100L, response.getPerformanceId());
		assertEquals(24_000, response.getTotalPaymentAmount());
		verify(scheduleRepository).findAllById(List.of(10L));
		verify(performanceSummaryReadPort).findAllByIds(List.of(100L));
		verify(scheduleRepository, never()).findById(anyLong());
		verify(performanceSummaryReadPort, never()).findById(anyLong());
	}

	@Test
	void memberRetrieveDeduplicatesBatchLookupInputsAndPreservesBookingOrder() {
		Member member = Member.rehydrate(1L, "member", "member@example.com", null, 7L, SocialIdentity.of(SocialType.KAKAO, 123L));
		Booking firstBooking = booking(1, 10L, 7L);
		Booking secondBooking = booking(3, 11L, 7L);
		Schedule firstSchedule = schedule(10L, 100L, ScheduleNumber.FIRST);
		Schedule secondSchedule = schedule(11L, 100L, ScheduleNumber.SECOND);
		PerformanceSummaryReadModel performance = performance(100L, 20_000);

		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(bookingRepository.findByUserId(7L)).thenReturn(List.of(firstBooking, secondBooking));
		when(scheduleRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(secondSchedule, firstSchedule));
		when(performanceSummaryReadPort.findAllByIds(List.of(100L))).thenReturn(List.of(performance));

		List<BookingRetrieveResult> responses = memberBookingRetrieveService.findMemberBookings(1L);

		assertEquals(2, responses.size());
		assertEquals(10L, responses.get(0).getScheduleId());
		assertEquals(ScheduleNumberType.FIRST.name(), responses.get(0).getScheduleNumber());
		assertEquals(20_000, responses.get(0).getTotalPaymentAmount());
		assertEquals(11L, responses.get(1).getScheduleId());
		assertEquals(ScheduleNumberType.SECOND.name(), responses.get(1).getScheduleNumber());
		assertEquals(60_000, responses.get(1).getTotalPaymentAmount());
		verify(scheduleRepository).findAllById(List.of(10L, 11L));
		verify(performanceSummaryReadPort).findAllByIds(List.of(100L));
		verify(scheduleRepository, never()).findById(anyLong());
		verify(performanceSummaryReadPort, never()).findById(anyLong());
	}

	@Test
	void memberRetrieveReturnsEmptyResponseWithoutPerBookingLookupWhenBookingsAreEmpty() {
		Member member = Member.rehydrate(1L, "member", "member@example.com", null, 7L, SocialIdentity.of(SocialType.KAKAO, 123L));

		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(bookingRepository.findByUserId(7L)).thenReturn(List.of());
		when(scheduleRepository.findAllById(List.of())).thenReturn(List.of());
		when(performanceSummaryReadPort.findAllByIds(List.of())).thenReturn(List.of());

		List<BookingRetrieveResult> responses = memberBookingRetrieveService.findMemberBookings(1L);

		assertEquals(List.of(), responses);
		verify(scheduleRepository).findAllById(List.of());
		verify(performanceSummaryReadPort).findAllByIds(List.of());
		verify(scheduleRepository, never()).findById(anyLong());
		verify(performanceSummaryReadPort, never()).findById(anyLong());
	}

	@Test
	void guestRetrieveThrowsSameScheduleNotFoundWhenBatchResultMissesBookingSchedule() {
		Booking booking = booking(2, 10L, 1L);

		when(bookingRepository.findByUserId(1L)).thenReturn(List.of(booking));
		when(scheduleRepository.findAllById(List.of(10L))).thenReturn(List.of());
		when(performanceSummaryReadPort.findAllByIds(List.of())).thenReturn(List.of());

		ApiApplicationException exception = assertThrows(ApiApplicationException.class, () ->
				guestBookingRetrieveService.findGuestBookings(1L));

		assertEquals(ScheduleApplicationErrorCode.NO_SCHEDULE_FOUND, exception.getErrorCode());
		verify(scheduleRepository).findAllById(List.of(10L));
		verify(performanceSummaryReadPort).findAllByIds(List.of());
		verify(scheduleRepository, never()).findById(anyLong());
		verify(performanceSummaryReadPort, never()).findById(anyLong());
	}

	@Test
	void guestRetrieveThrowsSamePerformanceNotFoundWhenBatchResultMissesSchedulePerformance() {
		Booking booking = booking(2, 10L, 1L);
		Schedule schedule = schedule(10L, 100L, ScheduleNumber.FIRST);

		when(bookingRepository.findByUserId(1L)).thenReturn(List.of(booking));
		when(scheduleRepository.findAllById(List.of(10L))).thenReturn(List.of(schedule));
		when(performanceSummaryReadPort.findAllByIds(List.of(100L))).thenReturn(List.of());

		ApiApplicationException exception = assertThrows(ApiApplicationException.class, () ->
				guestBookingRetrieveService.findGuestBookings(1L));

		assertEquals(PerformanceApplicationErrorCode.PERFORMANCE_NOT_FOUND, exception.getErrorCode());
		verify(scheduleRepository).findAllById(List.of(10L));
		verify(performanceSummaryReadPort).findAllByIds(List.of(100L));
		verify(scheduleRepository, never()).findById(anyLong());
		verify(performanceSummaryReadPort, never()).findById(anyLong());
	}

	private Booking booking(int purchaseTicketCount, Long scheduleId, Long userId) {
		return booking(purchaseTicketCount, scheduleId, userId, null);
	}

	private Booking booking(int purchaseTicketCount, Long scheduleId, Long userId, Integer totalPaymentAmount) {
		return Booking.create(
			purchaseTicketCount,
			"홍길동",
			"010-1234-5678",
			"990101",
			"1234",
			scheduleId,
			userId,
			LocalDateTime.of(2026, 1, 1, 12, 0),
			totalPaymentAmount
		);
	}

	private Schedule schedule(Long id, Long performanceId, ScheduleNumber scheduleNumber) {
		LocalDateTime performanceDate = LocalDateTime.now().plusDays(7);
		return Schedule.rehydrate(
			id,
			performanceDate,
			performanceDate.plusHours(2),
			100,
			0,
			scheduleNumber,
			performanceId
		);
	}

	private PerformanceSummaryReadModel performance(Long id, int ticketPrice) {
		return new PerformanceSummaryReadModel(
			id,
			7L,
			"공연",
			"PLAY",
			ticketPrice,
			"KAKAOBANK",
			"계좌",
			"예금주",
			"poster.png",
			"팀",
			"공연장",
			"010-0000-0000",
			2,
			LocalDate.of(2026, 1, 1),
			LocalDate.of(2026, 1, 1)
		);
	}
}
