package com.beat.apis.external.s3.facade;

import java.util.List;

import org.springframework.stereotype.Service;

import com.beat.apis.external.s3.api.dto.PerformanceMakerPresignedUrlFindAllResponse;
import com.beat.apis.external.s3.application.FileService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileFacade {
	private final FileService fileService;

	public PerformanceMakerPresignedUrlFindAllResponse issueAllPresignedUrlsForPerformanceMaker(String posterImage,
		List<String> castImages, List<String> staffImages, List<String> performanceImages) {
		return PerformanceMakerPresignedUrlFindAllResponse.from(
			fileService.issueAllPresignedUrlsForPerformanceMaker(
				posterImage,
				castImages,
				staffImages,
				performanceImages
			)
		);
	}
}
