package com.beat.apis.external.s3.application;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.beat.contracts.storage.FileStoragePort;
import com.beat.contracts.storage.PerformancePresignedUrls;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

	@Mock
	private FileStoragePort fileStoragePort;

	private FileService fileService;

	@BeforeEach
	void setUp() {
		fileService = new FileService(fileStoragePort);
	}

	@Test
	void issueAllPresignedUrlsNormalizesNullableListsBeforeCallingStoragePort() {
		PerformancePresignedUrls presignedUrls = new PerformancePresignedUrls(
			Map.of("poster", Map.of("poster.png", "https://example.com/poster.png"))
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
}
