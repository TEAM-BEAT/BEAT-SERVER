package com.beat.infra.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class AsyncConfigTest {

	@Test
	void beatAsyncConfigurerReusesApplicationExecutorWithoutSecuritySpecificWrapper() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.initialize();
		AsyncConfigurer configurer = new AsyncConfig().beatAsyncConfigurer(executor);

		assertSame(executor, configurer.getAsyncExecutor());
	}

	@Test
	void formatAsyncParametersReturnsNullWhenParamsAreMissing() {
		assertEquals("null", AsyncConfig.formatAsyncParameters(null));
	}

	@Test
	void formatAsyncParametersPreservesArrayFormatting() {
		Object[] params = {"member-123", 7};

		assertEquals("[member-123, 7]", AsyncConfig.formatAsyncParameters(params));
	}

	@Test
	void formatAsyncExceptionMessageMatchesDeletedHandlerShape() throws Exception {
		Method method = SampleAsyncTarget.class.getDeclaredMethod("send", String.class, int.class);
		Throwable ex = new IllegalStateException("boom");

		assertEquals(
			"비동기 작업 중 예외 발생! Method: [send], Params: [[member-123, 7]], Exception: [boom]",
			AsyncConfig.formatAsyncExceptionMessage(method, new Object[] {"member-123", 7}, ex)
		);
	}

	private static final class SampleAsyncTarget {
		@SuppressWarnings("unused")
		private void send(String memberId, int retryCount) {
		}
	}
}
