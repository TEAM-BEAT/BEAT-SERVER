package com.beat.domain.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookingId;

    @Column(nullable = false)
    private int purchaseTicketCount;

    @Column(nullable = false)
    private String bookerName;

    @Column(nullable = false)
    private String bookerPhoneNumber;

    @Column(nullable = false)
    private boolean isPaymentCompleted = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = true)
    private LocalDateTime birthDate;

    @Column(nullable = true)
    private String password;

    @Column(nullable = false)
    private Long scheduleId;

    @Column(nullable = false)
    private Long userId;
}