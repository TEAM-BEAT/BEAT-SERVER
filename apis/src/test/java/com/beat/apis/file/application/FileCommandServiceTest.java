package com.beat.apis.file.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.beat.apis.exception.ApiApplicationException;
import com.beat.apis.file.application.command.FileCommandService;
import com.beat.apis.file.exception.FileApplicationErrorCode;
import com.beat.contracts.storage.FileStoragePort;
import com.beat.contracts.storage.PerformancePresignedUrls;
import com.beat.contracts.storage.ImagePresignedUpload;

@ExtendWith(MockitoExtension.class)
class FileCommandServiceTest {

	@Mock
	private FileStoragePort fileStoragePort;

	private FileCommandService fileService;

	@BeforeEach
	void setUp() {
		fileService = new FileCommandService(fileStoragePort);
	}

	@Test
	void issueAllPresignedUrlsNormalizesNullableListsBeforeCallingStoragePort() {
		PerformancePresignedUrls presignedUrls = new PerformancePresignedUrls(
			Map.of("poster", Map.of("poster.png",
				ImagePresignedUpload.of("https://example.com/poster.png", "dev/poster/poster.png")))
		);
		when(fileStoragePort.issueAllPresignedUrlsForPerformanceMaker("poster.png", List.of(), List.of(), List.of()))
			.thenReturn(presignedUrls);

		fileService.issueAllPresignedUrlsForPerformanceMaker(
			"poster.png",
			null,
			null,
			null
		);

		verify(fileStoragePort).issueAllPresignedUrlsForPerformanceMaker("poster.png", List.of(), List.of(), List.of());
	}

	@Test
	void issueAllPresignedUrlsIgnoresLegacyEmptyListPlaceholders() {
		fileService.issueAllPresignedUrlsForPerformanceMaker(
			"poster.png",
			List.of(""),
			List.of(""),
			List.of("")
		);

		verify(fileStoragePort).issueAllPresignedUrlsForPerformanceMaker("poster.png", List.of(), List.of(), List.of());
	}

	@Test
	void issueAllPresignedUrlsRejectsPathLikeFileNamesBeforeCallingStorage() {
		ApiApplicationException exception = assertThrows(
			ApiApplicationException.class,
			() -> fileService.issueAllPresignedUrlsForPerformanceMaker("../poster.png", null, null, null)
		);

		assertEquals(FileApplicationErrorCode.INVALID_FILE_NAME, exception.getErrorCode());
		verifyNoInteractions(fileStoragePort);
	}
}
