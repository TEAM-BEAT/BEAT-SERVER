package com.beat.domain.schedule.dao.dto;

import java.time.LocalDateTime;

import com.querydsl.core.annotations.QueryProjection;

import lombok.Getter;

@Getter
public class MinPerformanceDateDto {
	private final Long performanceId;
	private final LocalDateTime performanceDate;

	@QueryProjection
	public MinPerformanceDateDto(Long performanceId, LocalDateTime performanceDate) {
		this.performanceId = performanceId;
		this.performanceDate = performanceDate;
	}
}
