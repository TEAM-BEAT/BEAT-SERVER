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
	private String performanceImageUrl;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "performance_id", nullable = false)
	private Performance performance;

	@Builder
	private PerformanceImage(String performanceImageUrl, Performance performance) {
		this.performanceImageUrl = performanceImageUrl;
		this.performance = performance;
	}

	public static PerformanceImage create(String performanceImageUrl, Performance performance) {
		return PerformanceImage.builder()
			.performanceImageUrl(performanceImageUrl)
			.performance(performance)
			.build();
	}

	public void updatePerformanceImageUrl(String performanceImageUrl) {
		this.performanceImageUrl = performanceImageUrl;
	}
}
