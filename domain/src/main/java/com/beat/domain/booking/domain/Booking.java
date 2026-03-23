package com.beat.domain.booking.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.beat.domain.performance.domain.BankName;
import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.user.domain.Users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "schedule_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Schedule schedule;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Users users;

	@Builder
	private Booking(int purchaseTicketCount, String bookerName, String bookerPhoneNumber, BookingStatus bookingStatus,
		String birthDate, String password, BankName bankName, String accountNumber, String accountHolder,
		Schedule schedule, Users users) {
		this.purchaseTicketCount = purchaseTicketCount;
		this.bookerName = bookerName;
		this.bookerPhoneNumber = bookerPhoneNumber;
		this.bookingStatus = bookingStatus;
		this.birthDate = birthDate;
		this.password = password;
		this.bankName = bankName;
		this.accountNumber = accountNumber;
		this.accountHolder = accountHolder;
		this.schedule = schedule;
		this.users = users;
	}

	public static Booking create(int purchaseTicketCount, String bookerName, String bookerPhoneNumber,
		BookingStatus bookingStatus, String birthDate, String password,
		BankName bankName, String accountNumber, String accountHolder, Schedule schedule, Users users) {
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
			.schedule(schedule)
			.users(users)
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
