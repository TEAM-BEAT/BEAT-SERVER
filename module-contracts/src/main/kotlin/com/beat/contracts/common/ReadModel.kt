package com.beat.contracts.common

/**
 * Marks a module-contracts type as a read-only query result shape.
 *
 * A ReadModel is not a domain model, JPA entity, or executable-module response DTO.
 * It exists only as an implementation-free query contract between an application query
 * service and an infra query adapter.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ReadModel
