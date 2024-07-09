package com.beat.domain.schedule.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long scheduleId;

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

    @Column(nullable = false)
    private Long performanceId;
}