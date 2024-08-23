package com.beat.domain.performance.domain;

import com.beat.domain.BaseTimeEntity;
import com.beat.domain.performance.exception.PerformanceErrorCode;
import com.beat.domain.promotion.domain.Promotion;
import com.beat.domain.user.domain.Users;
import com.beat.global.common.exception.BadRequestException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;

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

    @Column(nullable = true)
    private String accountHolder;

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

    @OneToMany(mappedBy = "performance", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Promotion> promotions = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Users users;

    @OneToMany(mappedBy = "performance", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PerformanceImage> performanceImageList = new ArrayList<>();

    @Builder
    public Performance(String performanceTitle, Genre genre, int runningTime, String performanceDescription, String performanceAttentionNote,
                       BankName bankName, String accountNumber, String accountHolder, String posterImage, String performanceTeamName, String performanceVenue, String performanceContact,
                       String performancePeriod, int ticketPrice, int totalScheduleCount, Users users) {
        this.performanceTitle = performanceTitle;
        this.genre = genre;
        this.runningTime = runningTime;
        this.performanceDescription = performanceDescription;
        this.performanceAttentionNote = performanceAttentionNote;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
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
            BankName bankName, String accountNumber, String accountHolder, String posterImage, String performanceTeamName, String performanceVenue, String performanceContact,
            String performancePeriod, int ticketPrice, int totalScheduleCount, Users users) {
        return Performance.builder()
                .performanceTitle(performanceTitle)
                .genre(genre)
                .runningTime(runningTime)
                .performanceDescription(performanceDescription)
                .performanceAttentionNote(performanceAttentionNote)
                .bankName(bankName)
                .accountNumber(accountNumber)
                .accountHolder(accountHolder)
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

    public void update(
            String performanceTitle, Genre genre, int runningTime, String performanceDescription, String performanceAttentionNote,
            BankName bankName, String accountNumber, String accountHolder, String posterImage, String performanceTeamName, String performanceVenue, String performanceContact,
            String performancePeriod, int totalScheduleCount) {
        this.performanceTitle = performanceTitle;
        this.genre = genre;
        this.runningTime = runningTime;
        this.performanceDescription = performanceDescription;
        this.performanceAttentionNote = performanceAttentionNote;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.posterImage = posterImage;
        this.performanceTeamName = performanceTeamName;
        this.performanceVenue = performanceVenue;
        this.performanceContact = performanceContact;
        this.performancePeriod = performancePeriod;
        this.totalScheduleCount = totalScheduleCount;
    }

    public void updateTicketPrice(int newTicketPrice) {
        if (newTicketPrice < 0) {
            throw new BadRequestException(PerformanceErrorCode.NEGATIVE_TICKET_PRICE);
        }
        this.ticketPrice = newTicketPrice;
    }
}