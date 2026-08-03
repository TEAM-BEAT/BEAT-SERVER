package com.beat.apis.ticket.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.beat.apis.ticket.exception.TicketApplicationErrorCode;
import com.beat.apis.ticket.application.command.TicketBookingStatus;
import com.beat.apis.ticket.application.command.TicketStatusUpdate;
import com.beat.apis.ticket.application.command.TicketBookingIdsCommand;
import com.beat.apis.ticket.application.command.TicketUpdateCommand;
import com.beat.apis.ticket.application.event.TicketPaymentConfirmedEvent;
import com.beat.apis.schedule.exception.ScheduleApplicationErrorCode;
import com.beat.contracts.booking.MakerTicketReadPort;
import com.beat.contracts.booking.readmodel.MakerTicketBookingStatus;
import com.beat.contracts.booking.readmodel.MakerTicketListItemReadModel;
import com.beat.contracts.booking.readmodel.MakerTicketScheduleNumber;
import com.beat.apis.booking.api.type.BookingStatusType;
import com.beat.domain.booking.model.Booking;
import com.beat.domain.booking.model.BookingStatus;
import com.beat.domain.booking.exception.BookingErrorCode;
import com.beat.domain.exception.DomainException;
import com.beat.domain.booking.repository.BookingRepository;
import com.beat.domain.member.model.Member;
import com.beat.domain.member.vo.SocialIdentity;
import com.beat.domain.member.model.SocialType;
import com.beat.domain.member.repository.MemberRepository;
import com.beat.domain.sharedkernel.vo.BankName;
import com.beat.domain.performance.model.Genre;
import com.beat.contracts.performance.PerformanceSummaryReadPort;
import com.beat.contracts.performance.readmodel.PerformanceSummaryReadModel;
import com.beat.contracts.schedule.ScheduleReadPort;
import com.beat.contracts.schedule.readmodel.ScheduleSummaryReadModel;
import com.beat.domain.performance.vo.PaymentAccount;
import com.beat.domain.performance.vo.PerformancePeriod;
import com.beat.domain.performance.vo.RunningTime;
import com.beat.domain.performance.vo.TicketPrice;
import com.beat.apis.schedule.api.type.ScheduleNumberType;
import com.beat.domain.schedule.model.Schedule;
import com.beat.domain.schedule.model.ScheduleNumber;
import com.beat.domain.schedule.repository.ScheduleRepository;
import com.beat.apis.exception.ApiApplicationException;
import com.beat.apis.ticket.application.command.TicketCommandService;
import com.beat.apis.ticket.application.query.TicketQueryService;
import com.beat.apis.ticket.application.query.TicketListQuery;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

	@Mock
	private BookingRepository bookingRepository;

	@Mock
	private MakerTicketReadPort makerTicketReadPort;

	@Mock
	private PerformanceSummaryReadPort performanceSummaryReadPort;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private ScheduleRepository scheduleRepository;

	@Mock
	private ScheduleReadPort scheduleReadPort;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	private TicketQueryService ticketQueryService;
	private TicketCommandService ticketCommandService;

	@BeforeEach
	void setUp() {
		ticketQueryService = new TicketQueryService(
			makerTicketReadPort,
			performanceSummaryReadPort,
			memberRepository,
			scheduleReadPort
		);
		ticketCommandService = new TicketCommandService(
			bookingRepository,
			performanceSummaryReadPort,
			memberRepository,
			scheduleRepository,
			eventPublisher,
			Clock.systemUTC()
		);
	}

	@Test
	void findAllTicketsByConditionsUsesExplicitContractEnumsAndReturnsApiStatus() {
		Member member = Member.rehydrate(1L, "maker", null, null, 10L, SocialIdentity.of(SocialType.KAKAO, 123L));
		PerformanceSummaryReadModel performance = performance(100L, 10L);
		LocalDateTime performanceDate = LocalDateTime.of(2026, 1, 1, 19, 0);
		Schedule schedule = Schedule.rehydrate(
			200L,
			performanceDate,
			performanceDate.plusHours(2),
			100,
			1,
			ScheduleNumber.FIRST,
			100L
		);
		MakerTicketListItemReadModel ticket = new MakerTicketListItemReadModel(
			300L,
			"booker",
			"010-0000-0000",
			200L,
			1,
			LocalDateTime.of(2026, 1, 1, 12, 0),
			MakerTicketBookingStatus.CHECKING_PAYMENT,
			"카카오뱅크",
			"123",
			"holder"
		);

		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(performanceSummaryReadPort.findById(100L)).thenReturn(Optional.of(performance));
		when(scheduleReadPort.findAllByPerformanceId(100L)).thenReturn(List.of(
			new ScheduleSummaryReadModel(200L, performanceDate, 100, 99, "FIRST")
		));
		when(makerTicketReadPort.findTickets(
			100L,
			List.of(MakerTicketScheduleNumber.FIRST),
			List.of(MakerTicketBookingStatus.CHECKING_PAYMENT)
		)).thenReturn(List.of(ticket));

		var response = ticketQueryService.findAllTicketsByConditions(1L,
			100L,
			new TicketListQuery(null, List.of("FIRST"), List.of("CHECKING_PAYMENT")));

		assertEquals("CHECKING_PAYMENT", response.getBookingList().get(0).getBookingStatus());
		assertEquals("FIRST", response.getBookingList().get(0).getScheduleNumber());
		assertEquals(99, response.getTotalPerformanceSoldTicketCount());
		verify(makerTicketReadPort).findTickets(
			100L,
			List.of(MakerTicketScheduleNumber.FIRST),
			List.of(MakerTicketBookingStatus.CHECKING_PAYMENT)
		);
	}

	@Test
	void updateTicketsRejectsBookingFromAnotherPerformance() {
		Member member = Member.rehydrate(1L, "maker", null, null, 10L,
			SocialIdentity.of(SocialType.KAKAO, 123L));
		PerformanceSummaryReadModel performance = performance(100L, 10L);
		LocalDateTime performanceDate = LocalDateTime.of(2026, 1, 1, 19, 0);
		Schedule foreignSchedule = Schedule.rehydrate(
			200L, performanceDate, performanceDate.plusHours(2), 100, 1, ScheduleNumber.FIRST, 999L);
		Booking booking = Booking.rehydrate(
			300L, 1, "booker", "010-0000-0000", BookingStatus.CHECKING_PAYMENT,
			LocalDateTime.of(2026, 1, 1, 12, 0), null, null, null, null, 200L, 20L);
		TicketStatusUpdate detail = ticketDetail(BookingStatusType.BOOKING_CONFIRMED);

		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(performanceSummaryReadPort.findById(100L)).thenReturn(Optional.of(performance));
		when(bookingRepository.findAllById(List.of(300L))).thenReturn(List.of(booking));
		when(scheduleRepository.lockById(200L)).thenReturn(Optional.of(foreignSchedule));

		ApiApplicationException exception = assertThrows(ApiApplicationException.class, () ->
			ticketCommandService.updateTickets(1L, TicketUpdateCommand.of(100L, List.of(detail))));

		assertEquals(ScheduleApplicationErrorCode.SCHEDULE_NOT_BELONG_TO_PERFORMANCE,
			exception.getErrorCode());
		verifyNoInteractions(eventPublisher);
	}

	@Test
	void updateTicketsRejectsUnsupportedStatusTransition() {
		Booking booking = booking(BookingStatus.CHECKING_PAYMENT);
		stubOwnedTicketUpdate(booking);
		TicketStatusUpdate detail = ticketDetail(BookingStatusType.BOOKING_CANCELLED);

		DomainException exception = assertThrows(DomainException.class, () ->
			ticketCommandService.updateTickets(1L, TicketUpdateCommand.of(100L, List.of(detail))));

		assertEquals(BookingErrorCode.STATUS_TRANSITION_NOT_ALLOWED, exception.getErrorCode());
		verify(bookingRepository, never()).save(any());
		verifyNoInteractions(eventPublisher);
	}

	@Test
	void updateTicketsPublishesConfirmationAfterStateChange() {
		Booking booking = booking(BookingStatus.CHECKING_PAYMENT);
		stubOwnedTicketUpdate(booking);

		ticketCommandService.updateTickets(1L,
			TicketUpdateCommand.of(100L, List.of(ticketDetail(BookingStatusType.BOOKING_CONFIRMED))));

		InOrder inOrder = inOrder(bookingRepository, eventPublisher);
		inOrder.verify(bookingRepository).save(booking.confirmPayment());
		ArgumentCaptor<TicketPaymentConfirmedEvent> eventCaptor =
			ArgumentCaptor.forClass(TicketPaymentConfirmedEvent.class);
		inOrder.verify(eventPublisher).publishEvent(eventCaptor.capture());
		assertEquals(300L, eventCaptor.getValue().getBookingId());
		assertEquals("booker", eventCaptor.getValue().getBookerName());
		assertEquals("010-0000-0000", eventCaptor.getValue().getBookerPhoneNumber());
		assertEquals("title", eventCaptor.getValue().getPerformanceTitle());
	}

	@Test
	void updateTicketsRejectsDuplicateBookingIdsBeforeMutation() {
		TicketStatusUpdate detail = ticketDetail(BookingStatusType.BOOKING_CONFIRMED);

		ApiApplicationException exception = assertThrows(ApiApplicationException.class, () ->
			ticketCommandService.updateTickets(1L, TicketUpdateCommand.of(100L, List.of(detail, detail))));

		assertEquals(TicketApplicationErrorCode.DUPLICATE_BOOKING_ID, exception.getErrorCode());
		verifyNoDependencyInteractions();
	}

	@Test
	void refundCompletionReleasesInventoryForRefundRequestedBooking() {
		Booking booking = booking(BookingStatus.REFUND_REQUESTED);
		stubOwnedTicketUpdate(booking);
		when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ticketCommandService.refundTicketsByBookingIds(
			1L,
			TicketBookingIdsCommand.of(100L, List.of(300L))
		);

		ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
		ArgumentCaptor<Schedule> scheduleCaptor = ArgumentCaptor.forClass(Schedule.class);
		verify(bookingRepository).save(bookingCaptor.capture());
		verify(scheduleRepository).save(scheduleCaptor.capture());
		assertEquals(BookingStatus.BOOKING_CANCELLED, bookingCaptor.getValue().getBookingStatus());
		assertEquals(0, scheduleCaptor.getValue().getAllocatedTicketCount());
	}

	@Test
	void refundCompletionRejectsConfirmedBookingWithoutReleasingInventory() {
		Booking confirmed = booking(BookingStatus.BOOKING_CONFIRMED);
		stubOwnedTicketUpdate(confirmed);

		DomainException exception = assertThrows(DomainException.class, () ->
			ticketCommandService.refundTicketsByBookingIds(
				1L,
				TicketBookingIdsCommand.of(100L, List.of(300L))
			)
		);

		assertEquals(BookingErrorCode.REFUND_COMPLETION_NOT_ALLOWED, exception.getErrorCode());
		verify(bookingRepository, never()).save(any());
		verify(scheduleRepository, never()).save(any());
	}

	@Test
	void deletionCancelsCheckingPaymentBookingAndReleasesInventory() {
		Booking booking = booking(BookingStatus.CHECKING_PAYMENT);
		stubOwnedTicketUpdate(booking);
		when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ticketCommandService.deleteTicketsByBookingIds(
			1L,
			TicketBookingIdsCommand.of(100L, List.of(300L))
		);

		ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
		ArgumentCaptor<Schedule> scheduleCaptor = ArgumentCaptor.forClass(Schedule.class);
		verify(bookingRepository).save(bookingCaptor.capture());
		verify(scheduleRepository).save(scheduleCaptor.capture());
		assertEquals(BookingStatus.BOOKING_DELETED, bookingCaptor.getValue().getBookingStatus());
		assertEquals(0, scheduleCaptor.getValue().getAllocatedTicketCount());
	}

	@Test
	void deletionCancelsConfirmedFreeBookingAndReleasesInventory() {
		Booking booking = booking(BookingStatus.BOOKING_CONFIRMED, 0);
		stubOwnedTicketUpdate(booking);
		when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ticketCommandService.deleteTicketsByBookingIds(
			1L,
			TicketBookingIdsCommand.of(100L, List.of(300L))
		);

		ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
		ArgumentCaptor<Schedule> scheduleCaptor = ArgumentCaptor.forClass(Schedule.class);
		verify(bookingRepository).save(bookingCaptor.capture());
		verify(scheduleRepository).save(scheduleCaptor.capture());
		assertEquals(BookingStatus.BOOKING_DELETED, bookingCaptor.getValue().getBookingStatus());
		assertEquals(0, scheduleCaptor.getValue().getAllocatedTicketCount());
	}

	@Test
	void deletionDoesNotReleaseInventoryAgainForCancelledBooking() {
		Booking booking = booking(BookingStatus.BOOKING_CANCELLED);
		stubOwnedTicketUpdate(booking);
		when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ticketCommandService.deleteTicketsByBookingIds(
			1L,
			TicketBookingIdsCommand.of(100L, List.of(300L))
		);

		ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
		verify(bookingRepository).save(bookingCaptor.capture());
		verify(scheduleRepository, never()).save(any());
		assertEquals(BookingStatus.BOOKING_DELETED, bookingCaptor.getValue().getBookingStatus());
	}

	@Test
	void deletionIsIdempotentWithoutReleasingInventoryForDeletedBooking() {
		Booking booking = booking(BookingStatus.BOOKING_DELETED);
		stubOwnedTicketUpdate(booking);
		when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ticketCommandService.deleteTicketsByBookingIds(
			1L,
			TicketBookingIdsCommand.of(100L, List.of(300L))
		);

		verify(bookingRepository).save(booking);
		verify(scheduleRepository, never()).save(any());
	}

	@Test
	void deletionRejectsConfirmedBookingWithoutReleasingInventory() {
		Booking confirmed = booking(BookingStatus.BOOKING_CONFIRMED);
		stubOwnedTicketUpdate(confirmed);

		DomainException confirmedError = assertThrows(DomainException.class, () ->
			ticketCommandService.deleteTicketsByBookingIds(
				1L,
				TicketBookingIdsCommand.of(100L, List.of(300L))
			)
		);

		assertEquals(BookingErrorCode.DELETION_NOT_ALLOWED, confirmedError.getErrorCode());
		verify(bookingRepository, never()).save(any());
		verify(scheduleRepository, never()).save(any());
	}

	@Test
	void deletionRejectsRefundRequestedBookingWithoutReleasingInventory() {
		Booking refundRequested = booking(BookingStatus.REFUND_REQUESTED);
		stubOwnedTicketUpdate(refundRequested);

		DomainException refundError = assertThrows(DomainException.class, () ->
			ticketCommandService.deleteTicketsByBookingIds(
				1L,
				TicketBookingIdsCommand.of(100L, List.of(300L))
			)
		);

		assertEquals(BookingErrorCode.DELETION_NOT_ALLOWED, refundError.getErrorCode());
		verify(bookingRepository, never()).save(any());
		verify(scheduleRepository, never()).save(any());
	}

	@Test
	void searchAllTicketsByConditionsRejectsNullSearchWord() {
		ApiApplicationException exception = assertThrows(ApiApplicationException.class, () ->
			ticketQueryService.searchAllTicketsByConditions(1L, 100L, new TicketListQuery(null, List.of(), List.of())));

		assertEquals(TicketApplicationErrorCode.SEARCH_WORD_TOO_SHORT, exception.getErrorCode());
		verifyNoDependencyInteractions();
	}

	@Test
	void searchAllTicketsByConditionsRejectsBlankSearchWord() {
		ApiApplicationException exception = assertThrows(ApiApplicationException.class, () ->
			ticketQueryService.searchAllTicketsByConditions(1L,
				100L,
				new TicketListQuery("", List.of("FIRST"), List.of("CHECKING_PAYMENT"))));

		assertEquals(TicketApplicationErrorCode.SEARCH_WORD_TOO_SHORT, exception.getErrorCode());
		verifyNoDependencyInteractions();
	}

	@Test
	void searchAllTicketsByConditionsRejectsSingleCharacterSearchWord() {
		ApiApplicationException exception = assertThrows(ApiApplicationException.class, () ->
			ticketQueryService.searchAllTicketsByConditions(1L, 100L, new TicketListQuery("a", List.of(), List.of())));

		assertEquals(TicketApplicationErrorCode.SEARCH_WORD_TOO_SHORT, exception.getErrorCode());
		verifyNoDependencyInteractions();
	}

	@Test
	void searchAllTicketsByConditionsRejectsDeletedBookingStatus() {
		ApiApplicationException exception = assertThrows(ApiApplicationException.class, () ->
			ticketQueryService.searchAllTicketsByConditions(1L,
				100L,
				new TicketListQuery("ab", List.of(), List.of("BOOKING_DELETED"))));

		assertEquals(TicketApplicationErrorCode.DELETED_TICKET_RETRIEVE_NOT_ALLOWED, exception.getErrorCode());
		verifyNoDependencyInteractions();
	}

	@Test
	void findAllTicketsByConditionsRejectsDeletedBookingStatus() {
		ApiApplicationException exception = assertThrows(ApiApplicationException.class, () ->
			ticketQueryService.findAllTicketsByConditions(1L,
				100L,
				new TicketListQuery(null, List.of(), List.of("BOOKING_DELETED"))));

		assertEquals(TicketApplicationErrorCode.DELETED_TICKET_RETRIEVE_NOT_ALLOWED, exception.getErrorCode());
		verifyNoDependencyInteractions();
	}

	private void verifyNoDependencyInteractions() {
		verifyNoInteractions(
			bookingRepository,
			makerTicketReadPort,
			performanceSummaryReadPort,
			memberRepository,
			scheduleRepository,
			eventPublisher
		);
	}

	private void stubOwnedTicketUpdate(Booking booking) {
		Member member = Member.rehydrate(1L, "maker", null, null, 10L,
			SocialIdentity.of(SocialType.KAKAO, 123L));
		LocalDateTime performanceDate = LocalDateTime.of(2026, 1, 1, 19, 0);
		Schedule schedule = Schedule.rehydrate(
			200L, performanceDate, performanceDate.plusHours(2), 100, 1, ScheduleNumber.FIRST, 100L);
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(performanceSummaryReadPort.findById(100L)).thenReturn(Optional.of(performance(100L, 10L)));
		when(bookingRepository.findAllById(List.of(300L))).thenReturn(List.of(booking));
		when(bookingRepository.lockById(300L)).thenReturn(Optional.of(booking));
		when(scheduleRepository.lockById(200L)).thenReturn(Optional.of(schedule));
	}

	private Booking booking(BookingStatus status) {
		return booking(status, null);
	}

	private Booking booking(BookingStatus status, Integer totalPaymentAmount) {
		return Booking.rehydrate(
			300L, 1, "booker", "010-0000-0000", status,
			LocalDateTime.of(2026, 1, 1, 12, 0), null, null, null, null, 200L, 20L,
			totalPaymentAmount);
	}

	private TicketStatusUpdate ticketDetail(BookingStatusType status) {
		return TicketStatusUpdate.of(300L, TicketBookingStatus.valueOf(status.name()));
	}

	private PerformanceSummaryReadModel performance(Long id, Long userId) {
		return new PerformanceSummaryReadModel(
			id, userId, "title", "BAND", 10000, "KAKAOBANK", "123", "holder",
			"poster", "team", "venue", "contact", 1,
			LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1));
	}
}
