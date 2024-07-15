package com.beat.domain.performance.domain;

import com.beat.domain.BaseTimeEntity;
import com.beat.domain.user.domain.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Performance extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true) // 테스트를 위한 false
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Users users;

    @Builder
    public Performance(String performanceTitle, Genre genre, int runningTime, String performanceDescription, String performanceAttentionNote,
                       BankName bankName, String accountNumber, String posterImage, String performanceTeamName, String performanceVenue, String performanceContact,
                       String performancePeriod, int ticketPrice, int totalScheduleCount, Users users) {
        this.performanceTitle = performanceTitle;
        this.genre = genre;
        this.runningTime = runningTime;
        this.performanceDescription = performanceDescription;
        this.performanceAttentionNote = performanceAttentionNote;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.posterImage = posterImage;
        this.performanceTeamName = performanceTeamName;
        this.performanceVenue = performanceVenue;
        this.performanceContact = performanceContact;
        this.performancePeriod = performancePeriod;
        this.ticketPrice = ticketPrice;
        this.totalScheduleCount = totalScheduleCount;
        this.users = users;
    }

    public static Performance create(
            String performanceTitle, Genre genre, int runningTime, String performanceDescription, String performanceAttentionNote,
            BankName bankName, String accountNumber, String posterImage, String performanceTeamName, String performanceVenue, String performanceContact,
            String performancePeriod, int ticketPrice, int totalScheduleCount, Users users) {
        return Performance.builder()
                .performanceTitle(performanceTitle)
                .genre(genre)
                .runningTime(runningTime)
                .performanceDescription(performanceDescription)
                .performanceAttentionNote(performanceAttentionNote)
                .bankName(bankName)
                .accountNumber(accountNumber)
                .posterImage(posterImage)
                .performanceTeamName(performanceTeamName)
                .performanceVenue(performanceVenue)
                .performanceContact(performanceContact)
                .performancePeriod(performancePeriod)
                .ticketPrice(ticketPrice)
                .totalScheduleCount(totalScheduleCount)
                .users(users)
                .build();
    }
}