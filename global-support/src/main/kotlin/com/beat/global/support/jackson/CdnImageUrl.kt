package com.beat.global.support.jackson

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside
import tools.jackson.databind.annotation.JsonSerialize

/**
 * Marks a response DTO field that holds a storage key for an image. Jackson
 * uses [CdnImageUrlSerializer] to prepend the configured CDN base when
 * serializing, so application services can keep dealing with bare keys.
 *
 * Usage:
 * ```kotlin
 * data class HomePerformanceDetail(
 *     val performanceId: Long,
 *     @CdnImageUrl val posterImage: String?,
 *     // ...
 * )
 * ```
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.VALUE_PARAMETER)
@JacksonAnnotationsInside
@JsonSerialize(using = CdnImageUrlSerializer::class)
annotation class CdnImageUrl
