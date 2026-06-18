package com.beat.apis.external.s3.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.beat.apis.external.s3.api.dto.PerformanceMakerPresignedUrlFindAllResponse;
import com.beat.apis.external.s3.application.FileService;
import com.beat.contracts.storage.PerformancePresignedUrls;

@ExtendWith(MockitoExtension.class)
class FileFacadeTest {

	@Mock
	private FileService fileService;

	private FileFacade fileFacade;

	@BeforeEach
	void setUp() {
		fileFacade = new FileFacade(fileService);
	}

	@Test
	void issueAllPresignedUrlsDelegatesNullableListsToApplicationService() {
		PerformancePresignedUrls presignedUrls = new PerformancePresignedUrls(
			Map.of("poster", Map.of("poster.png", "https://example.com/poster.png"))
		);
		when(fileService.issueAllPresignedUrlsForPerformanceMaker("poster.png", null, null, null))
			.thenReturn(presignedUrls);

		PerformanceMakerPresignedUrlFindAllResponse response = fileFacade.issueAllPresignedUrlsForPerformanceMaker(
			"poster.png",
			null,
			null,
			null
		);

		assertEquals(presignedUrls.getPerformanceMakerPresignedUrls(), response.performanceMakerPresignedUrls());
		verify(fileService).issueAllPresignedUrlsForPerformanceMaker("poster.png", null, null, null);
	}
}
