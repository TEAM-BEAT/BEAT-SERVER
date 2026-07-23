package com.beat.admin.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.beat.domain.booking.exception.BookingErrorCode;
import com.beat.domain.exception.DomainErrorCode;
import com.beat.domain.exception.DomainException;
import com.beat.domain.performance.exception.PerformanceErrorCode;
import com.beat.domain.schedule.exception.ScheduleErrorCode;
import com.beat.global.support.response.ErrorResponse;

class AdminGlobalExceptionHandlerTest {
	private final AdminGlobalExceptionHandler handler = new AdminGlobalExceptionHandler();
	private final MockMvc mockMvc = standaloneSetup(new TestController())
		.setControllerAdvice(handler)
		.build();

	@Test
	void malformedJsonUsesLegacyErrorEnvelope() throws Exception {
		mockMvc.perform(post("/test").contentType(MediaType.APPLICATION_JSON).content("{"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
	}

	@Test
	void unsupportedMethodPreservesAllowHeader() throws Exception {
		mockMvc.perform(put("/test"))
			.andExpect(status().isMethodNotAllowed())
			.andExpect(header().string("Allow", "POST"))
			.andExpect(jsonPath("$.status").value(405));
	}

	@Test
	void unknownRouteKeepsLegacyErrorEnvelope() throws Exception {
		mockMvc.perform(get("/unknown-route"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
	}

	@Test
	void missingStaticResourceKeepsEmptyNotFoundBody() {
		ResponseEntity<Object> response = handler.handleNoResourceFoundException(
			new NoResourceFoundException(HttpMethod.GET, "/missing-resource", "missing-resource"),
			new HttpHeaders(), HttpStatus.NOT_FOUND,
			new ServletWebRequest(new MockHttpServletRequest(), new MockHttpServletResponse()));

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertNull(response.getBody());
	}

	@Test
	void mapsDomainStateConflictToMatchingHttpAndBodyStatus() {
		ResponseEntity<ErrorResponse> response = handler.handleDomainException(
			new DomainException(ScheduleErrorCode.INSUFFICIENT_TICKETS));

		assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
		assertEquals(HttpStatus.CONFLICT.value(), response.getBody().getStatus());
		assertEquals(ScheduleErrorCode.INSUFFICIENT_TICKETS.getMessage(), response.getBody().getMessage());
	}

	@ParameterizedTest
	@MethodSource("genericV1DomainMappings")
	void preservesGenericV1DomainContract(DomainErrorCode errorCode) {
		ResponseEntity<ErrorResponse> response = handler.handleDomainException(new DomainException(errorCode));

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
		assertEquals("잘못된 데이터 형식입니다.", response.getBody().getMessage());
	}

	private static Stream<Arguments> genericV1DomainMappings() {
		return Stream.of(
			Arguments.of(BookingErrorCode.INVALID_PURCHASE_TICKET_COUNT),
			Arguments.of(BookingErrorCode.INVALID_REFUND_ACCOUNT),
			Arguments.of(BookingErrorCode.PAYMENT_CONFIRMATION_NOT_ALLOWED),
			Arguments.of(BookingErrorCode.REFUND_REQUEST_NOT_ALLOWED),
			Arguments.of(PerformanceErrorCode.NON_POSITIVE_RUNNING_TIME),
			Arguments.of(PerformanceErrorCode.NEGATIVE_SCHEDULE_COUNT),
			Arguments.of(ScheduleErrorCode.INVALID_BOOKING_WINDOW),
			Arguments.of(ScheduleErrorCode.NEGATIVE_TICKET_COUNT),
			Arguments.of(ScheduleErrorCode.NON_POSITIVE_TICKET_COUNT)
		);
	}

	@Test
	void doesNotExposeUnexpectedExceptionMessage() {
		ResponseEntity<ErrorResponse> response = handler.handleException(
			new IllegalArgumentException("internal contract detail"), new MockHttpServletRequest());

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().getStatus());
		assertEquals("서버 내부 오류입니다.", response.getBody().getMessage());
	}

	@Test
	void delegatesCommittedResponseProtectionToSpring() {
		MockHttpServletResponse servletResponse = new MockHttpServletResponse();
		servletResponse.setCommitted(true);

		ResponseEntity<Object> response = handler.handleExceptionInternal(
			new IllegalStateException("already committed"), null, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR,
			new ServletWebRequest(new MockHttpServletRequest(), servletResponse));

		assertNull(response);
	}

	@ParameterizedTest
	@MethodSource("applicationStatusMappings")
	void mapsEveryApplicationErrorTypeToHttpStatus(ApplicationErrorType type, HttpStatus expectedStatus) {
		ResponseEntity<ErrorResponse> response = handler.handleApplicationException(
			new AdminApplicationException(new TestApplicationErrorCode(type)), new MockHttpServletRequest());

		assertEquals(expectedStatus, response.getStatusCode());
		assertEquals(expectedStatus.value(), response.getBody().getStatus());
	}

	private static Stream<Arguments> applicationStatusMappings() {
		return Stream.of(
			Arguments.of(ApplicationErrorType.INVALID_INPUT, HttpStatus.BAD_REQUEST),
			Arguments.of(ApplicationErrorType.UNAUTHENTICATED, HttpStatus.UNAUTHORIZED),
			Arguments.of(ApplicationErrorType.FORBIDDEN, HttpStatus.FORBIDDEN),
			Arguments.of(ApplicationErrorType.NOT_FOUND, HttpStatus.NOT_FOUND),
			Arguments.of(ApplicationErrorType.STATE_CONFLICT, HttpStatus.CONFLICT),
			Arguments.of(ApplicationErrorType.UPSTREAM_FAILURE, HttpStatus.BAD_GATEWAY),
			Arguments.of(ApplicationErrorType.UPSTREAM_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE),
			Arguments.of(ApplicationErrorType.UPSTREAM_TIMEOUT, HttpStatus.GATEWAY_TIMEOUT),
			Arguments.of(ApplicationErrorType.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR)
		);
	}

	@RestController
	private static class TestController {

		@PostMapping("/test")
		void accept(@RequestBody TestRequest request) {
		}
	}

	private record TestRequest(String value) {
	}

	private record TestApplicationErrorCode(ApplicationErrorType type) implements ApplicationErrorCode {
		@Override
		public String getCode() {
			return "TEST_APPLICATION_ERROR";
		}

		@Override
		public ApplicationErrorType getType() {
			return type;
		}

		@Override
		public String getMessage() {
			return "안전한 테스트 오류입니다.";
		}
	}
}
