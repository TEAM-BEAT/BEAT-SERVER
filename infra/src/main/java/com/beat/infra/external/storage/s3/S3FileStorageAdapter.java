package com.beat.infra.external.storage.s3;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.beat.contracts.storage.BannerPresignedUrl;
import com.beat.contracts.storage.CarouselPresignedUrls;
import com.beat.contracts.storage.FileStoragePort;
import com.beat.contracts.storage.PerformancePresignedUrls;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class S3FileStorageAdapter implements FileStoragePort {

	@Value("${cloud.s3.bucket}")
	private String bucket;

	private final AmazonS3 amazonS3;

	@Override
	public PerformancePresignedUrls issueAllPresignedUrlsForPerformanceMaker(
		String posterImage,
		List<String> castImages,
		List<String> staffImages,
		List<String> performanceImages
	) {
		Map<String, Map<String, String>> performanceMakerPresignedUrls = new HashMap<>();

		Map<String, String> posterUrl = new HashMap<>();
		String posterFilePath = generatePath("poster", posterImage);
		URL posterPresignedUrl = amazonS3.generatePresignedUrl(buildPresignedUrlRequest(bucket, posterFilePath));
		posterUrl.put(posterImage, posterPresignedUrl.toString());
		performanceMakerPresignedUrls.put("poster", posterUrl);

		Map<String, String> castUrls = new HashMap<>();
		for (String castImage : castImages) {
			String castFilePath = generatePath("cast", castImage);
			URL castPresignedUrl = amazonS3.generatePresignedUrl(buildPresignedUrlRequest(bucket, castFilePath));
			castUrls.put(castImage, castPresignedUrl.toString());
		}
		performanceMakerPresignedUrls.put("cast", castUrls);

		Map<String, String> staffUrls = new HashMap<>();
		for (String staffImage : staffImages) {
			String staffFilePath = generatePath("staff", staffImage);
			URL staffPresignedUrl = amazonS3.generatePresignedUrl(buildPresignedUrlRequest(bucket, staffFilePath));
			staffUrls.put(staffImage, staffPresignedUrl.toString());
		}
		performanceMakerPresignedUrls.put("staff", staffUrls);

		Map<String, String> performanceImageUrls = new HashMap<>();
		for (String performanceImage : performanceImages) {
			String performanceImageFilePath = generatePath("performance", performanceImage);
			URL performanceImagePresignedUrl = amazonS3.generatePresignedUrl(
				buildPresignedUrlRequest(bucket, performanceImageFilePath));
			performanceImageUrls.put(performanceImage, performanceImagePresignedUrl.toString());
		}
		performanceMakerPresignedUrls.put("performance", performanceImageUrls);

		return new PerformancePresignedUrls(performanceMakerPresignedUrls);
	}

	@Override
	public CarouselPresignedUrls issueAllPresignedUrlsForCarousel(List<String> carouselImages) {
		Map<String, String> carouselPresignedUrls = new HashMap<>();

		for (String carouselImage : carouselImages) {
			String carouselFilePath = generatePath("carousel", carouselImage);
			URL carouselPresignedUrl = amazonS3.generatePresignedUrl(
				buildPresignedUrlRequest(bucket, carouselFilePath));
			carouselPresignedUrls.put(carouselImage, carouselPresignedUrl.toString());
		}

		return new CarouselPresignedUrls(carouselPresignedUrls);
	}

	@Override
	public BannerPresignedUrl issuePresignedUrlForBanner(String bannerImage) {
		String bannerFilePath = generatePath("banner", bannerImage);
		URL bannerPresignedUrl = amazonS3.generatePresignedUrl(buildPresignedUrlRequest(bucket, bannerFilePath));
		return new BannerPresignedUrl(bannerPresignedUrl.toString());
	}

	private GeneratePresignedUrlRequest buildPresignedUrlRequest(String bucket, String fileName) {
		return new GeneratePresignedUrlRequest(bucket, fileName)
			.withMethod(HttpMethod.PUT)
			.withExpiration(generatePresignedUrlExpiration());
	}

	private Date generatePresignedUrlExpiration() {
		Date expiration = new Date();
		long expTimeMillis = expiration.getTime() + 1000L * 60 * 60 * 2;
		expiration.setTime(expTimeMillis);
		return expiration;
	}

	private String generatePath(String prefix, String fileName) {
		return String.format("%s/%s", prefix, UUID.randomUUID() + "-" + fileName);
	}
}
