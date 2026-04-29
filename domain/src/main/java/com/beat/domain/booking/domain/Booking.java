package com.beat.domain.booking.domain;

import java.time.LocalDateTime;

import com.beat.domain.performance.domain.BankName;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Booking {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private int purchaseTicketCount;

	@Column(nullable = false)
	private String bookerName;

	@Column(nullable = false)
	private String bookerPhoneNumber;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private BookingStatus bookingStatus = BookingStatus.CHECKING_PAYMENT;

	@Column(nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(nullable = true)
	private LocalDateTime cancellationDate;

	@Column(nullable = true)
	private String birthDate;

	@Column(nullable = true)
	private String password;

	@Enumerated(EnumType.STRING)
	@Column(nullable = true)
	private BankName bankName;

	@Column(nullable = true)
	private String accountNumber;

	@Column(nullable = true)
	private String accountHolder;

	@Column(name = "schedule_id", nullable = false)
	private Long scheduleId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Builder
	private Booking(int purchaseTicketCount, String bookerName, String bookerPhoneNumber, BookingStatus bookingStatus,
		String birthDate, String password, BankName bankName, String accountNumber, String accountHolder,
		Long scheduleId, Long userId) {
		this.purchaseTicketCount = purchaseTicketCount;
		this.bookerName = bookerName;
		this.bookerPhoneNumber = bookerPhoneNumber;
		this.bookingStatus = bookingStatus;
		this.birthDate = birthDate;
		this.password = password;
		this.bankName = bankName;
		this.accountNumber = accountNumber;
		this.accountHolder = accountHolder;
		this.scheduleId = scheduleId;
		this.userId = userId;
	}

	public static Booking create(int purchaseTicketCount, String bookerName, String bookerPhoneNumber,
		BookingStatus bookingStatus, String birthDate, String password,
		BankName bankName, String accountNumber, String accountHolder, Long scheduleId, Long userId) {
		return Booking.builder()
			.purchaseTicketCount(purchaseTicketCount)
			.bookerName(bookerName)
			.bookerPhoneNumber(bookerPhoneNumber)
			.bookingStatus(bookingStatus)
			.birthDate(birthDate)
			.password(password)
			.bankName(bankName)
			.accountNumber(accountNumber)
			.accountHolder(accountHolder)
			.scheduleId(scheduleId)
			.userId(userId)
			.build();
	}

	public void updateBookingStatus(BookingStatus bookingStatus) {
		this.bookingStatus = bookingStatus;
		if (bookingStatus == BookingStatus.BOOKING_CANCELLED || bookingStatus == BookingStatus.BOOKING_DELETED) {
			this.cancellationDate = LocalDateTime.now();
		}
	}

	public void updateRefundInfo(BankName bankName, String accountNumber, String accountHolder) {
		this.bankName = bankName;
		this.accountNumber = accountNumber;
		this.accountHolder = accountHolder;
		this.bookingStatus = BookingStatus.REFUND_REQUESTED;
	}
}
