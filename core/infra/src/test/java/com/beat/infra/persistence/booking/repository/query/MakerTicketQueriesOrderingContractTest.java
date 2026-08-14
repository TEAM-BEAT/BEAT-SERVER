package com.beat.infra.persistence.booking.repository.query;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class MakerTicketQueriesOrderingContractTest {

	private static final String QUERY_SOURCE =
		"src/main/kotlin/com/beat/infra/persistence/booking/repository/query/MakerTicketQueries.kt";

	@Test
	void makerTicketQueriesOrderByCreatedAtDescendingAfterStatusPriority() throws IOException {
		String source = Files.readString(Path.of(System.getProperty("user.dir")).resolve(QUERY_SOURCE));

		int statusPriorityOrder = source.indexOf("BookingStatus.REFUND_REQUESTED)).then(1)");
		int createdAtDescendingOrder = source.indexOf("BookingJpaEntity::createdAt).desc()");

		assertTrue(statusPriorityOrder >= 0, "Ticket query must keep status-priority ordering first");
		assertTrue(createdAtDescendingOrder > statusPriorityOrder,
			"Ticket query must use createdAt DESC after status-priority ordering");
	}

	@Test
	void makerTicketReadModelUsesDisplayNameForBankName() throws IOException {
		String source = Files.readString(Path.of(System.getProperty("user.dir")).resolve(QUERY_SOURCE));

		assertTrue(source.contains(".displayName"));
		assertFalse(source.contains("bankName.name"));
	}
}
