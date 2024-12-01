package com.beat.domain.performance.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.beat.domain.BaseTimeEntity;
import com.beat.domain.performance.exception.PerformanceErrorCode;
import com.beat.domain.promotion.domain.Promotion;
import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.schedule.domain.ScheduleNumber;
import com.beat.domain.user.domain.Users;
import com.beat.global.common.exception.BadRequestException;
import com.beat.global.common.exception.ForbiddenException;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Performance extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String performanceTitle;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Genre genre;

	@Column(nullable = false)
	private int runningTime;

	@Column(nullable = false, length = 1500)
	private String performanceDescription;

	@Column(nullable = false, length = 1500)
	private String performanceAttentionNote;

	@Enumerated(EnumType.STRING)
	@Column(nullable = true)
	private BankName bankName;

	@Column(nullable = true)
	private String accountNumber;

	@Column(nullable = true)
	private String accountHolder;

	@Column(nullable = false)
	private String posterImage;

	@Column(nullable = false)
	private String performanceTeamName;

	@Column(nullable = false, columnDefinition = "text")
	private String performanceVenue;

	@Column(nullable = false)
	private String roadAddressName;

	@Column(nullable = false)
	private String placeDetailAddress;

	@Column(nullable = false)
	private String latitude;

	@Column(nullable = false)
	private String longitude;

	@Column(nullable = false)
	private String performanceContact;

	@Column(nullable = false)
	private String performancePeriod;

	@Column(nullable = false)
	private int ticketPrice = 0;

	@Column(nullable = false)
	private int totalScheduleCount;

	@OneToMany(mappedBy = "performance", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Promotion> promotions = new ArrayList<>();

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Users users;

	@OneToMany(mappedBy = "performance", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<PerformanceImage> performanceImageList = new ArrayList<>();

	@Builder
	private Performance(String performanceTitle, Genre genre, int runningTime, String performanceDescription,
		String performanceAttentionNote,
		BankName bankName, String accountNumber, String accountHolder, String posterImage, String performanceTeamName,
		String performanceVenue, String roadAddressName, String placeDetailAddress, String latitude, String longitude,
		String performanceContact, String performancePeriod, int ticketPrice, int totalScheduleCount, Users users) {
		this.performanceTitle = performanceTitle;
		this.genre = genre;
		this.runningTime = runningTime;
		this.performanceDescription = performanceDescription;
		this.performanceAttentionNote = performanceAttentionNote;
		this.bankName = bankName;
		this.accountNumber = accountNumber;
		this.accountHolder = accountHolder;
		this.posterImage = posterImage;
		this.performanceTeamName = performanceTeamName;
		this.performanceVenue = performanceVenue;
		this.roadAddressName = roadAddressName;
		this.placeDetailAddress = placeDetailAddress;
		this.latitude = latitude;
		this.longitude = longitude;
		this.performanceContact = performanceContact;
		this.performancePeriod = performancePeriod;
		this.ticketPrice = ticketPrice;
		this.totalScheduleCount = totalScheduleCount;
		this.users = users;
	}

	public static Performance create(
		String performanceTitle, Genre genre, int runningTime, String performanceDescription,
		String performanceAttentionNote,
		BankName bankName, String accountNumber, String accountHolder, String posterImage, String performanceTeamName,
		String performanceVenue, String roadAddressName, String placeDetailAddress, String latitude, String longitude,
		String performanceContact, String performancePeriod, int ticketPrice, int totalScheduleCount, Users users) {
		return Performance.builder()
			.performanceTitle(performanceTitle)
			.genre(genre)
			.runningTime(runningTime)
			.performanceDescription(performanceDescription)
			.performanceAttentionNote(performanceAttentionNote)
			.bankName(bankName)
			.accountNumber(accountNumber)
			.accountHolder(accountHolder)
			.posterImage(posterImage)
			.performanceTeamName(performanceTeamName)
			.performanceVenue(performanceVenue)
			.roadAddressName(roadAddressName)
			.placeDetailAddress(placeDetailAddress)
			.latitude(latitude)
			.longitude(longitude)
			.performanceContact(performanceContact)
			.performancePeriod(performancePeriod)
			.ticketPrice(ticketPrice)
			.totalScheduleCount(totalScheduleCount)
			.users(users)
			.build();
	}

	public void update(
		String performanceTitle, Genre genre, int runningTime, String performanceDescription,
		String performanceAttentionNote,
		BankName bankName, String accountNumber, String accountHolder, String posterImage, String performanceTeamName,
		String performanceVenue, String roadAddressName, String placeDetailAddress, String latitude, String longitude,
		String performanceContact, String performancePeriod, int totalScheduleCount) {
		this.performanceTitle = performanceTitle;
		this.genre = genre;
		this.runningTime = runningTime;
		this.performanceDescription = performanceDescription;
		this.performanceAttentionNote = performanceAttentionNote;
		this.bankName = bankName;
		this.accountNumber = accountNumber;
		this.accountHolder = accountHolder;
		this.posterImage = posterImage;
		this.performanceTeamName = performanceTeamName;
		this.performanceVenue = performanceVenue;
		this.roadAddressName = roadAddressName;
		this.placeDetailAddress = placeDetailAddress;
		this.latitude = latitude;
		this.longitude = longitude;
		this.performanceContact = performanceContact;
		this.performancePeriod = performancePeriod;
		this.totalScheduleCount = totalScheduleCount;
	}

	public void updateTicketPrice(int newTicketPrice) {
		if (newTicketPrice < 0) {
			throw new BadRequestException(PerformanceErrorCode.NEGATIVE_TICKET_PRICE);
		}
		this.ticketPrice = newTicketPrice;
	}

	public void updatePerformancePeriod(List<LocalDateTime> performanceDates) {
		if (performanceDates.isEmpty()) {
			throw new BadRequestException(PerformanceErrorCode.SCHEDULE_LIST_NOT_FOUND);
		}

		LocalDateTime startDate = performanceDates.stream().min(Comparator.naturalOrder()).get();
		LocalDateTime endDate = performanceDates.stream().max(Comparator.naturalOrder()).get();

		this.performancePeriod = formatPerformancePeriod(startDate, endDate);
	}

	private String formatPerformancePeriod(LocalDateTime startDate, LocalDateTime endDate) {
		if (startDate.toLocalDate().equals(endDate.toLocalDate())) {
			return startDate.toLocalDate().toString().replace("-", ".");
		}
		return startDate.toLocalDate().toString().replace("-", ".")
			+ "~" + endDate.toLocalDate().toString().replace("-", ".");
	}

	public void assignScheduleNumbers(List<Schedule> schedules) {
		List<ScheduleNumber> scheduleNumbers = List.of(ScheduleNumber.values());
		schedules.sort(Comparator.comparing(Schedule::getPerformanceDate));

		for (int i = 0; i < schedules.size(); i++) {
			if (i < scheduleNumbers.size()) {
				schedules.get(i).setScheduleNumber(scheduleNumbers.get(i));
			}
		}
	}

	public void validatePerformanceOwnership(Long userId) {
		if (!this.users.getId().equals(userId)) {
			throw new ForbiddenException(PerformanceErrorCode.NOT_PERFORMANCE_OWNER);
		}
	}

}
