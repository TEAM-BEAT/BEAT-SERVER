package com.beat.domain.schedule.domain;

import com.beat.domain.performance.domain.Performance;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime performanceDate;

    @Column(nullable = false)
    private int totalTicketCount;

    @Column(nullable = false)
    private int soldTicketCount = 0;

    @Column(nullable = false)
    private boolean isBooking = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleNumber scheduleNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id", nullable = false)
    private Performance performance;

    @Builder
    public Schedule(LocalDateTime performanceDate, int totalTicketCount, int soldTicketCount, boolean isBooking, ScheduleNumber scheduleNumber, Performance performance) {
        this.performanceDate = performanceDate;
        this.totalTicketCount = totalTicketCount;
        this.soldTicketCount = soldTicketCount;
        this.isBooking = isBooking;
        this.scheduleNumber = scheduleNumber;
        this.performance = performance;
    }

    public static Schedule create(LocalDateTime performanceDate, int totalTicketCount, int soldTicketCount, boolean isBooking, ScheduleNumber scheduleNumber, Performance performance) {
        return Schedule.builder()
                .performanceDate(performanceDate)
                .totalTicketCount(totalTicketCount)
                .soldTicketCount(soldTicketCount)
                .isBooking(isBooking)
                .scheduleNumber(scheduleNumber)
                .performance(performance)
                .build();
    }
}