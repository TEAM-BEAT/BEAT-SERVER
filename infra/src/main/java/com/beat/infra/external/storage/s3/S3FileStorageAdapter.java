package com.beat.infra.external.storage.s3;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.beat.contracts.storage.BannerPresignedUrl;
import com.beat.contracts.storage.CarouselPresignedUpload;
import com.beat.contracts.storage.CarouselPresignedUrls;
import com.beat.contracts.storage.FileStoragePort;
import com.beat.contracts.storage.ImageObjectMetadata;
import com.beat.contracts.storage.ImagePresignedUpload;
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
	private static final long PRESIGNED_URL_VALIDITY_MILLIS = 15L * 60 * 1000;

	@Value("${cloud.s3.bucket}")
	private String bucket;

	@Value("${cloud.s3.key-prefix:}")
	private String keyPrefix;

	private final AmazonS3 amazonS3;

	@Override
	public PerformancePresignedUrls issueAllPresignedUrlsForPerformanceMaker(
		String posterImage,
		List<String> castImages,
		List<String> staffImages,
		List<String> performanceImages
	) {
		Map<String, Map<String, ImagePresignedUpload>> performanceMakerPresignedUploads = new HashMap<>();

		Map<String, ImagePresignedUpload> posterUrl = new HashMap<>();
		String posterFilePath = generatePath("poster", posterImage);
		URL posterPresignedUrl = amazonS3.generatePresignedUrl(buildPresignedUrlRequest(bucket, posterFilePath));
		posterUrl.put(posterImage, ImagePresignedUpload.of(posterPresignedUrl.toString(), posterFilePath));
		performanceMakerPresignedUploads.put("poster", posterUrl);

		Map<String, ImagePresignedUpload> castUrls = new HashMap<>();
		for (String castImage : castImages) {
			String castFilePath = generatePath("cast", castImage);
			URL castPresignedUrl = amazonS3.generatePresignedUrl(buildPresignedUrlRequest(bucket, castFilePath));
			castUrls.put(castImage, ImagePresignedUpload.of(castPresignedUrl.toString(), castFilePath));
		}
		performanceMakerPresignedUploads.put("cast", castUrls);

		Map<String, ImagePresignedUpload> staffUrls = new HashMap<>();
		for (String staffImage : staffImages) {
			String staffFilePath = generatePath("staff", staffImage);
			URL staffPresignedUrl = amazonS3.generatePresignedUrl(buildPresignedUrlRequest(bucket, staffFilePath));
			staffUrls.put(staffImage, ImagePresignedUpload.of(staffPresignedUrl.toString(), staffFilePath));
		}
		performanceMakerPresignedUploads.put("staff", staffUrls);

		Map<String, ImagePresignedUpload> performanceImageUrls = new HashMap<>();
		for (String performanceImage : performanceImages) {
			String performanceImageFilePath = generatePath("performance", performanceImage);
			URL performanceImagePresignedUrl = amazonS3.generatePresignedUrl(
				buildPresignedUrlRequest(bucket, performanceImageFilePath));
			performanceImageUrls.put(performanceImage,
				ImagePresignedUpload.of(performanceImagePresignedUrl.toString(), performanceImageFilePath));
		}
		performanceMakerPresignedUploads.put("performance", performanceImageUrls);

		return new PerformancePresignedUrls(performanceMakerPresignedUploads);
	}

	@Override
	public CarouselPresignedUrls issueAllPresignedUrlsForCarousel(List<String> carouselImages) {
		Map<String, CarouselPresignedUpload> carouselPresignedUploads = new HashMap<>();

		for (String carouselImage : carouselImages) {
			String carouselFilePath = generatePath("carousel", carouselImage);
			URL carouselPresignedUrl = amazonS3.generatePresignedUrl(
				buildPresignedUrlRequest(bucket, carouselFilePath));
			carouselPresignedUploads.put(carouselImage,
				CarouselPresignedUpload.of(carouselPresignedUrl.toString(), carouselFilePath));
		}

		return new CarouselPresignedUrls(carouselPresignedUploads);
	}

	@Override
	public ImageObjectMetadata findImageObjectMetadata(String imageKey) {
		if (!imageKey.startsWith(imageKeyPrefix())) {
			return null;
		}
		try {
			ObjectMetadata objectMetadata = amazonS3.getObjectMetadata(bucket, imageKey);
			return ImageObjectMetadata.of(objectMetadata.getContentType(), objectMetadata.getContentLength());
		} catch (AmazonS3Exception exception) {
			if (exception.getStatusCode() == 404) {
				return null;
			}
			throw exception;
		}
	}

	@Override
	public BannerPresignedUrl issuePresignedUrlForBanner(String bannerImage) {
		String bannerFilePath = generatePath("banner", bannerImage);
		URL bannerPresignedUrl = amazonS3.generatePresignedUrl(buildPresignedUrlRequest(bucket, bannerFilePath));
		return new BannerPresignedUrl(bannerPresignedUrl.toString(), bannerFilePath);
	}

	private GeneratePresignedUrlRequest buildPresignedUrlRequest(String bucket, String fileName) {
		return new GeneratePresignedUrlRequest(bucket, fileName)
			.withMethod(HttpMethod.PUT)
			.withExpiration(generatePresignedUrlExpiration());
	}

	private Date generatePresignedUrlExpiration() {
		Date expiration = new Date();
		long expTimeMillis = expiration.getTime() + PRESIGNED_URL_VALIDITY_MILLIS;
		expiration.setTime(expTimeMillis);
		return expiration;
	}

	private String generatePath(String prefix, String fileName) {
		String filePath = String.format("%s/%s", prefix, UUID.randomUUID() + "-" + fileName);
		String normalizedKeyPrefix = normalizeKeyPrefix();
		if (normalizedKeyPrefix.isEmpty()) {
			return filePath;
		}
		return String.format("%s/%s", normalizedKeyPrefix, filePath);
	}

	private String normalizeKeyPrefix() {
		if (keyPrefix == null || keyPrefix.isBlank()) {
			return "";
		}
		return keyPrefix.replaceAll("^/+", "").replaceAll("/+$", "");
	}

	private String imageKeyPrefix() {
		String normalizedKeyPrefix = normalizeKeyPrefix();
		return normalizedKeyPrefix.isEmpty() ? "" : normalizedKeyPrefix + "/";
	}
}
