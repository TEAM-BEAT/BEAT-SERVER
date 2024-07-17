package com.beat.domain.cast.domain;

import  com.beat.domain.performance.domain.Performance;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cast{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String castName;

    @Column(nullable = false)
    private String castRole;

    @Column(nullable = false)
    private String castPhoto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Performance performance;

    @Builder
    public Cast(String castName, String castRole, String castPhoto, Performance performance) {
        this.castName = castName;
        this.castRole = castRole;
        this.castPhoto = castPhoto;
        this.performance = performance;
    }

    public static Cast create(String castName, String castRole, String castPhoto, Performance performance) {
        return Cast.builder()
                .castName(castName)
                .castRole(castRole)
                .castPhoto(castPhoto)
                .performance(performance)
                .build();
    }

    public void update(String castName, String castRole, String castPhoto) {
        this.castName = castName;
        this.castRole = castRole;
        this.castPhoto = castPhoto;
    }
}