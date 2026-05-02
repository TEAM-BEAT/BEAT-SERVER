package com.beat.apis.booking.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.beat.apis.booking.application.dto.GuestBookingRetrieveRequest;
import com.beat.apis.booking.application.dto.GuestBookingRetrieveResponse;
import com.beat.apis.booking.application.dto.MemberBookingRetrieveResponse;
import com.beat.apis.schedule.application.dto.ScheduleNumberType;
import com.beat.domain.booking.repository.BookingRepository;
import com.beat.domain.booking.domain.Booking;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.member.domain.Member;
import com.beat.domain.member.domain.SocialType;
import com.beat.domain.member.repository.MemberRepository;
import com.beat.domain.performance.domain.BankName;
import com.beat.domain.performance.domain.Genre;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.performance.repository.PerformanceRepository;
import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.schedule.domain.ScheduleNumber;
import com.beat.domain.schedule.repository.ScheduleRepository;
import com.beat.domain.user.domain.Role;
import com.beat.domain.user.domain.Users;
import com.beat.domain.user.repository.UserRepository;
import com.beat.global.common.exception.NotFoundException;
import com.beat.apis.performance.application.exception.PerformanceApplicationErrorCode;
import com.beat.apis.schedule.application.exception.ScheduleApplicationErrorCode;

@ExtendWith(MockitoExtension.class)
class BookingRetrieveServiceBatchLookupTest {

	@Mock
	private BookingRepository bookingRepository;

	@Mock
	private PerformanceRepository performanceRepository;

	@Mock
	private ScheduleRepository scheduleRepository;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private UserRepository userRepository;

	private GuestBookingRetrieveService guestBookingRetrieveService;
	private MemberBookingRetrieveService memberBookingRetrieveService;

	@BeforeEach
	void setUp() {
		guestBookingRetrieveService = new GuestBookingRetrieveService(
			bookingRepository,
			performanceRepository,
			scheduleRepository
		);
		memberBookingRetrieveService = new MemberBookingRetrieveService(
			bookingRepository,
			memberRepository,
			userRepository,
			performanceRepository,
			scheduleRepository
		);
	}

	@Test
	void guestRetrieveUsesBatchScheduleAndPerformanceLookupWithoutChangingResponse() {
		Booking booking = booking(2, 10L, 1L);
		Schedule schedule = schedule(10L, 100L, ScheduleNumber.FIRST);
		Performance performance = performance(100L, 15_000);

		when(bookingRepository.findByBookerNameAndBookerPhoneNumberAndPasswordAndBirthDate(
			"홍길동", "010-1234-5678", "1234", "990101"
		)).thenReturn(Optional.of(List.of(booking)));
		when(scheduleRepository.findAllById(List.of(10L))).thenReturn(List.of(schedule));
		when(performanceRepository.findAllById(List.of(100L))).thenReturn(List.of(performance));

		List<GuestBookingRetrieveResponse> responses = guestBookingRetrieveService.findGuestBookings(
			new GuestBookingRetrieveRequest("홍길동", "990101", "010-1234-5678", "1234")
		);

		assertEquals(1, responses.size());
		GuestBookingRetrieveResponse response = responses.getFirst();
		assertEquals(10L, response.scheduleId());
		assertEquals(100L, response.performanceId());
		assertEquals(30_000, response.totalPaymentAmount());
		verify(scheduleRepository).findAllById(List.of(10L));
		verify(performanceRepository).findAllById(List.of(100L));
		verify(scheduleRepository, never()).findById(anyLong());
		verify(performanceRepository, never()).findById(anyLong());
	}

	@Test
	void memberRetrieveDeduplicatesBatchLookupInputsAndPreservesBookingOrder() {
		Member member = Member.rehydrate(1L, "member", "member@example.com", null, 7L, 123L, SocialType.KAKAO);
		Users user = Users.rehydrate(7L, Role.USER);
		Booking firstBooking = booking(1, 10L, 7L);
		Booking secondBooking = booking(3, 11L, 7L);
		Schedule firstSchedule = schedule(10L, 100L, ScheduleNumber.FIRST);
		Schedule secondSchedule = schedule(11L, 100L, ScheduleNumber.SECOND);
		Performance performance = performance(100L, 20_000);

		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(userRepository.findById(7L)).thenReturn(Optional.of(user));
		when(bookingRepository.findByUserId(7L)).thenReturn(List.of(firstBooking, secondBooking));
		when(scheduleRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(secondSchedule, firstSchedule));
		when(performanceRepository.findAllById(List.of(100L))).thenReturn(List.of(performance));

		List<MemberBookingRetrieveResponse> responses = memberBookingRetrieveService.findMemberBookings(1L);

		assertEquals(2, responses.size());
		assertEquals(10L, responses.get(0).scheduleId());
		assertEquals(ScheduleNumberType.FIRST, responses.get(0).scheduleNumber());
		assertEquals(20_000, responses.get(0).totalPaymentAmount());
		assertEquals(11L, responses.get(1).scheduleId());
		assertEquals(ScheduleNumberType.SECOND, responses.get(1).scheduleNumber());
		assertEquals(60_000, responses.get(1).totalPaymentAmount());
		verify(scheduleRepository).findAllById(List.of(10L, 11L));
		verify(performanceRepository).findAllById(List.of(100L));
		verify(scheduleRepository, never()).findById(anyLong());
		verify(performanceRepository, never()).findById(anyLong());
	}

	@Test
	void memberRetrieveReturnsEmptyResponseWithoutPerBookingLookupWhenBookingsAreEmpty() {
		Member member = Member.rehydrate(1L, "member", "member@example.com", null, 7L, 123L, SocialType.KAKAO);
		Users user = Users.rehydrate(7L, Role.USER);

		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(userRepository.findById(7L)).thenReturn(Optional.of(user));
		when(bookingRepository.findByUserId(7L)).thenReturn(List.of());
		when(scheduleRepository.findAllById(List.of())).thenReturn(List.of());
		when(performanceRepository.findAllById(List.of())).thenReturn(List.of());

		List<MemberBookingRetrieveResponse> responses = memberBookingRetrieveService.findMemberBookings(1L);

		assertEquals(List.of(), responses);
		verify(scheduleRepository).findAllById(List.of());
		verify(performanceRepository).findAllById(List.of());
		verify(scheduleRepository, never()).findById(anyLong());
		verify(performanceRepository, never()).findById(anyLong());
	}

	@Test
	void guestRetrieveThrowsSameScheduleNotFoundWhenBatchResultMissesBookingSchedule() {
		Booking booking = booking(2, 10L, 1L);

		when(bookingRepository.findByBookerNameAndBookerPhoneNumberAndPasswordAndBirthDate(
			"홍길동", "010-1234-5678", "1234", "990101"
		)).thenReturn(Optional.of(List.of(booking)));
		when(scheduleRepository.findAllById(List.of(10L))).thenReturn(List.of());
		when(performanceRepository.findAllById(List.of())).thenReturn(List.of());

		NotFoundException exception = assertThrows(NotFoundException.class, () ->
			guestBookingRetrieveService.findGuestBookings(
				new GuestBookingRetrieveRequest("홍길동", "990101", "010-1234-5678", "1234")
			)
		);

		assertEquals(ScheduleApplicationErrorCode.NO_SCHEDULE_FOUND, exception.getBaseErrorCode());
		verify(scheduleRepository).findAllById(List.of(10L));
		verify(performanceRepository).findAllById(List.of());
		verify(scheduleRepository, never()).findById(anyLong());
		verify(performanceRepository, never()).findById(anyLong());
	}

	@Test
	void guestRetrieveThrowsSamePerformanceNotFoundWhenBatchResultMissesSchedulePerformance() {
		Booking booking = booking(2, 10L, 1L);
		Schedule schedule = schedule(10L, 100L, ScheduleNumber.FIRST);

		when(bookingRepository.findByBookerNameAndBookerPhoneNumberAndPasswordAndBirthDate(
			"홍길동", "010-1234-5678", "1234", "990101"
		)).thenReturn(Optional.of(List.of(booking)));
		when(scheduleRepository.findAllById(List.of(10L))).thenReturn(List.of(schedule));
		when(performanceRepository.findAllById(List.of(100L))).thenReturn(List.of());

		NotFoundException exception = assertThrows(NotFoundException.class, () ->
			guestBookingRetrieveService.findGuestBookings(
				new GuestBookingRetrieveRequest("홍길동", "990101", "010-1234-5678", "1234")
			)
		);

		assertEquals(PerformanceApplicationErrorCode.PERFORMANCE_NOT_FOUND, exception.getBaseErrorCode());
		verify(scheduleRepository).findAllById(List.of(10L));
		verify(performanceRepository).findAllById(List.of(100L));
		verify(scheduleRepository, never()).findById(anyLong());
		verify(performanceRepository, never()).findById(anyLong());
	}

	private Booking booking(int purchaseTicketCount, Long scheduleId, Long userId) {
		return Booking.create(
			purchaseTicketCount,
			"홍길동",
			"010-1234-5678",
			BookingStatus.CHECKING_PAYMENT,
			"990101",
			"1234",
			BankName.NONE,
			"",
			"",
			scheduleId,
			userId
		);
	}

	private Schedule schedule(Long id, Long performanceId, ScheduleNumber scheduleNumber) {
		return Schedule.rehydrate(
			id,
			LocalDateTime.now().plusDays(7),
			100,
			0,
			true,
			scheduleNumber,
			performanceId
		);
	}

	private Performance performance(Long id, int ticketPrice) {
		return Performance.rehydrate(
			id,
			"공연",
			Genre.PLAY,
			120,
			"설명",
			"주의",
			BankName.NONE,
			"계좌",
			"예금주",
			"poster.png",
			"팀",
			"공연장",
			"도로명",
			"상세",
			"37.0",
			"127.0",
			"010-0000-0000",
			"2026.01.01",
			ticketPrice,
			2,
			7L
		);
	}
}
