package com.beat.contracts.auth.guest.readmodel

data class GuestCredentialReadModel(
    val userId: Long,
    val encodedPassword: String,
)
