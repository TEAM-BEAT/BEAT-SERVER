package com.beat.apis.file.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import com.beat.apis.file.api.response.PerformanceMakerPresignedUrlFindAllResponse;
import com.beat.apis.file.facade.FileFacade;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

	@Mock
	private FileFacade fileFacade;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = standaloneSetup(new FileController(fileFacade)).build();
		when(fileFacade.issueAllPresignedUrlsForPerformanceMaker(
			org.mockito.ArgumentMatchers.anyString(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any()
		)).thenReturn(mock(PerformanceMakerPresignedUrlFindAllResponse.class));
	}

	@Test
	void canonicalPerformanceImagesParameterIsForwarded() throws Exception {
		mockMvc.perform(get("/api/files/presigned-url")
				.param("posterImage", "poster.png")
				.param("performanceImages", "performance.png"))
			.andExpect(status().isOk());

		verify(fileFacade).issueAllPresignedUrlsForPerformanceMaker(
			"poster.png", null, null, List.of("performance.png"));
	}

	@Test
	void legacyPerformImagesParameterIsForwarded() throws Exception {
		mockMvc.perform(get("/api/files/presigned-url")
				.param("posterImage", "poster.png")
				.param("performImages", "performance.png"))
			.andExpect(status().isOk());

		verify(fileFacade).issueAllPresignedUrlsForPerformanceMaker(
			"poster.png", null, null, List.of("performance.png"));
	}

	@Test
	void canonicalPerformanceImagesTakesPrecedenceOverLegacyAlias() throws Exception {
		mockMvc.perform(get("/api/files/presigned-url")
				.param("posterImage", "poster.png")
				.param("performanceImages", "canonical.png")
				.param("performImages", "legacy.png"))
			.andExpect(status().isOk());

		verify(fileFacade).issueAllPresignedUrlsForPerformanceMaker(
			"poster.png", null, null, List.of("canonical.png"));
	}

	@Test
	void emptyPerformanceImagesPlaceholderIsBoundAsAnEmptyList() throws Exception {
		mockMvc.perform(get("/api/files/presigned-url")
				.param("posterImage", "poster.png")
				.param("performanceImages", ""))
			.andExpect(status().isOk());

		verify(fileFacade).issueAllPresignedUrlsForPerformanceMaker("poster.png", null, null, List.of());
	}
}
