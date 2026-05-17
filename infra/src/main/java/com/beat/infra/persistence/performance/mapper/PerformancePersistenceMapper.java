package com.beat.infra.persistence.performance.mapper;

import org.springframework.stereotype.Component;

import com.beat.domain.performance.domain.Performance;
import com.beat.infra.persistence.performance.entity.PerformanceJpaEntity;

@Component
public class PerformancePersistenceMapper {

	public Performance toDomain(PerformanceJpaEntity entity) {
		return Performance.rehydrate(
			entity.getId(),
			entity.getPerformanceTitle(),
			entity.getGenre(),
			entity.getRunningTime(),
			entity.getPerformanceDescription(),
			entity.getPerformanceAttentionNote(),
			entity.getBankName(),
			entity.getAccountNumber(),
			entity.getAccountHolder(),
			entity.getPosterImage(),
			entity.getPerformanceTeamName(),
			entity.getPerformanceVenue(),
			entity.getRoadAddressName(),
			entity.getPlaceDetailAddress(),
			entity.getLatitude(),
			entity.getLongitude(),
			entity.getPerformanceContact(),
			entity.getPerformancePeriod(),
			entity.getTicketPrice(),
			entity.getTotalScheduleCount(),
			entity.getUserId()
		);
	}

	public PerformanceJpaEntity toEntity(Performance domain) {
		return PerformanceJpaEntity.rehydrate(
			domain.getId(),
			domain.getPerformanceTitle(),
			domain.getGenre(),
			domain.getRunningTime(),
			domain.getPerformanceDescription(),
			domain.getPerformanceAttentionNote(),
			domain.getBankName(),
			domain.getAccountNumber(),
			domain.getAccountHolder(),
			domain.getPosterImage(),
			domain.getPerformanceTeamName(),
			domain.getPerformanceVenue(),
			domain.getRoadAddressName(),
			domain.getPlaceDetailAddress(),
			domain.getLatitude(),
			domain.getLongitude(),
			domain.getPerformanceContact(),
			domain.getPerformancePeriod(),
			domain.getTicketPrice(),
			domain.getTotalScheduleCount(),
			domain.getUserId()
		);
	}
}
