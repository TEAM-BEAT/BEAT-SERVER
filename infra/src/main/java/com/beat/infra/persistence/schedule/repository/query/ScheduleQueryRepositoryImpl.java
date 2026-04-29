package com.beat.infra.persistence.schedule.repository.query;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.beat.domain.schedule.repository.dto.MinPerformanceDateDto;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ScheduleQueryRepositoryImpl implements ScheduleQueryRepository {

    private final EntityManager entityManager;

    @Override
    public List<MinPerformanceDateDto> findMinPerformanceDateByPerformanceIds(List<Long> performanceIds) {
        String jpql = """
            SELECT new com.beat.domain.schedule.repository.dto.MinPerformanceDateDto(
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

        @SuppressWarnings("unchecked")
        Query query = entityManager.createQuery(jpql, MinPerformanceDateDto.class);
        query.setParameter("now", LocalDateTime.now());
        query.setParameter("performanceIds", performanceIds);

        @SuppressWarnings("unchecked")
        List<MinPerformanceDateDto> result = query.getResultList();
        return result;
    }
}
