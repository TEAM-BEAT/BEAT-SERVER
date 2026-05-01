package com.beat.apis.external.s3.application;

import java.util.List;

import org.springframework.stereotype.Service;

import com.beat.contracts.storage.FileStoragePort;
import com.beat.contracts.storage.PerformancePresignedUrls;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileService {

	private final FileStoragePort fileStoragePort;

	public PerformancePresignedUrls issueAllPresignedUrlsForPerformanceMaker(String posterImage,
		List<String> castImages, List<String> staffImages, List<String> performanceImages) {
		return fileStoragePort.issueAllPresignedUrlsForPerformanceMaker(
			posterImage,
			nullToEmpty(castImages),
			nullToEmpty(staffImages),
			nullToEmpty(performanceImages)
		);
	}

	private List<String> nullToEmpty(List<String> values) {
		if (values == null) {
			return List.of();
		}
		return values;
	}
}
