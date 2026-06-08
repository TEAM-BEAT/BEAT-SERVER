package com.beat.apis.query;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.beat.apis.support.AbstractIntegrationTest;
import com.beat.contracts.booking.MakerTicketReadPort;
import com.beat.contracts.schedule.ScheduleReadPort;

/**
 * Kotlin JDSL read/query adapter 가 실제 MySQL 에 대해 렌더링·실행되는지 검증한다.
 *
 * <p>데이터를 적재하지 않고 빈 결과를 기대하더라도, hibernate-support 의 {@code createQuery} extension 이
 * Spring 이 관리하는 {@code EntityManager} 에서 동작하고 JDSL 이 생성한 JPQL 이 유효한 SQL 로
 * 실행된다는 점(문법 정합성)을 testcontainer MySQL 위에서 실증한다.</p>
 */
@Transactional
class ReadPortJdslExecutionIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private ScheduleReadPort scheduleReadPort;

	@Autowired
	private MakerTicketReadPort makerTicketReadPort;

	@Test
	void scheduleReadPortRendersAndExecutesCoalesceMinGroupByOnMySql() {
		// COALESCE(MIN(CASE ...), MIN(CASE ...)) + GROUP BY 가 실제 MySQL 에서 실행되는지 검증한다.
		List<?> result = scheduleReadPort.findMinPerformanceDateByPerformanceIds(List.of(999_999L));

		assertNotNull(result);
		assertTrue(result.isEmpty(), "존재하지 않는 performanceId 조회 시 빈 결과여야 한다");
	}

	@Test
	void makerTicketReadPortRendersAndExecutesCrossJoinDynamicQueryOnMySql() {
		// Booking × Schedule cross-join + 동적조건 + ORDER BY CASE 가 실제 MySQL 에서 실행되는지 검증한다.
		List<?> result = makerTicketReadPort.findTickets(999_999L, List.of(), List.of());

		assertNotNull(result);
		assertTrue(result.isEmpty(), "존재하지 않는 performanceId 조회 시 빈 결과여야 한다");
	}
}
