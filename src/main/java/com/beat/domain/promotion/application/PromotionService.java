package com.beat.domain.promotion.application;

import com.beat.domain.performance.domain.Performance;
import com.beat.domain.promotion.dao.PromotionRepository;
import com.beat.domain.promotion.domain.Promotion;
import com.beat.domain.schedule.application.ScheduleService;
import com.beat.domain.schedule.dao.ScheduleRepository;
import com.beat.domain.schedule.domain.Schedule;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleService scheduleService;

    @Scheduled(cron = "1 0 0 * * ?")
    @Transactional
    public void checkAndDeleteInvalidPromotions() {
        List<Promotion> promotions = promotionRepository.findAll();

        for (Promotion promotion : promotions) {
            if (promotion.getPerformance() != null) {
                Performance performance = promotion.getPerformance();

                List<Schedule> schedules = scheduleRepository.findByPerformanceId(performance.getId());
                int minDueDate = scheduleService.getMinDueDate(schedules);

                // MinDueDate가 음수일 경우 Promotion 삭제
                if (minDueDate < 0) {
                    promotionRepository.delete(promotion);
                }
            }
        }
    }
}