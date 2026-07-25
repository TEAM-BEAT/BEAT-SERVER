package com.beat.infra.persistence.exception;

public final class PersistenceMappingException extends RuntimeException {

	private PersistenceMappingException(String message, RuntimeException cause) {
		super(message, cause);
	}

	public static PersistenceMappingException invalidStoredState(
		String entityName,
		Object entityId,
		RuntimeException cause
	) {
		return new PersistenceMappingException(
			"Invalid stored state for " + entityName + " with id=" + entityId,
			cause
		);
	}
}
