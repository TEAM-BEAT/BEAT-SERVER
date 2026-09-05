package com.beat.application.frontoffice.booking.booker.experiment

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.core.type.AnnotatedTypeMetadata

/**
 * Keeps the dev-only experiment flag independent from Spring Boot's autoconfigure module.
 * Application and infrastructure adapters both use this condition for their experiment beans.
 */
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Conditional(StockContentionExperimentEnabledCondition::class)
annotation class StockContentionExperimentEnabled

class StockContentionExperimentEnabledCondition : Condition {
    override fun matches(
        context: ConditionContext,
        metadata: AnnotatedTypeMetadata,
    ): Boolean = context.environment.getProperty("booking.experiment.enabled")?.toBoolean() == true
}
