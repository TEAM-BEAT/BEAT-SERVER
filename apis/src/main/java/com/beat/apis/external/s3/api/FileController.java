package com.beat.apis.external.s3.api;

import com.beat.global.support.response.SuccessResponse;
import com.beat.apis.external.s3.api.dto.PerformanceMakerPresignedUrlFindAllResponse;
import com.beat.apis.external.s3.exception.FileSuccessCode;
import com.beat.apis.external.s3.facade.FileFacade;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController implements FileApi {

	private final FileFacade fileFacade;

	@GetMapping("/presigned-url")
	@Override
	public ResponseEntity<SuccessResponse<PerformanceMakerPresignedUrlFindAllResponse>> generateAllPresignedUrls(
		@RequestParam String posterImage,
		@RequestParam(required = false) List<String> castImages,
		@RequestParam(required = false) List<String> staffImages,
		@RequestParam(required = false) List<String> performanceImages) {
		// S3 upload URL issuance is currently public by existing API policy.
		PerformanceMakerPresignedUrlFindAllResponse response = fileFacade.issueAllPresignedUrlsForPerformanceMaker(
			posterImage,
			castImages,
			staffImages,
			performanceImages
		);
		return ResponseEntity.ok(SuccessResponse.of(FileSuccessCode.PERFORMANCE_MAKER_PRESIGNED_URL_ISSUED, response));
	}
}
