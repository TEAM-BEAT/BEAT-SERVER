package com.beat.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalManagementPort;

import com.beat.batch.support.AbstractBatchIntegrationTest;

class BatchActuatorHealthBootTest extends AbstractBatchIntegrationTest {

	@LocalManagementPort
	private int managementPort;

	@Test
	void actuatorHealthEndpointIsReachable() throws Exception {
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + managementPort + "/actuator-test/health"))
			.GET()
			.build();

		HttpResponse<String> response = HttpClient.newHttpClient().send(
			request,
			HttpResponse.BodyHandlers.ofString()
		);

		assertEquals(200, response.statusCode());
		assertTrue(response.body().contains("\"status\""));
	}
}
