package com.beat.domain.staff.domain;

import com.beat.domain.performance.domain.Performance;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String staffName;

    @Column(nullable = false)
    private String staffRole;

    @Column(nullable = false)
    private String staffPhoto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Performance performance;
}
