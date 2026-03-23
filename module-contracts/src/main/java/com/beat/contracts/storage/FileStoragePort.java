package com.beat.contracts.storage;

import java.util.List;

public interface FileStoragePort {

	PerformancePresignedUrls issueAllPresignedUrlsForPerformanceMaker(
		String posterImage,
		List<String> castImages,
		List<String> staffImages,
		List<String> performanceImages
	);

	CarouselPresignedUrls issueAllPresignedUrlsForCarousel(List<String> carouselImages);

	BannerPresignedUrl issuePresignedUrlForBanner(String bannerImage);
}
