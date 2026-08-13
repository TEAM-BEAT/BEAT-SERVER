package com.beat.infra.persistence.performance.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.beat.domain.sharedkernel.vo.BankName;
import com.beat.domain.performance.model.Genre;
import com.beat.domain.performance.vo.PaymentAccount;
import com.beat.domain.performance.model.Performance;
import com.beat.domain.performance.vo.PerformancePeriod;
import com.beat.domain.performance.vo.RunningTime;
import com.beat.domain.performance.vo.TicketPrice;
import com.beat.infra.persistence.exception.PersistenceMappingException;
import com.beat.infra.persistence.performance.entity.PerformanceJpaEntity;
import com.beat.infra.persistence.performance.entity.PerformancePeriodJpaValue;

class PerformancePersistenceMapperTest {
	private final PerformancePersistenceMapper mapper = new PerformancePersistenceMapper();

	@Test
	void roundTripsValueObjectsWithoutChangingLegacyColumns() {
		Performance domain = performance(PaymentAccount.of(BankName.KAKAOBANK, "123", "holder"));

		PerformanceJpaEntity entity = mapper.toEntity(domain);
		Performance roundTrip = mapper.toDomain(entity);

		assertEquals(BankName.KAKAOBANK, entity.getPaymentAccount().getBankName());
		assertEquals(LocalDate.of(2026, 7, 16), entity.getPerformancePeriodValue().getStartDate());
		assertEquals("2026.07.16~2026.07.18", entity.getLegacyPerformancePeriod());
		assertEquals(domain.getPaymentAccount(), roundTrip.getPaymentAccount());
		assertEquals(domain.getPerformancePeriodValue(), roundTrip.getPerformancePeriodValue());
		assertEquals(domain.getRunningTimeValue(), roundTrip.getRunningTimeValue());
		assertEquals(domain.getTicketPriceValue(), roundTrip.getTicketPriceValue());
	}

	@Test
	void keepsAllNullPaymentAccountAsNull() {
		PerformanceJpaEntity entity = mapper.toEntity(performance(null));

		assertNull(entity.getPaymentAccount());
		assertNull(mapper.toDomain(entity).getPaymentAccount());
	}

	@Test
	void rejectsPartiallyPopulatedPeriodColumns() throws ReflectiveOperationException {
		PerformanceJpaEntity entity = mapper.toEntity(performance(null));
		PerformancePeriodJpaValue partialPeriod = new PerformancePeriodJpaValue(
			LocalDate.of(2026, 7, 16), LocalDate.of(2026, 7, 18));
		var startDateField = PerformancePeriodJpaValue.class.getDeclaredField("startDate");
		startDateField.setAccessible(true);
		startDateField.set(partialPeriod, null);
		entity = PerformanceJpaEntity.rehydrate(
			entity.getId(), entity.getPerformanceTitle(), entity.getGenre(), entity.getRunningTime(),
			entity.getPerformanceDescription(), entity.getPerformanceAttentionNote(), entity.getPaymentAccount(),
			entity.getPosterImage(), entity.getPerformanceTeamName(), entity.getPerformanceVenue(),
			entity.getRoadAddressName(), entity.getPlaceDetailAddress(), entity.getLatitude(), entity.getLongitude(),
			entity.getPerformanceContact(), partialPeriod,
			entity.getLegacyPerformancePeriod(), entity.getTicketPrice(), entity.getTotalScheduleCount(), entity.getUserId());

		PerformanceJpaEntity partiallyPopulated = entity;
		assertThrows(PersistenceMappingException.class, () -> mapper.toDomain(partiallyPopulated));
	}

	private Performance performance(PaymentAccount paymentAccount) {
		return Performance.rehydrate(
			1L, "title", Genre.BAND, RunningTime.of(90), "description", "attention", paymentAccount,
			"poster", "team", "venue", "road", "detail", "37.1", "127.1", "010-0000-0000",
			PerformancePeriod.of(LocalDate.of(2026, 7, 16), LocalDate.of(2026, 7, 18)),
			TicketPrice.of(20_000), 3, 7L
		);
	}
}
