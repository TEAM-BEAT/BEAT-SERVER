package com.beat.global.support.jackson

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside
import tools.jackson.databind.annotation.JsonSerialize

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.VALUE_PARAMETER)
@JacksonAnnotationsInside
@JsonSerialize(using = CdnImageUrlSerializer::class)
annotation class CdnImageUrl
