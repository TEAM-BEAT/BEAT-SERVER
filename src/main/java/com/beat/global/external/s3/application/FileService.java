package com.beat.global.external.s3.application;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.beat.global.external.s3.application.dto.PerformanceMakerPresignedUrlFindAllResponse;
import com.beat.global.external.s3.port.in.FileUseCase;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService implements FileUseCase {

	@Value("${cloud.s3.bucket}")
	private String bucket;

	private final AmazonS3 amazonS3;

	@Override
	public PerformanceMakerPresignedUrlFindAllResponse issueAllPresignedUrlsForPerformanceMaker(String posterImage,
		List<String> castImages, List<String> staffImages, List<String> performanceImages) {
		Map<String, Map<String, String>> performanceMakerPresignedUrls = new HashMap<>();

		// Poster Image URL
		Map<String, String> posterUrl = new HashMap<>();
		String posterFilePath = generatePath("poster", posterImage);
		URL posterPresignedUrl = amazonS3.generatePresignedUrl(buildPresignedUrlRequest(bucket, posterFilePath));
		posterUrl.put(posterImage, posterPresignedUrl.toString());
		performanceMakerPresignedUrls.put("poster", posterUrl);

		// Cast Images URLs
		Map<String, String> castUrls = new HashMap<>();
		for (String castImage : castImages) {
			String castFilePath = generatePath("cast", castImage);
			URL castPresignedUrl = amazonS3.generatePresignedUrl(buildPresignedUrlRequest(bucket, castFilePath));
			castUrls.put(castImage, castPresignedUrl.toString());
		}
		performanceMakerPresignedUrls.put("cast", castUrls);

		// Staff Images URLs
		Map<String, String> staffUrls = new HashMap<>();
		for (String staffImage : staffImages) {
			String staffFilePath = generatePath("staff", staffImage);
			URL staffPresignedUrl = amazonS3.generatePresignedUrl(buildPresignedUrlRequest(bucket, staffFilePath));
			staffUrls.put(staffImage, staffPresignedUrl.toString());
		}
		performanceMakerPresignedUrls.put("staff", staffUrls);

		// Performance Images URLs
		Map<String, String> performanceImageUrls = new HashMap<>();
		for (String performanceImage : performanceImages) {
			String performanceImageFilePath = generatePath("performance", performanceImage);
			URL performanceImagePresignedUrl = amazonS3.generatePresignedUrl(
				buildPresignedUrlRequest(bucket, performanceImageFilePath));
			performanceImageUrls.put(performanceImage, performanceImagePresignedUrl.toString());
		}
		performanceMakerPresignedUrls.put("performance", performanceImageUrls);

		return PerformanceMakerPresignedUrlFindAllResponse.from(performanceMakerPresignedUrls);
	}

	// Carousel Images URLs
	@Override
	public Map<String, String> issueAllPresignedUrlsForCarousel(List<String> carouselImages) {
		Map<String, String> carouselPresignedUrls = new HashMap<>();

		for (String carouselImage : carouselImages) {
			String carouselFilePath = generatePath("carousel", carouselImage);
			URL carouselPresignedUrl = amazonS3.generatePresignedUrl(
				buildPresignedUrlRequest(bucket, carouselFilePath));
			carouselPresignedUrls.put(carouselImage, carouselPresignedUrl.toString());
		}

		return carouselPresignedUrls;
	}

	// Banner Image URL
	@Override
	public String issuePresignedUrlForBanner(String bannerImage) {
		String bannerFilePath = generatePath("banner", bannerImage);
		URL bannerPresignedUrl = amazonS3.generatePresignedUrl(buildPresignedUrlRequest(bucket, bannerFilePath));

		return bannerPresignedUrl.toString();
	}

	private GeneratePresignedUrlRequest buildPresignedUrlRequest(String bucket, String fileName) {
		return new GeneratePresignedUrlRequest(bucket, fileName)
			.withMethod(HttpMethod.PUT)
			.withExpiration(generatePresignedUrlExpiration());
	}

	private Date generatePresignedUrlExpiration() {
		Date expiration = new Date();
		long expTimeMillis = expiration.getTime();
		expTimeMillis += 1000 * 60 * 60 * 2;
		expiration.setTime(expTimeMillis);

		return expiration;
	}

	private String generateFileId() {
		return UUID.randomUUID().toString();
	}

	private String generatePath(String prefix, String fileName) {
		String fileId = generateFileId();
		return String.format("%s/%s", prefix, fileId + "-" + fileName);
	}
}