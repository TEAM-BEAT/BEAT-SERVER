package com.beat.domain.schedule.dao;


import static com.beat.domain.schedule.domain.QSchedule.*;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.beat.domain.schedule.dao.dto.MinPerformanceDateDto;
import com.beat.domain.schedule.dao.dto.QMinPerformanceDateDto;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ScheduleRepositoryCustomImpl implements ScheduleRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	@Override
	public List<MinPerformanceDateDto> findMinPerformanceDateByPerformanceIds(List<Long> performanceIds) {
		LocalDateTime now = LocalDateTime.now();

		// 미래 중 가장 빠른 날짜 (없으면 null)
		DateTimeExpression<LocalDateTime> futureMin = Expressions
			.cases()
			.when(schedule.performanceDate.goe(now))
			.then(schedule.performanceDate)
			.otherwise((LocalDateTime) null)
			.min();

		// (현재) 전체 중 가장 빠른 과거 날짜 -> 변동 여지 있음!
		DateTimeExpression<LocalDateTime> pastMin = Expressions
			.cases()
			.when(schedule.performanceDate.lt(now))
			.then(schedule.performanceDate)
			.otherwise((LocalDateTime) null)
			.min();

		// 미래가 있으면 미래 중 최솟값, 없으면 가장 늦은 과거 값
		DateTimeExpression<LocalDateTime> finalPerformanceDate = Expressions
			.cases()
			.when(futureMin.isNotNull()).then(futureMin)
			.otherwise(pastMin);

		return queryFactory
			.select(new QMinPerformanceDateDto(
				schedule.performance.id,
				finalPerformanceDate
			))
			.from(schedule)
			.where(schedule.performance.id.in(performanceIds))
			.groupBy(schedule.performance.id)
			.fetch();
	}
}
