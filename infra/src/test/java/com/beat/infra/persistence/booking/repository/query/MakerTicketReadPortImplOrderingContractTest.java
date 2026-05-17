package com.beat.infra.persistence.booking.repository.query;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class MakerTicketReadPortImplOrderingContractTest {

	@Test
	void makerTicketQueriesOrderByCreatedAtDescendingAfterStatusPriority() throws IOException {
		String source = Files.readString(Path.of(System.getProperty("user.dir"))
			.resolve("src/main/java/com/beat/infra/persistence/booking/repository/query/MakerTicketReadPortImpl.java"));

		int statusPriorityOrder = source.indexOf("END ASC");
		int createdAtDescendingOrder = source.indexOf("b.createdAt DESC");

		assertTrue(statusPriorityOrder >= 0, "Ticket query must keep status-priority ordering first");
		assertTrue(createdAtDescendingOrder > statusPriorityOrder,
			"Ticket query must use createdAt DESC after status-priority ordering");
	}

	@Test
	void makerTicketReadModelUsesDisplayNameForBankName() throws IOException {
		String source = Files.readString(Path.of(System.getProperty("user.dir"))
			.resolve("src/main/java/com/beat/infra/persistence/booking/repository/query/MakerTicketReadPortImpl.java"));

		assertTrue(source.contains("bankName.getDisplayName()"));
		assertFalse(source.contains("bankName.name()"));
	}
}
