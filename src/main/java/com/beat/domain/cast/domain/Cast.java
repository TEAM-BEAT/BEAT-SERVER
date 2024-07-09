package com.beat.domain.cast.domain;

import com.beat.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cast extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long castId;

    @Column(nullable = false)
    private String castName;

    @Column(nullable = false)
    private String castRole;

    @Column(nullable = false)
    private String castPhoto;

    @Column(nullable = false)
    private Long performanceId;
}
