package com.beat.domain.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.beat.domain.sharedkernel.vo.BankName;
import com.beat.domain.performance.vo.PaymentAccount;
import com.beat.domain.performance.vo.PerformancePeriod;
import com.beat.domain.performance.vo.RunningTime;
import com.beat.domain.performance.vo.TicketPrice;
import com.beat.domain.performance.exception.PerformanceErrorCode;
import com.beat.domain.exception.DomainException;

class PerformanceValueObjectTest {
	@Test
	void paymentAccountAcceptsOnlyAllNullOrAllPresent() {
		assertNull(PaymentAccount.fromNullable(null, null, null));
		PaymentAccount account = PaymentAccount.fromNullable(BankName.KAKAOBANK, "123", "holder");
		assertEquals(BankName.KAKAOBANK, account.getBankName());

		DomainException exception = assertThrows(DomainException.class, () -> PaymentAccount.fromNullable(BankName.KAKAOBANK, null, "holder"));
		assertEquals(PerformanceErrorCode.INCOMPLETE_PAYMENT_ACCOUNT, exception.getErrorCode());
		assertThrows(DomainException.class, () -> PaymentAccount.of(BankName.KAKAOBANK, " ", "holder"));
		assertThrows(DomainException.class, () -> PaymentAccount.of(BankName.KAKAOBANK, "123", " "));
		assertNull(PaymentAccount.fromNullable(BankName.NONE, "", " "));
		assertThrows(DomainException.class, () -> PaymentAccount.of(BankName.NONE, "123", "holder"));
	}

	@Test
	void performancePeriodUsesMinimumAndMaximumCalendarDates() {
		PerformancePeriod period = PerformancePeriod.fromPerformanceDateTimes(List.of(
			LocalDateTime.of(2026, 7, 18, 20, 0),
			LocalDateTime.of(2026, 7, 16, 22, 0),
			LocalDateTime.of(2026, 7, 17, 19, 0)
		));

		assertEquals(LocalDate.of(2026, 7, 16), period.getStartDate());
		assertEquals(LocalDate.of(2026, 7, 18), period.getEndDate());
		assertDomainError(PerformanceErrorCode.INVALID_PERFORMANCE_PERIOD,
			() -> PerformancePeriod.fromDates(List.of()));
		assertDomainError(PerformanceErrorCode.INVALID_PERFORMANCE_PERIOD,
			() -> PerformancePeriod.of(LocalDate.of(2026, 7, 18), LocalDate.of(2026, 7, 16)));
	}

	@Test
	void runningTimeAndTicketPriceProtectTheirValues() {
		assertEquals(LocalDateTime.of(2026, 7, 16, 21, 30),
			RunningTime.of(90).endsAt(LocalDateTime.of(2026, 7, 16, 20, 0)));
		assertEquals(60_000L, TicketPrice.of(20_000).totalFor(3));
		assertThrows(DomainException.class, () -> RunningTime.of(0));
		assertThrows(DomainException.class, () -> TicketPrice.of(-1));
		assertDomainError(PerformanceErrorCode.NEGATIVE_TICKET_QUANTITY,
			() -> TicketPrice.of(20_000).totalFor(-1));
	}

	private static void assertDomainError(PerformanceErrorCode errorCode, Runnable action) {
		DomainException exception = assertThrows(DomainException.class, action::run);
		assertEquals(errorCode, exception.getErrorCode());
	}
}
