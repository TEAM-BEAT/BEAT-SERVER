package com.beat.apis.external.s3.facade;

import java.util.List;

import org.springframework.stereotype.Service;

import com.beat.apis.external.s3.api.dto.PerformanceMakerPresignedUrlFindAllResponse;
import com.beat.contracts.storage.FileStoragePort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileFacade {
	private final FileStoragePort fileStoragePort;

	public PerformanceMakerPresignedUrlFindAllResponse issueAllPresignedUrlsForPerformanceMaker(String posterImage,
		List<String> castImages, List<String> staffImages, List<String> performanceImages) {
		return PerformanceMakerPresignedUrlFindAllResponse.from(
			fileStoragePort.issueAllPresignedUrlsForPerformanceMaker(
				posterImage,
				nullToEmpty(castImages),
				nullToEmpty(staffImages),
				nullToEmpty(performanceImages)
			)
		);
	}

	private List<String> nullToEmpty(List<String> values) {
		if (values == null) {
			return List.of();
		}
		return values;
	}
}
