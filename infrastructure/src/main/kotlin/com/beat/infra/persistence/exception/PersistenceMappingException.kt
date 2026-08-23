package com.beat.infra.persistence.exception

internal class PersistenceMappingException private constructor(
    message: String,
    cause: RuntimeException,
) : RuntimeException(message, cause) {
    companion object {
        fun invalidStoredState(
            entityName: String,
            entityId: Any?,
            cause: RuntimeException,
        ): PersistenceMappingException =
            PersistenceMappingException(
                "Invalid stored state for $entityName with id=$entityId",
                cause,
            )
    }
}
