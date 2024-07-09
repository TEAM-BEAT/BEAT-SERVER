package com.beat.domain.performance.domain;

import com.beat.domain.BaseTimeEntity;
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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Performance extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long performanceId;

    @Column(nullable = false)
    private String performanceTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Genre genre;

    @Column(nullable = false)
    private int runningTime;

    @Column(nullable = false)
    private String performanceDescription;

    @Column(nullable = false)
    private String performanceAttentionNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private BankName bankName;

    @Column(nullable = true)
    private String accountNumber;

    @Column(nullable = false)
    private String posterImage;

    @Column(nullable = false)
    private String performanceTeamName;

    @Column(nullable = false)
    private String performanceVenue;

    @Column(nullable = false)
    private String performanceContact;

    @Column(nullable = false)
    private String performancePeriod;

    @Column(nullable = false)
    private int ticketPrice = 0;

    @Column(nullable = false)
    private int totalScheduleCount;

    @Column(nullable = false)
    private Long userId;
}