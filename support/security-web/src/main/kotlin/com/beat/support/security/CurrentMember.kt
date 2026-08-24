package com.beat.support.security

import io.swagger.v3.oas.annotations.Parameter

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Parameter(hidden = true)
annotation class CurrentMember
