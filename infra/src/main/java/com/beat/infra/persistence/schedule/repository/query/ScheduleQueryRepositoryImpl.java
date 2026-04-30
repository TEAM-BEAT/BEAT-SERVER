package com.beat.infra.persistence.schedule.repository.query;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.beat.contracts.schedule.ScheduleReadPort;
import com.beat.contracts.schedule.readmodel.MinPerformanceDateReadModel;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ScheduleQueryRepositoryImpl implements ScheduleReadPort {

	private final EntityManager entityManager;

	@Override
	public List<MinPerformanceDateReadModel> findMinPerformanceDateByPerformanceIds(List<Long> performanceIds) {
		if (performanceIds == null || performanceIds.isEmpty()) {
			return List.of();
		}

		String jpql = """
			SELECT new com.beat.contracts.schedule.readmodel.MinPerformanceDateReadModel(
				s.performanceId,
				CASE
					WHEN MIN(CASE WHEN s.performanceDate >= :now THEN s.performanceDate ELSE NULL END) IS NOT NULL
					THEN MIN(CASE WHEN s.performanceDate >= :now THEN s.performanceDate ELSE NULL END)
					ELSE MIN(CASE WHEN s.performanceDate < :now THEN s.performanceDate ELSE NULL END)
				END
			)
			FROM Schedule s
			WHERE s.performanceId IN :performanceIds
			GROUP BY s.performanceId
			""";

		TypedQuery<MinPerformanceDateReadModel> query = entityManager.createQuery(
			jpql,
			MinPerformanceDateReadModel.class
		);
		query.setParameter("now", LocalDateTime.now());
		query.setParameter("performanceIds", performanceIds);

		return query.getResultList();
	}
}
