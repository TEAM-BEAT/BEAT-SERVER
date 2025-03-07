package com.beat.domain.schedule.dao;


import static com.beat.domain.schedule.domain.QSchedule.*;
import static org.hibernate.query.results.Builders.*;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.beat.domain.schedule.dao.dto.MinPerformanceDateDto;
import com.beat.domain.schedule.dao.dto.QMinPerformanceDateDto;
import com.beat.domain.schedule.domain.QSchedule;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ScheduleRepositoryCustomImpl implements ScheduleRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	@Override
	public List<MinPerformanceDateDto> findMinPerformanceDateByPerformanceIds(List<Long> performanceIds) {
		LocalDateTime now = LocalDateTime.now();

		// 미래 일정 중 가장 빠른 날짜 조회
		List<MinPerformanceDateDto> futureDates = queryFactory
			.select(new QMinPerformanceDateDto(
				schedule.performance.id,
				schedule.performanceDate.min()
			))
			.from(schedule)
			.where(schedule.performance.id.in(performanceIds)
				.and(schedule.performanceDate.goe(now)))
			.groupBy(schedule.performance.id)
			.fetch();

		// 미래 일정이 존재하면 즉시 반환 → 과거 일정 조회 생략
		if (!futureDates.isEmpty()) {
			return futureDates;
		}

		// 과거 일정 중 빠른 늦은 날짜 조회 (미래 일정이 없는 경우)
		return queryFactory
			.select(new QMinPerformanceDateDto(
				schedule.performance.id,
				schedule.performanceDate.min()
			))
			.from(schedule)
			.where(schedule.performance.id.in(performanceIds)
				.and(schedule.performanceDate.lt(now)))
			.groupBy(schedule.performance.id)
			.fetch();
	}
}
