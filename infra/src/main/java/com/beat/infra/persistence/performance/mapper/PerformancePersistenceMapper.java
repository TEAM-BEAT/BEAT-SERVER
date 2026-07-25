package com.beat.infra.persistence.performance.mapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.beat.domain.exception.DomainException;
import com.beat.domain.performance.model.Performance;
import com.beat.domain.performance.model.Cast;
import com.beat.domain.performance.model.PerformanceImage;
import com.beat.domain.performance.model.Staff;
import com.beat.domain.performance.vo.PaymentAccount;
import com.beat.domain.performance.vo.PerformancePeriod;
import com.beat.domain.performance.vo.RunningTime;
import com.beat.domain.performance.vo.TicketPrice;
import com.beat.infra.persistence.exception.PersistenceMappingException;
import com.beat.infra.persistence.performance.entity.PaymentAccountJpaValue;
import com.beat.infra.persistence.performance.entity.PerformanceJpaEntity;
import com.beat.infra.persistence.performance.entity.PerformancePeriodJpaValue;

@Component
public class PerformancePersistenceMapper {
	private static final DateTimeFormatter LEGACY_DATE_FORMATTER =
		DateTimeFormatter.ofPattern("uuuu.MM.dd").withResolverStyle(ResolverStyle.STRICT);

	public Performance toDomain(PerformanceJpaEntity entity) {
		return toDomain(entity, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
	}

	public Performance toDomain(
		PerformanceJpaEntity entity,
		List<Cast> casts,
		List<Staff> staffs,
		List<PerformanceImage> images
	) {
		try {
			return Performance.rehydrate(
				entity.getId(),
				entity.getPerformanceTitle(),
				entity.getGenre(),
				RunningTime.of(entity.getRunningTime()),
				entity.getPerformanceDescription(),
				entity.getPerformanceAttentionNote(),
				toDomain(entity.getPaymentAccount()),
				entity.getPosterImage(),
				entity.getPerformanceTeamName(),
				entity.getPerformanceVenue(),
				entity.getRoadAddressName(),
				entity.getPlaceDetailAddress(),
				entity.getLatitude(),
				entity.getLongitude(),
				entity.getPerformanceContact(),
				toDomainPeriod(entity),
				TicketPrice.of(entity.getTicketPrice()),
				entity.getTotalScheduleCount(),
					entity.getUserId(),
					casts,
					staffs,
					images
				);
		} catch (DomainException exception) {
			throw PersistenceMappingException.invalidStoredState("Performance", entity.getId(), exception);
		}
	}

	public PerformanceJpaEntity toEntity(Performance domain) {
		return PerformanceJpaEntity.rehydrate(
			domain.getId(),
			domain.getPerformanceTitle(),
			domain.getGenre(),
			domain.getRunningTimeValue().getMinutes(),
			domain.getPerformanceDescription(),
			domain.getPerformanceAttentionNote(),
			toEntity(domain.getPaymentAccount()),
			domain.getPosterImage(),
			domain.getPerformanceTeamName(),
			domain.getPerformanceVenue(),
			domain.getRoadAddressName(),
			domain.getPlaceDetailAddress(),
			domain.getLatitude(),
			domain.getLongitude(),
			domain.getPerformanceContact(),
			toEntity(domain.getPerformancePeriodValue()),
			formatLegacy(domain.getPerformancePeriodValue()),
			domain.getTicketPriceValue().getAmount(),
			domain.getTotalScheduleCount(),
			domain.getUserId()
		);
	}

	private PaymentAccount toDomain(PaymentAccountJpaValue value) {
		if (value == null) {
			return null;
		}
		return PaymentAccount.fromNullable(value.getBankName(), value.getAccountNumber(), value.getAccountHolder());
	}

	private PaymentAccountJpaValue toEntity(PaymentAccount value) {
		if (value == null) {
			return null;
		}
		return new PaymentAccountJpaValue(value.getBankName(), value.getAccountNumber(), value.getAccountHolder());
	}

	private PerformancePeriod toDomainPeriod(PerformanceJpaEntity entity) {
		PerformancePeriodJpaValue value = entity.getPerformancePeriodValue();
		if (value == null) {
			return parseLegacyPeriod(entity);
		}
		if (value.getStartDate() == null || value.getEndDate() == null) {
			throw PersistenceMappingException.invalidStoredState(
				"Performance",
				entity.getId(),
				new IllegalStateException("Performance period columns must be both null or both non-null")
			);
		}
		return PerformancePeriod.of(value.getStartDate(), value.getEndDate());
	}

	private PerformancePeriod parseLegacyPeriod(PerformanceJpaEntity entity) {
		String legacyPeriod = entity.getLegacyPerformancePeriod();
		if (legacyPeriod == null || legacyPeriod.isBlank()) {
			throw invalidLegacyPeriod(entity, null);
		}

		String[] dates = legacyPeriod.split("~", -1);
		if (dates.length > 2) {
			throw invalidLegacyPeriod(entity, null);
		}

		try {
			LocalDate startDate = LocalDate.parse(dates[0], LEGACY_DATE_FORMATTER);
			LocalDate endDate = dates.length == 1
				? startDate
				: LocalDate.parse(dates[1], LEGACY_DATE_FORMATTER);
			return PerformancePeriod.of(startDate, endDate);
		} catch (DateTimeParseException exception) {
			throw invalidLegacyPeriod(entity, exception);
		}
	}

	private PersistenceMappingException invalidLegacyPeriod(PerformanceJpaEntity entity, Throwable cause) {
		IllegalStateException invalidState = new IllegalStateException("Invalid legacy performance period", cause);
		return PersistenceMappingException.invalidStoredState("Performance", entity.getId(), invalidState);
	}

	private PerformancePeriodJpaValue toEntity(PerformancePeriod value) {
		return new PerformancePeriodJpaValue(value.getStartDate(), value.getEndDate());
	}

	private String formatLegacy(PerformancePeriod value) {
		String start = value.getStartDate().format(LEGACY_DATE_FORMATTER);
		if (value.getStartDate().equals(value.getEndDate())) {
			return start;
		}
		return start + "~" + value.getEndDate().format(LEGACY_DATE_FORMATTER);
	}
}
