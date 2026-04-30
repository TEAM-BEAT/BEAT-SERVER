package com.beat.contracts.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a module-contracts type as a read-only query result shape.
 *
 * <p>A ReadModel is not a domain model, JPA entity, or executable-module response DTO.
 * It exists only as an implementation-free query contract between an application query
 * service and an infra query adapter.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReadModel {
}
