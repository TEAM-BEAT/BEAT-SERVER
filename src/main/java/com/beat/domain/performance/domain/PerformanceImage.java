package com.beat.domain.performance.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PerformanceImage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String performanceImage;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "performance_id", nullable = false)
	private Performance performance;

	@Builder
	public PerformanceImage(String performanceImage, Performance performance) {
		this.performanceImage = performanceImage;
		this.performance = performance;
	}

	public static PerformanceImage create(String perforemanceImage, Performance performance) {
		return PerformanceImage.builder()
			.performanceImage(perforemanceImage)
			.performance(performance)
			.build();
	}

	public void update(String performanceImage) {
		this.performanceImage = performanceImage;
	}
}
